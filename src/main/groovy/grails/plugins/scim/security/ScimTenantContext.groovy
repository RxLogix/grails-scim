package grails.plugins.scim.security

import groovy.transform.CompileStatic

/**
 * Request-scoped holder for the tenant authenticated via a tenant token.
 * Not set for requests authenticated with the legacy static token.
 * Must be cleared by the interceptor at the end of every request.
 */
@CompileStatic
final class ScimTenantContext {

    private static final ThreadLocal<String> TENANT = new ThreadLocal<>()

    private ScimTenantContext() {}

    static void set(String tenantId) {
        if (tenantId) {
            TENANT.set(tenantId)
        } else {
            TENANT.remove()
        }
    }

    /** @return the authenticated tenant id, or null when the request came through the legacy flow. */
    static String currentTenantId() {
        TENANT.get()
    }

    static boolean isSet() {
        TENANT.get() != null
    }

    static void clear() {
        TENANT.remove()
    }
}
