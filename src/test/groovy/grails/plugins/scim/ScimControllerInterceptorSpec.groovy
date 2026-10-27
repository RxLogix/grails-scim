package grails.plugins.scim

import grails.config.Config
import grails.core.GrailsApplication
import grails.plugins.scim.security.ScimJwtParser
import grails.plugins.scim.security.ScimTenantContext
import grails.plugins.scim.security.ScimTenantJwtSecretResolver
import grails.plugins.scim.security.ScimTenantKeyResolver
import grails.plugins.scim.security.ScimTenantTokenParser
import grails.testing.web.interceptor.InterceptorUnitTest
import groovy.json.JsonSlurper
import spock.lang.Specification

class ScimControllerInterceptorSpec extends Specification implements InterceptorUnitTest<ScimControllerInterceptor> {

    Config mockConfig = Mock(Config)
    GrailsApplication mockApplication = Mock(GrailsApplication)
    ScimTenantKeyResolver resolver = Mock(ScimTenantKeyResolver)
    ScimTenantJwtSecretResolver jwtResolver = Mock(ScimTenantJwtSecretResolver)

    static final byte[] JWT_SECRET = 'jwt-secret-for-tenant-001-0123456789abcdef'.bytes

    static final byte[] TENANT_SECRET = 'tenant-secret-bytes-0123456789ab'.bytes
    static final String TENANT_TOKEN = ScimTenantTokenParser.build('tenant-001', TENANT_SECRET)

    void setup() {
        mockApplication.getConfig() >> mockConfig
        interceptor.grailsApplication = mockApplication
        interceptor.scimTenantKeyResolver = resolver
        interceptor.scimTenantJwtSecretResolver = jwtResolver
        ScimTenantContext.clear()
    }

    private static String jwt(Map headerOverrides = [:], Map payloadOverrides = [:], byte[] secret = JWT_SECRET) {
        long now = System.currentTimeMillis().intdiv(1000)
        Map header = [alg: 'HS256', typ: 'JWT', kid: 'kid-1', tid: 'tenant-001'] + headerOverrides
        Map payload = [iss: 'https://idp.example', sub: 'scim', iat: now, exp: now + 300] + payloadOverrides
        ScimJwtParser.sign(header, payload, secret)
    }

    private void jwtConfigured(String tenantId = 'tenant-001', String kid = 'kid-1', byte[] secret = JWT_SECRET, String issuer = null) {
        jwtResolver.resolveSecret(tenantId, kid) >> secret
        jwtResolver.expectedIssuer(tenantId) >> issuer
    }

    void cleanup() {
        ScimTenantContext.clear()
    }

    private void scimEnabled(boolean enabled, String token = 'secret', Boolean legacyEnabled = null, String defaultTenant = null) {
        mockConfig.getProperty('grails.scim.enabled', _) >> enabled
        mockConfig.getProperty('grails.scim.api_token', _) >> token
        mockConfig.getProperty(ScimControllerInterceptor.LEGACY_ENABLED_KEY, _) >> legacyEnabled
        mockConfig.getProperty(ScimControllerInterceptor.LEGACY_DEFAULT_TENANT_KEY, _) >> defaultTenant
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

    void "legacy token sets a non-authoritative default tenant context when configured"() {
        given:
        scimEnabled(true, 'secret', true, 'tenant-default')
        request.addHeader('Authorization', 'Bearer secret')

        when:
        boolean allowed = interceptor.before()

        then:
        allowed
        ScimTenantContext.isSet()
        !ScimTenantContext.isAuthoritative()
        ScimTenantContext.currentTenantId() == 'tenant-default'
        ScimTenantContext.authoritativeTenantId() == null
        0 * resolver.resolveSecretHash(_)
    }

    void "legacy default tenant is ignored when it is not a valid tenant id"() {
        given:
        scimEnabled(true, 'secret', true, 'bad tenant/id')
        request.addHeader('Authorization', 'Bearer secret')

        when:
        boolean allowed = interceptor.before()

        then:
        allowed
        !ScimTenantContext.isSet()
    }

    void "legacy default tenant does not apply to tenant token flow"() {
        given:
        scimEnabled(true, 'secret', true, 'tenant-default')
        tenantConfigured()
        request.addHeader('Authorization', "Bearer ${ScimTenantTokenParser.build('tenant-001', TENANT_SECRET)}")

        when:
        boolean allowed = interceptor.before()

        then:
        allowed
        ScimTenantContext.isAuthoritative()
        ScimTenantContext.currentTenantId() == 'tenant-001'
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

    // ------------------------
    // TENANT JWT
    // ------------------------
    void "valid tenant JWT sets tenant context"() {
        given:
        scimEnabled(true, 'secret')
        jwtConfigured()
        request.addHeader('Authorization', "Bearer ${jwt()}")

        when:
        boolean allowed = interceptor.before()

        then:
        allowed
        ScimTenantContext.currentTenantId() == 'tenant-001'
        0 * resolver.resolveSecretHash(_)
    }

    void "valid tenant JWT works when legacy flag is disabled and issuer is enforced"() {
        given:
        scimEnabled(true, 'secret', false)
        jwtConfigured('tenant-001', 'kid-1', JWT_SECRET, 'https://idp.example')
        request.addHeader('Authorization', "Bearer ${jwt()}")

        expect:
        interceptor.before()
        ScimTenantContext.currentTenantId() == 'tenant-001'
    }

    void "tenant JWT signed with wrong secret is rejected with 401"() {
        given:
        scimEnabled(true, 'secret')
        jwtConfigured()
        request.addHeader('Authorization', "Bearer ${jwt([:], [:], 'wrong-secret-wrong-secret-wrong-secret'.bytes)}")

        expect:
        !interceptor.before()
        response.status == 401
        !ScimTenantContext.isSet()
    }

    void "tenant JWT with unknown kid is rejected with same 401 body"() {
        given:
        scimEnabled(true, 'secret')
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
        scimEnabled(true, 'secret')
        jwtConfigured()
        long past = System.currentTimeMillis().intdiv(1000) - 3600
        request.addHeader('Authorization', "Bearer ${jwt([:], [exp: past])}")

        expect:
        !interceptor.before()
        response.status == 401
    }

    void "tenant JWT with issuer mismatch is rejected"() {
        given:
        scimEnabled(true, 'secret')
        jwtConfigured('tenant-001', 'kid-1', JWT_SECRET, 'https://expected')
        request.addHeader('Authorization', "Bearer ${jwt()}")

        expect:
        !interceptor.before()
        response.status == 401
    }

    void "tenant JWT with alg none never reaches the resolver"() {
        given:
        scimEnabled(true, 'secret')
        request.addHeader('Authorization', "Bearer ${jwt(alg: 'none')}")

        when:
        boolean allowed = interceptor.before()

        then:
        !allowed
        0 * jwtResolver.resolveSecret(_, _)
    }

    void "tenant JWT is rejected when no jwt resolver bean is present"() {
        given:
        scimEnabled(true, 'secret')
        interceptor.scimTenantJwtSecretResolver = null
        request.addHeader('Authorization', "Bearer ${jwt()}")

        expect:
        !interceptor.before()
        response.status == 401
    }

    void "three-part bearer is routed to JWT flow and never to tenant token flow"() {
        given:
        scimEnabled(true, 'secret')
        jwtResolver.resolveSecret(_, _) >> null
        request.addHeader('Authorization', "Bearer ${jwt()}")

        when:
        interceptor.before()

        then:
        0 * resolver.resolveSecretHash(_)
    }

    void "configured clock skew is honoured"() {
        given:
        scimEnabled(true, 'secret')
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