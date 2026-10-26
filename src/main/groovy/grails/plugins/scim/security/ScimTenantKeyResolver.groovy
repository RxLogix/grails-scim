package grails.plugins.scim.security

import groovy.transform.CompileStatic

/**
 * Resolves the stored SHA-256 hash of a tenant's SCIM secret.
 * Hosts may override the default {@link ConfigScimTenantKeyResolver} bean with a
 * database, cache or vault backed implementation.
 */
@CompileStatic
interface ScimTenantKeyResolver {

    /**
     * @param tenantId a tenant id that has already passed {@link ScimTenantTokenParser} validation
     * @return the SHA-256 digest of the tenant's secret, or null when the tenant is unknown or disabled.
     *         Implementations must return null rather than throw so the failure path stays uniform.
     */
    byte[] resolveSecretHash(String tenantId)
}
