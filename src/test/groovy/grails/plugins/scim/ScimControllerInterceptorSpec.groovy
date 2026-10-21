package grails.plugins.scim

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import grails.config.Config
import grails.core.GrailsApplication
import grails.plugins.scim.tenant.ScimTenantConfig
import grails.plugins.scim.tenant.ScimTenantConfigProvider
import grails.plugins.scim.tenant.ScimTenantContext
import grails.testing.web.interceptor.InterceptorUnitTest
import groovy.json.JsonSlurper
import org.slf4j.LoggerFactory
import spock.lang.Specification

class ScimControllerInterceptorSpec extends Specification implements InterceptorUnitTest<ScimControllerInterceptor> {

    Config mockConfig = Mock(Config)
    GrailsApplication mockApplication = Mock(GrailsApplication)

    void setup() {
        mockApplication.getConfig() >> mockConfig
        interceptor.grailsApplication = mockApplication
    }

    private void scimEnabled(boolean enabled, String token = 'secret') {
        mockConfig.getProperty('grails.scim.enabled', _) >> enabled
        mockConfig.getProperty('grails.scim.api_token', _) >> token
    }

    private Map scimError() {
        new JsonSlurper().parseText(response.contentAsString) as Map
    }

    // ========================================================================
    // STATIC-TOKEN MODE (no scimTenantConfigProvider bean - non multi-tenant hosts)
    // ========================================================================

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
        scimError().schemas == ['urn:ietf:params:scim:api:messages:2.0:Error']
        scimError().status == '403'
        scimError().detail == 'SCIM is not enabled'
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
        ScimTenantContext.currentTenantId() == null
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
        request.addHeader('Authorization', 'secret')

        when:
        boolean allowed = interceptor.before()

        then:
        !allowed
        response.status == 401
    }

    void "before returns false and 401 when no api token is configured"() {
        given:
        scimEnabled(true, null)
        request.addHeader('Authorization', 'Bearer null')

        when:
        boolean allowed = interceptor.before()

        then:
        !allowed
        response.status == 401
    }

    // ========================================================================
    // MULTI-TENANT MODE (host registers scimTenantConfigProvider) - PVADMIN-50831
    // ========================================================================

    ScimTenantConfigProvider provider = Mock(ScimTenantConfigProvider)
    String tenantSecret = ScimTestTokens.newSecret()

    private void multiTenant() {
        interceptor.scimTenantConfigProvider = provider
    }

    // ------------------------
    // JWT PATH
    // ------------------------
    void "valid tenant JWT is accepted and the tenant is published to the host"() {
        given:
        multiTenant()
        provider.getTenantConfig(5L) >> new ScimTenantConfig(tenantId: 5L, enabled: true, jwtSecret: tenantSecret)
        request.addHeader('Authorization', "Bearer ${ScimTestTokens.mint(tenantSecret, 5L)}")

        when:
        boolean allowed = interceptor.before()

        then:
        allowed
        ScimTenantContext.currentTenantId() == 5L
        !ScimTenantContext.isLegacyToken()
        request.getAttribute(ScimTenantContext.TENANT_ID_ATTRIBUTE) == 5L
    }

    void "the global enable flag is not consulted in multi-tenant mode"() {
        given:
        multiTenant()
        provider.getTenantConfig(5L) >> new ScimTenantConfig(tenantId: 5L, enabled: true, jwtSecret: tenantSecret)
        request.addHeader('Authorization', "Bearer ${ScimTestTokens.mint(tenantSecret, 5L)}")

        when:
        boolean allowed = interceptor.before()

        then:
        allowed
        0 * mockConfig.getProperty('grails.scim.enabled', _)
    }

    void "valid JWT for a tenant with scim disabled returns 403"() {
        given:
        multiTenant()
        provider.getTenantConfig(5L) >> new ScimTenantConfig(tenantId: 5L, enabled: false, jwtSecret: tenantSecret)
        request.addHeader('Authorization', "Bearer ${ScimTestTokens.mint(tenantSecret, 5L)}")

        when:
        boolean allowed = interceptor.before()

        then:
        !allowed
        response.status == 403
        scimError().status == '403'
        ScimTenantContext.currentTenantId() == null
    }

    void "JWT for an unknown tenant or a tenant without secret returns 401"() {
        given:
        multiTenant()
        provider.getTenantConfig(5L) >> config
        request.addHeader('Authorization', "Bearer ${ScimTestTokens.mint(tenantSecret, 5L)}")

        when:
        boolean allowed = interceptor.before()

        then:
        !allowed
        response.status == 401
        scimError().detail == 'UNAUTHORIZED access on scim endpoint'

        where:
        config << [null, new ScimTenantConfig(tenantId: 5L, enabled: true, jwtSecret: null)]
    }

    void "JWT signed with a rotated secret returns 401"() {
        given: 'the tenant secret was regenerated after the token was issued'
        multiTenant()
        String oldSecret = ScimTestTokens.newSecret()
        provider.getTenantConfig(5L) >> new ScimTenantConfig(tenantId: 5L, enabled: true, jwtSecret: tenantSecret)
        request.addHeader('Authorization', "Bearer ${ScimTestTokens.mint(oldSecret, 5L)}")

        when:
        boolean allowed = interceptor.before()

        then:
        !allowed
        response.status == 401
        ScimTenantContext.currentTenantId() == null
    }

    void "a tenant cannot authenticate with a token signed by another tenant's secret"() {
        given: 'tenant 6 forges a token claiming tenant 5 using its own secret'
        multiTenant()
        String tenant6Secret = ScimTestTokens.newSecret()
        provider.getTenantConfig(5L) >> new ScimTenantConfig(tenantId: 5L, enabled: true, jwtSecret: tenantSecret)
        request.addHeader('Authorization', "Bearer ${ScimTestTokens.mint(tenant6Secret, 5L)}")

        when:
        boolean allowed = interceptor.before()

        then:
        !allowed
        response.status == 401
    }

    void "malformed JWT returns 401 without calling the provider"() {
        given:
        multiTenant()
        request.addHeader('Authorization', 'Bearer a.b.c')

        when:
        boolean allowed = interceptor.before()

        then:
        !allowed
        response.status == 401
        0 * provider.getTenantConfig(_)
    }

    // ------------------------
    // LEGACY PATH
    // ------------------------
    void "legacy token maps to the default tenant when the legacy flag is not set"() {
        given: 'grails.scim.legacyToken.enabled is not configured, so it defaults to on'
        multiTenant()
        provider.getDefaultTenantConfig() >> new ScimTenantConfig(tenantId: 1L, enabled: true, legacyApiToken: 'legacy-token')
        request.addHeader('Authorization', 'Bearer legacy-token')

        when:
        boolean allowed = interceptor.before()

        then:
        allowed
        ScimTenantContext.currentTenantId() == 1L
        ScimTenantContext.isLegacyToken()
    }

    void "wrong or unsupported legacy token returns 401"() {
        given:
        multiTenant()
        provider.getDefaultTenantConfig() >> defaultConfig
        request.addHeader('Authorization', 'Bearer legacy-token')

        when:
        boolean allowed = interceptor.before()

        then:
        !allowed
        response.status == 401
        ScimTenantContext.currentTenantId() == null

        where:
        defaultConfig << [
                new ScimTenantConfig(tenantId: 1L, enabled: true, legacyApiToken: 'another-token'),
                new ScimTenantConfig(tenantId: 1L, enabled: true, legacyApiToken: null),
                null
        ]
    }

    void "legacy token for a disabled default tenant returns 403"() {
        given:
        multiTenant()
        provider.getDefaultTenantConfig() >> new ScimTenantConfig(tenantId: 1L, enabled: false, legacyApiToken: 'legacy-token')
        request.addHeader('Authorization', 'Bearer legacy-token')

        when:
        boolean allowed = interceptor.before()

        then:
        !allowed
        response.status == 403
    }

    void "legacy token is rejected without calling the provider when the legacy flag is off"() {
        given:
        multiTenant()
        mockConfig.getProperty(ScimControllerInterceptor.LEGACY_TOKEN_ENABLED_KEY, _) >> false
        request.addHeader('Authorization', 'Bearer legacy-token')

        when:
        boolean allowed = interceptor.before()

        then:
        !allowed
        response.status == 401
        scimError().detail == 'UNAUTHORIZED access on scim endpoint'
        ScimTenantContext.currentTenantId() == null
        0 * provider.getDefaultTenantConfig()
    }

    void "tenant JWT still works when the legacy flag is off"() {
        given:
        multiTenant()
        mockConfig.getProperty(ScimControllerInterceptor.LEGACY_TOKEN_ENABLED_KEY, _) >> false
        provider.getTenantConfig(5L) >> new ScimTenantConfig(tenantId: 5L, enabled: true, jwtSecret: tenantSecret)
        request.addHeader('Authorization', "Bearer ${ScimTestTokens.mint(tenantSecret, 5L)}")

        when:
        boolean allowed = interceptor.before()

        then:
        allowed
        ScimTenantContext.currentTenantId() == 5L
    }

    void "legacy token is accepted when the legacy flag is explicitly on"() {
        given:
        multiTenant()
        mockConfig.getProperty(ScimControllerInterceptor.LEGACY_TOKEN_ENABLED_KEY, _) >> true
        provider.getDefaultTenantConfig() >> new ScimTenantConfig(tenantId: 1L, enabled: true, legacyApiToken: 'legacy-token')
        request.addHeader('Authorization', 'Bearer legacy-token')

        when:
        boolean allowed = interceptor.before()

        then:
        allowed
        ScimTenantContext.currentTenantId() == 1L
        ScimTenantContext.isLegacyToken()
    }

    void "static-token mode ignores the legacy flag"() {
        given: 'no provider bean, so the legacy flag does not apply'
        scimEnabled(true, 'secret')
        mockConfig.getProperty(ScimControllerInterceptor.LEGACY_TOKEN_ENABLED_KEY, _) >> false
        request.addHeader('Authorization', 'Bearer secret')

        when:
        boolean allowed = interceptor.before()

        then:
        allowed
    }

    void "missing or non-bearer authorization returns 401 without calling the provider"() {
        given:
        multiTenant()
        if (header != null) {
            request.addHeader('Authorization', header)
        }

        when:
        boolean allowed = interceptor.before()

        then:
        !allowed
        response.status == 401
        scimError().detail == 'UNAUTHORIZED access on scim endpoint'
        0 * provider._

        where:
        header << [null, 'Bearer ', 'Basic legacy-token', 'legacy-token']
    }

    void "missing token returns 401 even when the provider would fail"() {
        given:
        multiTenant()
        provider._ >> { throw new IllegalStateException('CMT unavailable') }

        when:
        boolean allowed = interceptor.before()

        then:
        !allowed
        response.status == 401
    }

    // ------------------------
    // FAILURES / LOGGING
    // ------------------------
    void "provider failure fails closed with 500"() {
        given:
        multiTenant()
        provider.getTenantConfig(5L) >> { throw new IllegalStateException('CMT unavailable') }
        request.addHeader('Authorization', "Bearer ${ScimTestTokens.mint(tenantSecret, 5L)}")

        when:
        boolean allowed = interceptor.before()

        then:
        !allowed
        response.status == 500
        scimError().status == '500'
        ScimTenantContext.currentTenantId() == null
    }

    void "the bearer token is never written to the log"() {
        given:
        Logger logger = (Logger) LoggerFactory.getLogger(ScimControllerInterceptor)
        Level previousLevel = logger.level
        ListAppender<ILoggingEvent> appender = new ListAppender<>()
        appender.start()
        logger.addAppender(appender)
        logger.level = Level.TRACE

        and:
        multiTenant()
        String rejectedToken = ScimTestTokens.mint(ScimTestTokens.newSecret(), 5L)
        provider.getTenantConfig(5L) >> new ScimTenantConfig(tenantId: 5L, enabled: true, jwtSecret: tenantSecret)
        request.addHeader('Authorization', "Bearer ${rejectedToken}")

        when:
        boolean allowed = interceptor.before()

        then:
        !allowed
        !appender.list.isEmpty()
        appender.list.every { ILoggingEvent event -> !event.formattedMessage.contains(rejectedToken) }
        appender.list.every { ILoggingEvent event -> !event.formattedMessage.contains(tenantSecret) }

        cleanup:
        logger.detachAppender(appender)
        logger.level = previousLevel
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
