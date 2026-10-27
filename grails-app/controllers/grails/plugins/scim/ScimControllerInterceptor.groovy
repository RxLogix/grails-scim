package grails.plugins.scim

import grails.converters.JSON
import grails.core.GrailsApplication
import grails.plugins.scim.security.InvalidScimTokenException
import grails.plugins.scim.security.ScimJwtParser
import grails.plugins.scim.security.ScimTenantContext
import grails.plugins.scim.security.ScimTenantJwtSecretResolver
import groovy.util.logging.Slf4j
import org.springframework.http.HttpStatus
import jakarta.servlet.http.HttpServletRequest

import java.nio.charset.StandardCharsets
import java.security.MessageDigest

/**
 * Authenticates SCIM requests.
 *
 * Two mutually exclusive flows, tried in this order:
 * <ul>
 *   <li><b>Legacy static token</b>: the bearer value matches {@code grails.scim.api_token}. No
 *       tenant context is set and payload tenant fields are honoured as sent.</li>
 *   <li><b>Tenant JWT</b>: an HS256 JWS whose header carries {@code tid} and {@code kid}. The secret
 *       comes from {@link ScimTenantJwtSecretResolver}; signature, {@code exp}, {@code nbf} and optional
 *       issuer are validated by {@link ScimJwtParser}. After the signature verifies, SCIM must be
 *       enabled for that tenant ({@code <tenantId>.grails.scim.enabled}) or the request is 403.</li>
 * </ul>
 * The JWT flow publishes the tenant via {@link ScimTenantContext} for the rest of the request.
 */
@Slf4j
class ScimControllerInterceptor {

    static final String JWT_CLOCK_SKEW_KEY = 'grails.scim.jwt.clockSkewSeconds'
    static final long DEFAULT_JWT_CLOCK_SKEW = 60L

    GrailsApplication grailsApplication

    /** Injected by bean name; hosts override by declaring their own {@code scimTenantJwtSecretResolver} bean. */
    ScimTenantJwtSecretResolver scimTenantJwtSecretResolver

    ScimControllerInterceptor() {
        match controller: ~/(scimHome|scimUser|scimGroup|)/
    }

    boolean before() {
        ScimTenantContext.clear()

        String bearer = extractBearer(request.getHeader("Authorization"))
        if (!bearer) {
            return reject('missing bearer')
        }

        String tenantId

        if (matchesLegacyToken(bearer)) {
            log.trace("Valid scim legacy token by Ip Address: {}", getClientIP(request))
            if (!scimEnabled()) {
                log.info("Scim is not enabled for default tenant by Ip Address: {}", getClientIP(request))
                forbidden("SCIM is not enabled")
                return false
            }
            tenantId = grailsApplication.config.getProperty('DEFAULT_TENANT_ID', Long.class).toString()
        }
        else if (ScimJwtParser.looksLikeJwt(bearer)) {
            tenantId = authenticateJwt(bearer)
            if (!tenantId) {
                return reject('token mismatch')
            }

            // Valid per-tenant JWT: SCIM must be enabled for that tenant (checked after verification).
            if (!scimEnabledForTenant(tenantId)) {
                log.info("Scim is not enabled for tenant {} by Ip Address: {}", tenantId, getClientIP(request))
                forbidden("SCIM is not enabled")
                return false
            }
        }
        else {
            return reject('token mismatch')
        }



        ScimTenantContext.set(tenantId)
        log.trace("Valid scim token for tenant {} by Ip Address: {}", tenantId, getClientIP(request))
        return true
    }

    boolean after() {
        ScimTenantContext.clear()
        true
    }

    void afterView() {
        ScimTenantContext.clear()
    }

    // ---- helpers ----

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

    /** Whether SCIM is enabled for the tenant: {@code <tenantId>.grails.scim.enabled} is true. */
    private boolean scimEnabledForTenant(String tenantId) {
        Boolean enabled = grailsApplication.config.getProperty(tenantId + '.grails.scim.enabled', Boolean.class)
        enabled != null && enabled
    }

    private boolean scimEnabled() {
        Boolean enabled = grailsApplication.config.getProperty('grails.scim.enabled', Boolean.class)
        enabled != null && enabled
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

    private void forbidden(String message) {
        response.status = HttpStatus.FORBIDDEN.value()
        render([
                schemas: ["urn:ietf:params:scim:api:messages:2.0:Error"],
                status : HttpStatus.FORBIDDEN.value().toString(),
                detail : message
        ] as JSON)
    }

}
