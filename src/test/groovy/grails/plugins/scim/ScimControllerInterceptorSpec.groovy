package grails.plugins.scim

import grails.config.Config
import grails.core.GrailsApplication
import grails.testing.web.interceptor.InterceptorUnitTest
import groovy.json.JsonSlurper
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
        // No space -> split yields a single element, so the size > 1 guard fails
        request.addHeader('Authorization', 'secret')

        when:
        boolean allowed = interceptor.before()

        then:
        !allowed
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