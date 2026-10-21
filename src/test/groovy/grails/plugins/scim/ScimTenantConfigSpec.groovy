package grails.plugins.scim

import grails.plugins.scim.tenant.ScimTenantConfig
import spock.lang.Specification

class ScimTenantConfigSpec extends Specification {

    void "toString never exposes the signing secret or the legacy token"() {
        given:
        ScimTenantConfig config = new ScimTenantConfig(
                tenantId: 5L, enabled: true, jwtSecret: 'c2lnbmluZy1zZWNyZXQ=', legacyApiToken: 'legacy-token')

        when:
        String text = config.toString()

        then:
        text.contains('tenantId:5')
        text.contains('enabled:true')
        !text.contains('c2lnbmluZy1zZWNyZXQ=')
        !text.contains('legacy-token')
    }

    void "enabled defaults to false when not supplied"() {
        expect:
        !new ScimTenantConfig(tenantId: 5L, jwtSecret: 'c2VjcmV0').enabled
    }

}
