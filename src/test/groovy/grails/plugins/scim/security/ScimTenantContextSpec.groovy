package grails.plugins.scim.security

import spock.lang.Specification

class ScimTenantContextSpec extends Specification {

    void cleanup() {
        ScimTenantContext.clear()
    }

    void "set, read and clear on the current thread"() {
        expect:
        !ScimTenantContext.isSet()
        ScimTenantContext.currentTenantId() == null

        when:
        ScimTenantContext.set('tenant-001')

        then:
        ScimTenantContext.isSet()
        ScimTenantContext.currentTenantId() == 'tenant-001'

        when:
        ScimTenantContext.clear()

        then:
        !ScimTenantContext.isSet()
    }

    void "single-arg set is authoritative"() {
        when:
        ScimTenantContext.set('tenant-001')

        then:
        ScimTenantContext.isAuthoritative()
        ScimTenantContext.authoritativeTenantId() == 'tenant-001'
    }

    void "default tenant is visible via currentTenantId but not authoritativeTenantId"() {
        when:
        ScimTenantContext.set('tenant-default', false)

        then:
        ScimTenantContext.isSet()
        !ScimTenantContext.isAuthoritative()
        ScimTenantContext.currentTenantId() == 'tenant-default'
        ScimTenantContext.authoritativeTenantId() == null
    }

    void "setting null or empty clears the context"() {
        given:
        ScimTenantContext.set('tenant-001')

        when:
        ScimTenantContext.set(value)

        then:
        !ScimTenantContext.isSet()

        where:
        value << [null, '']
    }

    void "context is not visible from another thread"() {
        given:
        ScimTenantContext.set('tenant-001')
        String seen = 'unset'

        when:
        Thread t = new Thread({ seen = ScimTenantContext.currentTenantId() })
        t.start(); t.join()

        then:
        seen == null
    }
}
