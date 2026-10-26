package grails.plugins.scim

import grails.converters.JSON
import grails.core.GrailsApplication
import grails.plugins.scim.security.InvalidScimTokenException
import grails.plugins.scim.security.ScimJwtParser
import grails.plugins.scim.security.ScimTenantContext
import grails.plugins.scim.security.ScimTenantJwtSecretResolver
import grails.plugins.scim.security.ScimTenantKeyResolver
import grails.plugins.scim.security.ScimTenantTokenParser
import groovy.util.logging.Slf4j
import org.springframework.http.HttpStatus
import jakarta.servlet.http.HttpServletRequest

import java.nio.charset.StandardCharsets
import java.security.MessageDigest

/**
 * Authenticates SCIM requests.
 *
 * Three mutually exclusive flows, tried in this order:
 * <ul>
 *   <li><b>Legacy static token</b> ({@code grails.scim.legacyStaticToken.enabled}, default true):
 *       the bearer value matches {@code grails.scim.api_token}. No tenant context is set and
 *       payload tenant fields are honoured as sent.</li>
 *   <li><b>Tenant JWT</b>: an HS256 JWS whose header carries {@code tid} and {@code kid}. The secret
 *       comes from {@link ScimTenantJwtSecretResolver}; signature, {@code exp}, {@code nbf} and optional
 *       issuer are validated by {@link ScimJwtParser}.</li>
 *   <li><b>Tenant token</b>: {@code base64url(tenantId).base64url(secret)}. The SHA-256 of the
 *       presented secret is compared with the hash supplied by {@link ScimTenantKeyResolver}.</li>
 * </ul>
 * For both tenant flows {@link ScimTenantContext} carries the tenant for the rest of the request.
 */
@Slf4j
class ScimControllerInterceptor {

    static final String LEGACY_ENABLED_KEY = 'grails.scim.legacyStaticToken.enabled'
    static final String JWT_CLOCK_SKEW_KEY = 'grails.scim.jwt.clockSkewSeconds'
    static final long DEFAULT_JWT_CLOCK_SKEW = 60L

    GrailsApplication grailsApplication

    /** Injected by bean name; hosts override by declaring their own {@code scimTenantKeyResolver} bean. */
    ScimTenantKeyResolver scimTenantKeyResolver

    /** Injected by bean name; hosts override by declaring their own {@code scimTenantJwtSecretResolver} bean. */
    ScimTenantJwtSecretResolver scimTenantJwtSecretResolver

    ScimControllerInterceptor() {
        match controller: ~/(scimHome|scimUser|scimGroup|)/
    }

    boolean before() {
        ScimTenantContext.clear()

        if (!grailsApplication.config.getProperty("grails.scim.enabled", Boolean.class)) {
            log.debug('Scim is not enabled for this env.')
            render text: 'SCIM is not enabled', status: HttpStatus.FORBIDDEN
            return false
        }

        String bearer = extractBearer(request.getHeader("Authorization"))
        if (!bearer) {
            return reject('missing bearer')
        }

        if (legacyEnabled() && matchesLegacyToken(bearer)) {
            log.trace("Valid scim legacy token by Ip Address: {}", getClientIP(request))
            return true
        }

        String tenantId = ScimJwtParser.looksLikeJwt(bearer) ? authenticateJwt(bearer) : authenticateTenantToken(bearer)
        if (tenantId) {
            ScimTenantContext.set(tenantId)
            log.trace("Valid scim tenant token for tenant {} by Ip Address: {}", tenantId, getClientIP(request))
            return true
        }

        return reject('token mismatch')
    }

    boolean after() {
        ScimTenantContext.clear()
        true
    }

    void afterView() {
        ScimTenantContext.clear()
    }

    // ---- helpers ----

    private boolean legacyEnabled() {
        Boolean enabled = grailsApplication.config.getProperty(LEGACY_ENABLED_KEY, Boolean.class)
        enabled == null ? true : enabled
    }

    private static String extractBearer(String header) {
        if (!header) {
            return null
        }
        String[] parts = header.trim().split(/\s+/, 2)
        if (parts.length != 2 || !parts[0].equalsIgnoreCase('Bearer')) {
            return null
        }
        parts[1].trim() ?: null
    }

    private boolean matchesLegacyToken(String bearer) {
        String apiToken = grailsApplication.config.getProperty("grails.scim.api_token", String.class)
        if (!apiToken) {
            return false
        }
        MessageDigest.isEqual(bearer.getBytes(StandardCharsets.UTF_8), apiToken.getBytes(StandardCharsets.UTF_8))
    }

    /** @return the tenant id when the bearer is a valid tenant JWT, otherwise null. */
    private String authenticateJwt(String bearer) {
        if (scimTenantJwtSecretResolver == null) {
            return null
        }
        try {
            ScimJwtParser.Header header = ScimJwtParser.readHeader(bearer)
            byte[] secret = scimTenantJwtSecretResolver.resolveSecret(header.tenantId, header.kid)
            if (secret == null) {
                return null
            }
            String issuer = scimTenantJwtSecretResolver.expectedIssuer(header.tenantId)
            return ScimJwtParser.verify(bearer, secret, issuer, jwtClockSkew()).tenantId
        } catch (InvalidScimTokenException ignored) {
            return null
        }
    }

    private long jwtClockSkew() {
        Long configured = grailsApplication.config.getProperty(JWT_CLOCK_SKEW_KEY, Long.class)
        configured == null || configured < 0 ? DEFAULT_JWT_CLOCK_SKEW : configured
    }

    /** @return the tenant id when the bearer is a valid tenant token, otherwise null. */
    private String authenticateTenantToken(String bearer) {
        if (scimTenantKeyResolver == null) {
            return null
        }
        ScimTenantTokenParser.ParsedToken parsed
        try {
            parsed = ScimTenantTokenParser.parse(bearer)
        } catch (InvalidScimTokenException ignored) {
            return null
        }
        byte[] expected = scimTenantKeyResolver.resolveSecretHash(parsed.tenantId)
        byte[] presented = ScimTenantTokenParser.sha256(parsed.secret)
        if (expected != null && MessageDigest.isEqual(presented, expected)) {
            return parsed.tenantId
        }
        return null
    }

    private boolean reject(String reason) {
        log.warn("Unauthorized scim access ({}) by Ip Address: {}", reason, getClientIP(request))
        unauthorized("UNAUTHORIZED access on scim endpoint")
        return false
    }

    private String getClientIP(HttpServletRequest request) {
        String ip = request.getHeader("X-FORWARDED-FOR")
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr()
        }
        return ip
    }

    private void unauthorized(String message) {
        response.status = HttpStatus.UNAUTHORIZED.value()
        render([
                schemas: ["urn:ietf:params:scim:api:messages:2.0:Error"],
                status : HttpStatus.UNAUTHORIZED.value().toString(),
                detail : message
        ] as JSON)
    }

}
