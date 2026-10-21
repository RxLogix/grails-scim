package grails.plugins.scim

import grails.converters.JSON
import grails.core.GrailsApplication
import grails.plugins.scim.auth.ScimTokenVerifier
import grails.plugins.scim.exceptions.ScimAuthenticationException
import grails.plugins.scim.exceptions.ScimAuthenticationException.Reason
import grails.plugins.scim.tenant.ScimTenantConfig
import grails.plugins.scim.tenant.ScimTenantConfigProvider
import grails.plugins.scim.tenant.ScimTenantContext
import groovy.util.logging.Slf4j
import org.springframework.http.HttpStatus
import jakarta.servlet.http.HttpServletRequest

import java.nio.charset.StandardCharsets
import java.security.MessageDigest

/**
 * Authenticates every SCIM request.
 *
 * Two modes (PVADMIN-50831):
 * <ul>
 *     <li><b>Multi-tenant</b> - the host registers a {@code scimTenantConfigProvider} bean. A JWT bearer is
 *     verified with its tenant's secret (New Admin per-tenant token); a non-JWT bearer is checked
 *     against the default tenant's legacy token. The resolved tenant is published via
 *     {@link ScimTenantContext}.</li>
 *     <li><b>Static token</b> - no provider bean (host not migrated yet). Previous behaviour: global
 *     {@code grails.scim.enabled} and {@code grails.scim.api_token}.</li>
 * </ul>
 * Authentication failures always answer the same generic SCIM 401, and the bearer value is never logged.
 */
@Slf4j
class ScimControllerInterceptor {

    /**
     * Multi-tenant mode only: switches the legacy (non-JWT) token path on or off. Defaults to {@code true}
     * when not set, so the old static token keeps working during the cutover; set it to {@code false}
     * once every SCIM client uses its per-tenant JWT. Static-token mode (no provider bean) is not affected.
     */
    static final String LEGACY_TOKEN_ENABLED_KEY = 'grails.scim.legacyToken.enabled'

    private static final String BEARER_PREFIX = 'Bearer '
    private static final String UNAUTHORIZED_DETAIL = 'UNAUTHORIZED access on scim endpoint'
    private static final String FORBIDDEN_DETAIL = 'SCIM is not enabled'
    private static final String UNAVAILABLE_DETAIL = 'SCIM authentication is currently unavailable'

    GrailsApplication grailsApplication

    /**
     * Optional host bean. Grails autowires interceptors by name, so this stays {@code null} when the
     * host does not define {@code scimTenantConfigProvider}, which selects static-token mode.
     */
    ScimTenantConfigProvider scimTenantConfigProvider

    private final ScimTokenVerifier tokenVerifier = new ScimTokenVerifier()

    ScimControllerInterceptor() {
        match controller: ~/(scimHome|scimUser|scimGroup|)/
    }

    boolean before() {
        String token = extractBearerToken(request)
        if (scimTenantConfigProvider == null) {
            return authenticateStaticToken(token)
        }
        try {
            // Reject a missing bearer up front: it must not reach the legacy path, where a failing
            // provider lookup would turn an unauthenticated request into a 500 instead of a 401.
            if (token == null) {
                throw new ScimAuthenticationException(Reason.MISSING_TOKEN)
            }
            return tokenVerifier.isJwt(token) ? authenticateJwt(token) : authenticateLegacyToken(token)
        } catch (ScimAuthenticationException authException) {
            log.warn("Rejected scim token ({}) from Ip Address: {}", authException.reason, getClientIP(request))
            renderScimError(HttpStatus.UNAUTHORIZED, UNAUTHORIZED_DETAIL)
            return false
        } catch (Exception providerException) {
            // Fail closed when the host cannot supply tenant config (e.g. CMT unavailable).
            log.error("Unable to load scim tenant config for request from Ip Address: {}", getClientIP(request), providerException)
            renderScimError(HttpStatus.INTERNAL_SERVER_ERROR, UNAVAILABLE_DETAIL)
            return false
        }
    }

    boolean after() { true }

    void afterView() {
        // no-op
    }

    /**
     * Per-tenant JWT: the unverified tenantId only selects the secret; the request is authenticated
     * only after the signature is verified, and then only if SCIM is enabled for that tenant.
     */
    private boolean authenticateJwt(String token) {
        Long tenantId = tokenVerifier.readUnverifiedTenantId(token)
        ScimTenantConfig tenantConfig = scimTenantConfigProvider.getTenantConfig(tenantId)
        if (!tenantConfig?.jwtSecret) {
            throw new ScimAuthenticationException(Reason.UNKNOWN_TENANT)
        }
        tokenVerifier.verify(token, tenantConfig.jwtSecret, tenantId)
        if (!tenantConfig.enabled) {
            log.info("Scim is not enabled for tenant {}", tenantId)
            renderScimError(HttpStatus.FORBIDDEN, FORBIDDEN_DETAIL)
            return false
        }
        ScimTenantContext.bind(request, tenantId, false)
        log.trace("Valid scim token for tenant {} by Ip Address: {}", tenantId, getClientIP(request))
        return true
    }

    /**
     * Legacy (non-JWT) token kept for the cutover: maps to the org / default tenant (non-MT).
     * Accepted only while {@link #LEGACY_TOKEN_ENABLED_KEY} is on AND the host supplies the token.
     */
    private boolean authenticateLegacyToken(String token) {
        // Checked before the provider call so a switched-off legacy path never touches host config.
        if (!legacyTokenEnabled()) {
            throw new ScimAuthenticationException(Reason.INVALID_LEGACY_TOKEN)
        }
        ScimTenantConfig defaultConfig = scimTenantConfigProvider.getDefaultTenantConfig()
        if (!defaultConfig?.legacyApiToken || !constantTimeEquals(token, defaultConfig.legacyApiToken)) {
            throw new ScimAuthenticationException(Reason.INVALID_LEGACY_TOKEN)
        }
        if (!defaultConfig.enabled) {
            log.info("Scim is not enabled for default tenant {}", defaultConfig.tenantId)
            renderScimError(HttpStatus.FORBIDDEN, FORBIDDEN_DETAIL)
            return false
        }
        ScimTenantContext.bind(request, defaultConfig.tenantId, true)
        log.trace("Valid legacy scim token for default tenant {} by Ip Address: {}", defaultConfig.tenantId, getClientIP(request))
        return true
    }

    /**
     * Non multi-tenant hosts: unchanged contract (global enable flag + single static token),
     * now compared in constant time.
     */
    private boolean authenticateStaticToken(String token) {
        if (!grailsApplication.config.getProperty("grails.scim.enabled", Boolean.class)) {
            log.debug('Scim is not enabled for this env.')
            renderScimError(HttpStatus.FORBIDDEN, FORBIDDEN_DETAIL)
            return false
        }
        String apiToken = grailsApplication.getConfig().getProperty("grails.scim.api_token", String.class)
        if (apiToken && constantTimeEquals(token, apiToken)) {
            log.trace("Valid scim access token by Ip Address: {}", getClientIP(request))
            return true
        }
        // The Authorization header used to be logged here; it carried the token, so it is no longer logged.
        log.warn("Invalid scim token by Ip Address: {}", getClientIP(request))
        renderScimError(HttpStatus.UNAUTHORIZED, UNAUTHORIZED_DETAIL)
        return false
    }

    /** Read per request, so a host that reloads its config at runtime does not need a restart. */
    private boolean legacyTokenEnabled() {
        Boolean enabled = grailsApplication.config.getProperty(LEGACY_TOKEN_ENABLED_KEY, Boolean.class)
        return enabled == null ? true : enabled
    }

    private static String extractBearerToken(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization")
        if (!authorization || !authorization.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            return null
        }
        String token = authorization.substring(BEARER_PREFIX.length()).trim()
        return token ?: null
    }

    /** Avoids leaking how much of the token matched through response timing. */
    private static boolean constantTimeEquals(String presented, String expected) {
        if (presented == null || expected == null) {
            return false
        }
        return MessageDigest.isEqual(presented.getBytes(StandardCharsets.UTF_8), expected.getBytes(StandardCharsets.UTF_8))
    }

    private String getClientIP(HttpServletRequest request) {
        String ip = request.getHeader("X-FORWARDED-FOR")
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr()
        }
        return ip
    }

    private void renderScimError(HttpStatus status, String detail) {
        response.status = status.value()
        render([
                schemas: ["urn:ietf:params:scim:api:messages:2.0:Error"],
                status : status.value().toString(),
                detail : detail
        ] as JSON)
    }

}
