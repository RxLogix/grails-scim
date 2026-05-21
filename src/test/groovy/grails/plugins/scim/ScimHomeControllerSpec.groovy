package grails.plugins.scim

import grails.testing.web.controllers.ControllerUnitTest
import spock.lang.Specification

class ScimHomeControllerSpec extends Specification implements ControllerUnitTest<ScimHomeController> {

    // ------------------------
    // INDEX
    // ------------------------
    void "index should render empty array"() {
        when:
        controller.index()

        then:
        response.status == 200
        response.text == '[]'
    }

    // ------------------------
    // SCHEMAS
    // ------------------------
    void "schemas with no id should return all schemas"() {
        when:
        controller.schemas(null)

        then:
        response.status == 200
    }

    void "schemas with valid id should return matching schema"() {
        when:
        controller.schemas('urn:ietf:params:scim:schemas:core:2.0:User')

        then:
        response.status == 200
    }

    void "schemas with invalid id should return 404"() {
        when:
        controller.schemas('unknown-id')

        then:
        response.status == 404
    }

    // ------------------------
    // SERVICE PROVIDER CONFIG
    // ------------------------
    void "serviceProviderConfig should return 200"() {
        when:
        controller.serviceProviderConfig()

        then:
        response.status == 200
    }

    // ------------------------
    // RESOURCE TYPES
    // ------------------------
    void "resourceTypes should return 200"() {
        when:
        controller.resourceTypes()

        then:
        response.status == 200
    }

    // ------------------------
    // SCIM AUTH DOCS
    // ------------------------
    void "scimAuthDocs should return JSON when Accept header is application/json"() {
        given:
        request.addHeader('Accept', 'application/json')

        when:
        controller.scimAuthDocs()

        then:
        response.status == 200
        response.contentType.contains('application/json')
    }

    void "scimAuthDocs should return HTML when Accept header is not application/json"() {
        when:
        controller.scimAuthDocs()

        then:
        response.status == 200
        response.contentType.contains('text/html')
    }

}