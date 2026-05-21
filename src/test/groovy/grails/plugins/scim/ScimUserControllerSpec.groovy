package grails.plugins.scim

import grails.plugins.scim.exceptions.*
import grails.plugins.scim.messages.ListResponse
import grails.plugins.scim.resources.ScimUser
import grails.testing.web.controllers.ControllerUnitTest
import groovy.json.JsonSlurper
import spock.lang.Specification

class ScimUserControllerSpec extends Specification implements ControllerUnitTest<ScimUserController> {

    def scimUserService = Mock(ScimUserService)

    void setup() {
        controller.scimUserService = scimUserService
    }

    // ------------------------
    // INDEX
    // ------------------------
    void "index should return list response"() {
        given:
        def responseObj = new ListResponse(totalResults: 1)
        scimUserService.list(_, _, _, _, _) >> responseObj

        when:
        controller.index("userName eq 'john'", 10, 1, null, null)

        then:
        response.status == 200
        response.contentType == "application/scim+json;charset=utf-8"
    }

    // ------------------------
    // SAVE
    // ------------------------
    void "save should return 201 on success"() {
        given:
        request.contentType = 'application/json'
        request.json = new JsonSlurper().parseText('{"userName": "john"}')
        def user = new ScimUser(userName: "john")
        scimUserService.save(_) >> user

        when:
        controller.save()

        then:
        response.status == 201
    }

    void "save should return 400 on InvalidRequestDataException"() {
        given:
        request.contentType = 'application/json'
        request.json = new JsonSlurper().parseText('{"userName": "john"}')
        scimUserService.save(_) >> { throw new InvalidRequestDataException("bad data") }

        when:
        controller.save()

        then:
        response.status == 400
    }

    void "save should return 409 on ResourceConflictException"() {
        given:
        request.contentType = 'application/json'
        request.json = new JsonSlurper().parseText('{"userName": "john"}')
        scimUserService.save(_) >> { throw new ResourceConflictException("conflict") }

        when:
        controller.save()

        then:
        response.status == 409
    }

    void "save should return 500 on unknown exception"() {
        given:
        request.contentType = 'application/json'
        request.json = new JsonSlurper().parseText('{"userName": "john"}')
        scimUserService.save(_) >> { throw new RuntimeException("boom") }

        when:
        controller.save()

        then:
        response.status == 500
    }

    // ------------------------
    // UPDATE
    // ------------------------
    void "update should return 200 on success"() {
        given:
        request.contentType = 'application/json'
        request.json = new JsonSlurper().parseText('{"userName": "john", "id": "123"}')
        def user = new ScimUser(userName: "john")
        scimUserService.update(_) >> user

        when:
        controller.update("123")

        then:
        response.status == 200
    }

    void "update should return 400 when payload id does not match uri id"() {
        given:
        def jsonPayload = '''
            {
                "id": "999",
                "userName": "john"
            }
            '''

        request.contentType = 'application/json'
        request.json = new JsonSlurper().parseText(jsonPayload)

        when:
        controller.update("123")

        then:
        response.status == 400
        0 * scimUserService.update(_)
    }

    void "update should return 400 on invalid request"() {
        given:
        request.contentType = 'application/json'
        request.json = new JsonSlurper().parseText('{"userName": "john"}')
        scimUserService.update(_) >> { throw new InvalidRequestDataException("bad") }

        when:
        controller.update("123")

        then:
        response.status == 400
    }

    void "update should return 404 when user not found"() {
        given:
        request.contentType = 'application/json'
        request.json = new JsonSlurper().parseText('{"userName": "john", "id":"123"}')
        scimUserService.update(_) >> { throw new ResourceNotFoundException("missing") }

        when:
        controller.update("123")

        then:
        response.status == 404
    }

    void "update should return 500 on unknown exception"() {
        given:
        request.contentType = 'application/json'
        request.json = new JsonSlurper().parseText('{"userName": "john", "id": "123"}')
        scimUserService.update(_) >> { throw new RuntimeException("boom") }

        when:
        controller.update("123")

        then:
        response.status == 500
    }

    // ------------------------
    // PATCH
    // ------------------------
    void "patch should return 204 on success"() {
        given:
        request.contentType = 'application/json'
        request.json = [:]
        scimUserService.patch(_) >> [:]

        when:
        controller.patch("123")

        then:
        response.status == 204
    }

    void "patch should return 400 on invalid request"() {
        given:
        request.contentType = 'application/json'
        request.json = [:]
        scimUserService.patch(_) >> { throw new InvalidRequestDataException("bad") }

        when:
        controller.patch("123")

        then:
        response.status == 400
    }

    void "patch should return 404 when user not found"() {
        given:
        request.contentType = 'application/json'
        request.json = [:]
        scimUserService.patch(_) >> { throw new ResourceNotFoundException("missing") }

        when:
        controller.patch("123")

        then:
        response.status == 404
    }

    void "patch should return 500 on unknown exception"() {
        given:
        request.contentType = 'application/json'
        request.json = [:]
        scimUserService.patch(_) >> { throw new RuntimeException("boom") }

        when:
        controller.patch("123")

        then:
        response.status == 500
    }

    // ------------------------
    // DELETE
    // ------------------------
    void "delete should return 204 on success"() {
        given:
        scimUserService.delete("123") >> null

        when:
        controller.delete("123")

        then:
        response.status == 204
    }

    void "delete should return 404 when user not found"() {
        given:
        scimUserService.delete("123") >> { throw new ResourceNotFoundException("missing") }

        when:
        controller.delete("123")

        then:
        response.status == 404
    }

    void "delete should return 501 when unsupported"() {
        given:
        scimUserService.delete("123") >> { throw new UnsupportedActionException("unsupported") }

        when:
        controller.delete("123")

        then:
        response.status == 501
    }

    void "delete should return 500 on unknown exception"() {
        given:
        scimUserService.delete("123") >> { throw new RuntimeException("boom") }

        when:
        controller.delete("123")

        then:
        response.status == 500
    }

    // ------------------------
    // SHOW
    // ------------------------
    void "show should return 200 on success"() {
        given:
        scimUserService.getUser(_, _, _) >> new ScimUser(userName: "john")

        when:
        controller.show("123", null, null)

        then:
        response.status == 200
    }

    void "show should return 404 when user missing"() {
        given:
        scimUserService.getUser(_, _, _) >> { throw new ResourceNotFoundException("missing") }

        when:
        controller.show("123", null, null)

        then:
        response.status == 404
    }

}