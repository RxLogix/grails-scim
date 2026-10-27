package grails.plugins.scim.security

import groovy.transform.CompileStatic

/**
 * Request-scoped holder for the tenant of the current SCIM request.
 * <ul>
 *   <li><b>Authoritative</b> (tenant token / tenant JWT): the tenant came from the credential.
 *       Controllers overwrite payload tenant fields with it.</li>
 *   <li><b>Default</b> (legacy static token with {@code DEFAULT_TENANT_ID}):
 *       the tenant is a configured fallback so repositories always have a context. Controllers
 *       still honour payload tenant fields as sent.</li>
 * </ul>
 * Not set for legacy requests when no default tenant is configured.
 * Must be cleared by the interceptor at the end of every request.
 */
@CompileStatic
final class ScimTenantContext {

    private static final ThreadLocal<Entry> TENANT = new ThreadLocal<>()

    private ScimTenantContext() {}

    /** Sets an authoritative tenant (credential-derived). */
    static void set(String tenantId) {
        set(tenantId, true)
    }

    static void set(String tenantId, boolean authoritative) {
        if (tenantId) {
            TENANT.set(new Entry(tenantId, authoritative))
        } else {
            TENANT.remove()
        }
    }

    /** @return the tenant id for this request (authoritative or default), or null when none is set. */
    static String currentTenantId() {
        TENANT.get()?.tenantId
    }

    /** @return the tenant id only when it was derived from the credential, otherwise null. */
    static String authoritativeTenantId() {
        Entry entry = TENANT.get()
        entry != null && entry.authoritative ? entry.tenantId : null
    }

    static boolean isSet() {
        TENANT.get() != null
    }

    /** @return true when the tenant came from a tenant token or tenant JWT rather than the legacy default. */
    static boolean isAuthoritative() {
        TENANT.get()?.authoritative ?: false
    }

    static void clear() {
        TENANT.remove()
    }

    private static final class Entry {
        final String tenantId
        final boolean authoritative

        Entry(String tenantId, boolean authoritative) {
            this.tenantId = tenantId
            this.authoritative = authoritative
        }
    }
}
