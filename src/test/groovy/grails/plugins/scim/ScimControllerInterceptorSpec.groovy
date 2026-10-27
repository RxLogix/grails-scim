package grails.plugins.scim

import grails.config.Config
import grails.core.GrailsApplication
import grails.plugins.scim.security.ScimJwtParser
import grails.plugins.scim.security.ScimTenantContext
import grails.plugins.scim.security.ScimTenantJwtSecretResolver
import grails.testing.web.interceptor.InterceptorUnitTest
import groovy.json.JsonSlurper
import spock.lang.Specification

class ScimControllerInterceptorSpec extends Specification implements InterceptorUnitTest<ScimControllerInterceptor> {

    Config mockConfig = Mock(Config)
    GrailsApplication mockApplication = Mock(GrailsApplication)
    ScimTenantJwtSecretResolver jwtResolver = Mock(ScimTenantJwtSecretResolver)

    static final byte[] JWT_SECRET = 'jwt-secret-for-tenant-001-0123456789abcdef'.bytes

    void setup() {
        mockApplication.getConfig() >> mockConfig
        interceptor.grailsApplication = mockApplication
        interceptor.scimTenantJwtSecretResolver = jwtResolver
        ScimTenantContext.clear()
    }

    private static String jwt(Map headerOverrides = [:], Map payloadOverrides = [:], byte[] secret = JWT_SECRET) {
        long now = System.currentTimeMillis().intdiv(1000)
        Map header = [alg: 'HS256', typ: 'JWT', kid: 'kid-1', tid: 'tenant-001'] + headerOverrides
        Map payload = [iss: 'https://idp.example', sub: 'scim', iat: now, exp: now + 300] + payloadOverrides
        ScimJwtParser.sign(header, payload, secret)
    }

    private void jwtConfigured(String tenantId = 'tenant-001', String kid = 'kid-1', byte[] secret = JWT_SECRET,
                               String issuer = null, boolean tenantEnabled = true) {
        jwtResolver.resolveSecret(tenantId, kid) >> secret
        jwtResolver.expectedIssuer(tenantId) >> issuer
        // Per-tenant enable flag read after the signature verifies.
        mockConfig.getProperty(tenantId + '.grails.scim.enabled', _) >> tenantEnabled
    }

    void cleanup() {
        ScimTenantContext.clear()
    }

    private void scimConfigured(String token = 'secret') {
        mockConfig.getProperty('grails.scim.api_token', _) >> token
    }

    /** Config the legacy (default-tenant) flow needs: SCIM enabled + the default tenant id. */
    private void legacyConfigured(long defaultTenantId = 1L, boolean enabled = true) {
        mockConfig.getProperty('grails.scim.enabled', _) >> enabled
        mockConfig.getProperty('DEFAULT_TENANT_ID', _) >> defaultTenantId
    }

    // ------------------------
    // LEGACY STATIC TOKEN
    // ------------------------
    void "legacy token binds the default tenant when scim is enabled"() {
        given:
        scimConfigured()
        legacyConfigured(1L, true)
        request.addHeader('Authorization', 'Bearer secret')

        when:
        boolean allowed = interceptor.before()

        then:
        allowed
        ScimTenantContext.currentTenantId() == '1'
    }

    void "legacy token binds the default tenant and never consults the jwt resolver"() {
        given:
        scimConfigured()
        legacyConfigured(1L, true)
        request.addHeader('Authorization', 'Bearer secret')

        when:
        boolean allowed = interceptor.before()

        then:
        allowed
        ScimTenantContext.currentTenantId() == '1'
        0 * jwtResolver.resolveSecret(_, _)
    }

    void "legacy token is rejected with 403 when scim is disabled for the default tenant"() {
        given:
        scimConfigured()
        legacyConfigured(1L, false)
        request.addHeader('Authorization', 'Bearer secret')

        when:
        boolean allowed = interceptor.before()

        then:
        !allowed
        response.status == 403
        !ScimTenantContext.isSet()
    }

    // ------------------------
    // TENANT JWT
    // ------------------------
    void "valid tenant JWT sets tenant context"() {
        given:
        scimConfigured()
        jwtConfigured()
        request.addHeader('Authorization', "Bearer ${jwt()}")

        when:
        boolean allowed = interceptor.before()

        then:
        allowed
        ScimTenantContext.currentTenantId() == 'tenant-001'
    }

    void "valid tenant JWT works when issuer is enforced"() {
        given:
        scimConfigured()
        jwtConfigured('tenant-001', 'kid-1', JWT_SECRET, 'https://idp.example')
        request.addHeader('Authorization', "Bearer ${jwt()}")

        expect:
        interceptor.before()
        ScimTenantContext.currentTenantId() == 'tenant-001'
    }

    void "valid tenant JWT but SCIM disabled for the tenant is rejected with 403"() {
        given:
        scimConfigured()
        jwtConfigured('tenant-001', 'kid-1', JWT_SECRET, null, false)
        request.addHeader('Authorization', "Bearer ${jwt()}")

        when:
        boolean allowed = interceptor.before()

        then:
        !allowed
        response.status == 403
        def body = new JsonSlurper().parseText(response.contentAsString)
        body.schemas == ['urn:ietf:params:scim:api:messages:2.0:Error']
        body.status == '403'
        body.detail == 'SCIM is not enabled'
        !ScimTenantContext.isSet()
    }

    void "tenant JWT signed with wrong secret is rejected with 401"() {
        given:
        scimConfigured()
        jwtConfigured()
        request.addHeader('Authorization', "Bearer ${jwt([:], [:], 'wrong-secret-wrong-secret-wrong-secret'.bytes)}")

        expect:
        !interceptor.before()
        response.status == 401
        !ScimTenantContext.isSet()
    }

    void "tenant JWT with unknown kid is rejected with same 401 body"() {
        given:
        scimConfigured()
        jwtResolver.resolveSecret(_, _) >> null
        request.addHeader('Authorization', "Bearer ${jwt(kid: 'kid-unknown')}")

        when:
        boolean allowed = interceptor.before()

        then:
        !allowed
        response.status == 401
        new JsonSlurper().parseText(response.contentAsString).detail == 'UNAUTHORIZED access on scim endpoint'
    }

    void "expired tenant JWT is rejected"() {
        given:
        scimConfigured()
        jwtConfigured()
        long past = System.currentTimeMillis().intdiv(1000) - 3600
        request.addHeader('Authorization', "Bearer ${jwt([:], [exp: past])}")

        expect:
        !interceptor.before()
        response.status == 401
    }

    void "tenant JWT with issuer mismatch is rejected"() {
        given:
        scimConfigured()
        jwtConfigured('tenant-001', 'kid-1', JWT_SECRET, 'https://expected')
        request.addHeader('Authorization', "Bearer ${jwt()}")

        expect:
        !interceptor.before()
        response.status == 401
    }

    void "tenant JWT with alg none never reaches the resolver"() {
        given:
        scimConfigured()
        request.addHeader('Authorization', "Bearer ${jwt(alg: 'none')}")

        when:
        boolean allowed = interceptor.before()

        then:
        !allowed
        0 * jwtResolver.resolveSecret(_, _)
    }

    void "tenant JWT is rejected when no jwt resolver bean is present"() {
        given:
        scimConfigured()
        interceptor.scimTenantJwtSecretResolver = null
        request.addHeader('Authorization', "Bearer ${jwt()}")

        expect:
        !interceptor.before()
        response.status == 401
    }

    void "configured clock skew is honoured"() {
        given:
        scimConfigured()
        mockConfig.getProperty(ScimControllerInterceptor.JWT_CLOCK_SKEW_KEY, _) >> 600L
        jwtConfigured()
        long slightlyPast = System.currentTimeMillis().intdiv(1000) - 300
        request.addHeader('Authorization', "Bearer ${jwt([:], [exp: slightlyPast])}")

        expect:
        interceptor.before()
        ScimTenantContext.currentTenantId() == 'tenant-001'
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
        scimConfigured()

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
        scimConfigured()
        request.addHeader('Authorization', 'Bearer wrong-token')

        when:
        boolean allowed = interceptor.before()

        then:
        !allowed
        response.status == 401
    }

    void "before returns false and 401 when authorization header has no scheme"() {
        given:
        scimConfigured()
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
        scimConfigured()
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
