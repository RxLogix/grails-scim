package grails.plugins.scim.tenant

import groovy.transform.CompileStatic
import groovy.transform.MapConstructor
import groovy.transform.ToString

/**
 * SCIM configuration of one tenant for the current application, as supplied by the host through
 * {@link ScimTenantConfigProvider} (PVADMIN-50831).
 *
 * The host builds it from the per-(tenant, app) CMT keys:
 * <ul>
 *     <li>{@code enabled}        &lt;- {@code grails.scim.enabled}</li>
 *     <li>{@code jwtSecret}      &lt;- {@code grails.scim.jwt.secret} (base64, exactly as New Admin stores it)</li>
 *     <li>{@code legacyApiToken} &lt;- the old static SCIM token; only meaningful on the default tenant's config</li>
 * </ul>
 *
 * Both secrets are excluded from {@code toString()} so the object can never leak them into logs.
 */
@CompileStatic
@MapConstructor
@ToString(includePackage = false, includeNames = true, excludes = ['jwtSecret', 'legacyApiToken'])
class ScimTenantConfig {

    final Long tenantId

    final boolean enabled

    final String jwtSecret

    final String legacyApiToken

}
