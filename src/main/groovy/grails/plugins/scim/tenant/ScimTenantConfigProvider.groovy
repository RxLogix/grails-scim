package grails.plugins.scim.tenant

import groovy.transform.CompileStatic

/**
 * Host extension point for multi-tenant SCIM authentication (PVADMIN-50831).
 *
 * The plugin has no persistence and no access to CMT, so the host application implements this
 * interface and registers it as a Spring bean named {@code scimTenantConfigProvider}
 * (resources.groovy, next to {@code scimUserRepository} / {@code scimGroupRepository}).
 *
 * <ul>
 *     <li>When the bean is present, {@code ScimControllerInterceptor} authenticates every request with
 *     the per-tenant JWT minted by New Admin, or with the legacy token for the default tenant.</li>
 *     <li>When it is absent, the plugin keeps the previous single static-token behaviour
 *     ({@code grails.scim.enabled} + {@code grails.scim.api_token}).</li>
 * </ul>
 *
 * The plugin calls the provider on every request and does not cache the result, so a secret
 * rotation takes effect as soon as the host's own view of CMT is refreshed (on
 * {@code CMT_CONFIG_CHANGED{SCIM}}). Implementations are expected to cache.
 */
@CompileStatic
interface ScimTenantConfigProvider {

    /**
     * @param tenantId tenant taken from the (not yet verified) token
     * @return the tenant's SCIM config, or {@code null} when the tenant is unknown
     */
    ScimTenantConfig getTenantConfig(Long tenantId)

    /**
     * Config of the org / default tenant, used for the legacy (non-JWT) token path.
     *
     * @return the default tenant's config with {@code legacyApiToken} set, or {@code null}
     *         (or a config without {@code legacyApiToken}) to reject legacy tokens
     */
    ScimTenantConfig getDefaultTenantConfig()

}
