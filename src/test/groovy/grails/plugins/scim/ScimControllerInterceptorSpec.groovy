package grails.plugins.scim

import grails.config.Config
import grails.core.GrailsApplication
import grails.plugins.scim.security.ScimTenantContext
import grails.plugins.scim.security.ScimTenantKeyResolver
import grails.plugins.scim.security.ScimTenantTokenParser
import grails.testing.web.interceptor.InterceptorUnitTest
import groovy.json.JsonSlurper
import spock.lang.Specification

class ScimControllerInterceptorSpec extends Specification implements InterceptorUnitTest<ScimControllerInterceptor> {

    Config mockConfig = Mock(Config)
    GrailsApplication mockApplication = Mock(GrailsApplication)
    ScimTenantKeyResolver resolver = Mock(ScimTenantKeyResolver)

    static final byte[] TENANT_SECRET = 'tenant-secret-bytes-0123456789ab'.bytes
    static final String TENANT_TOKEN = ScimTenantTokenParser.build('tenant-001', TENANT_SECRET)

    void setup() {
        mockApplication.getConfig() >> mockConfig
        interceptor.grailsApplication = mockApplication
        interceptor.scimTenantKeyResolver = resolver
        ScimTenantContext.clear()
    }

    void cleanup() {
        ScimTenantContext.clear()
    }

    private void scimEnabled(boolean enabled, String token = 'secret', Boolean legacyEnabled = null) {
        mockConfig.getProperty('grails.scim.enabled', _) >> enabled
        mockConfig.getProperty('grails.scim.api_token', _) >> token
        mockConfig.getProperty(ScimControllerInterceptor.LEGACY_ENABLED_KEY, _) >> legacyEnabled
    }

    private void tenantConfigured(String tenantId = 'tenant-001', byte[] secret = TENANT_SECRET) {
        resolver.resolveSecretHash(tenantId) >> ScimTenantTokenParser.sha256(secret)
    }

    // ------------------------
    // SCIM DISABLED
    // ------------------------
    void "before returns false and 403 when scim is not enabled"() {
        given:
        scimEnabled(false)

        when:
        boolean allowed = interceptor.before()

        then:
        !allowed
        response.status == 403
        response.contentAsString == 'SCIM is not enabled'
    }

    // ------------------------
    // VALID TOKEN
    // ------------------------
    void "before returns true when scim is enabled and bearer token is valid"() {
        given:
        scimEnabled(true, 'secret')
        request.addHeader('Authorization', 'Bearer secret')

        when:
        boolean allowed = interceptor.before()

        then:
        allowed
        !ScimTenantContext.isSet()
    }

    void "legacy token sets no tenant context and never consults the resolver"() {
        given:
        scimEnabled(true, 'secret', true)
        request.addHeader('Authorization', 'Bearer secret')

        when:
        boolean allowed = interceptor.before()

        then:
        allowed
        !ScimTenantContext.isSet()
        0 * resolver.resolveSecretHash(_)
    }

    void "legacy token is rejected when legacy flag is disabled"() {
        given:
        scimEnabled(true, 'secret', false)
        request.addHeader('Authorization', 'Bearer secret')

        when:
        boolean allowed = interceptor.before()

        then:
        !allowed
        response.status == 401
        !ScimTenantContext.isSet()
    }

    // ------------------------
    // TENANT TOKEN
    // ------------------------
    void "valid tenant token sets tenant context"() {
        given:
        scimEnabled(true, 'secret')
        tenantConfigured()
        request.addHeader('Authorization', "Bearer ${TENANT_TOKEN}")

        when:
        boolean allowed = interceptor.before()

        then:
        allowed
        ScimTenantContext.currentTenantId() == 'tenant-001'
    }

    void "valid tenant token works when legacy flag is disabled"() {
        given:
        scimEnabled(true, 'secret', false)
        tenantConfigured()
        request.addHeader('Authorization', "Bearer ${TENANT_TOKEN}")

        expect:
        interceptor.before()
        ScimTenantContext.currentTenantId() == 'tenant-001'
    }

    void "tenant token with wrong secret is rejected with 401"() {
        given:
        scimEnabled(true, 'secret')
        tenantConfigured('tenant-001', 'some-other-secret'.bytes)
        request.addHeader('Authorization', "Bearer ${TENANT_TOKEN}")

        when:
        boolean allowed = interceptor.before()

        then:
        !allowed
        response.status == 401
        !ScimTenantContext.isSet()
    }

    void "tenant token for unknown tenant is rejected with same 401 body"() {
        given:
        scimEnabled(true, 'secret')
        resolver.resolveSecretHash(_) >> null
        request.addHeader('Authorization', "Bearer ${TENANT_TOKEN}")

        when:
        boolean allowed = interceptor.before()

        then:
        !allowed
        response.status == 401
        def body = new JsonSlurper().parseText(response.contentAsString)
        body.detail == 'UNAUTHORIZED access on scim endpoint'
        !ScimTenantContext.isSet()
    }

    void "malformed tenant token is rejected without consulting the resolver"() {
        given:
        scimEnabled(true, 'secret')
        request.addHeader('Authorization', 'Bearer not.a.valid.token')

        when:
        boolean allowed = interceptor.before()

        then:
        !allowed
        response.status == 401
        0 * resolver.resolveSecretHash(_)
    }

    void "tenant token is rejected when no resolver bean is present"() {
        given:
        scimEnabled(true, 'secret')
        interceptor.scimTenantKeyResolver = null
        request.addHeader('Authorization', "Bearer ${TENANT_TOKEN}")

        expect:
        !interceptor.before()
        response.status == 401
    }

    void "after and afterView clear the tenant context"() {
        given:
        ScimTenantContext.set('tenant-001')

        when:
        boolean result = interceptor.after()

        then:
        result
        !ScimTenantContext.isSet()

        when:
        ScimTenantContext.set('tenant-001')
        interceptor.afterView()

        then:
        !ScimTenantContext.isSet()
    }

    // ------------------------
    // MISSING / INVALID TOKEN
    // ------------------------
    void "before returns false and 401 when authorization header is missing"() {
        given:
        scimEnabled(true, 'secret')

        when:
        boolean allowed = interceptor.before()

        then:
        !allowed
        response.status == 401
        def body = new JsonSlurper().parseText(response.contentAsString)
        body.schemas == ['urn:ietf:params:scim:api:messages:2.0:Error']
        body.status == '401'
        body.detail == 'UNAUTHORIZED access on scim endpoint'
    }

    void "before returns false and 401 when bearer token does not match"() {
        given:
        scimEnabled(true, 'secret')
        request.addHeader('Authorization', 'Bearer wrong-token')

        when:
        boolean allowed = interceptor.before()

        then:
        !allowed
        response.status == 401
    }

    void "before returns false and 401 when authorization header has no scheme"() {
        given:
        scimEnabled(true, 'secret')
        // No scheme -> not a Bearer header
        request.addHeader('Authorization', 'secret')

        when:
        boolean allowed = interceptor.before()

        then:
        !allowed
        response.status == 401
    }

    void "before returns false and 401 when scheme is not Bearer"() {
        given:
        scimEnabled(true, 'secret')
        request.addHeader('Authorization', 'Basic secret')

        expect:
        !interceptor.before()
        response.status == 401
    }

    // ------------------------
    // MATCHING
    // ------------------------
    void "interceptor matches scim controllers"() {
        when:
        withRequest(controller: controllerName)

        then:
        interceptor.doesMatch()

        where:
        controllerName << ['scimHome', 'scimUser', 'scimGroup']
    }
}