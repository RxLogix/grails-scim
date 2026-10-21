package grails.plugins.scim.tenant

import groovy.transform.CompileStatic
import jakarta.servlet.http.HttpServletRequest
import org.springframework.web.context.request.RequestAttributes
import org.springframework.web.context.request.RequestContextHolder

/**
 * Tenant resolved by {@code ScimControllerInterceptor} for the current SCIM request (PVADMIN-50831).
 *
 * The host's {@code ScimResourceRepository} implementations read it to scope queries to the
 * tenant and to stamp the owner tenant on SCIM-created users. It is carried as a request
 * attribute, so it is only available on the request thread.
 *
 * {@link #currentTenantId()} returns {@code null} when the plugin runs without a
 * {@link ScimTenantConfigProvider} (non multi-tenant mode).
 */
@CompileStatic
final class ScimTenantContext {

    static final String TENANT_ID_ATTRIBUTE = 'grails.plugins.scim.tenantId'

    static final String LEGACY_TOKEN_ATTRIBUTE = 'grails.plugins.scim.legacyToken'

    private ScimTenantContext() {
    }

    /** Tenant the current request was authenticated for, or {@code null}. */
    static Long currentTenantId() {
        (Long) currentRequestAttributes()?.getAttribute(TENANT_ID_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST)
    }

    /** {@code true} when the current request was authenticated with the legacy (non-JWT) token. */
    static boolean isLegacyToken() {
        currentRequestAttributes()?.getAttribute(LEGACY_TOKEN_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST) == Boolean.TRUE
    }

    /** Called by the interceptor once the request is authenticated. */
    static void bind(HttpServletRequest request, Long tenantId, boolean legacyToken) {
        request.setAttribute(TENANT_ID_ATTRIBUTE, tenantId)
        request.setAttribute(LEGACY_TOKEN_ATTRIBUTE, legacyToken)
    }

    private static RequestAttributes currentRequestAttributes() {
        RequestContextHolder.getRequestAttributes()
    }

}
