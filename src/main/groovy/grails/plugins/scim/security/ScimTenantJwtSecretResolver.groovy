package grails.plugins.scim.security

import groovy.transform.CompileStatic

/**
 * Resolves the raw HMAC secret used to verify a tenant's signed JWT.
 * Unlike {@link ScimTenantKeyResolver}, the secret must be returned in full because
 * HMAC verification needs the key bytes; hosts must therefore source it from a vault
 * or environment, never from committed config.
 */
@CompileStatic
interface ScimTenantJwtSecretResolver {

    /**
     * @param tenantId tenant id from the JWT header, already pattern-validated
     * @param kid      key id from the JWT header, already pattern-validated
     * @return the raw secret bytes (at least 32), or null when the tenant or key is unknown.
     *         Implementations must return null rather than throw.
     */
    byte[] resolveSecret(String tenantId, String kid)

    /**
     * @return the issuer the tenant's tokens must carry in {@code iss}, or null when not enforced.
     */
    String expectedIssuer(String tenantId)
}
