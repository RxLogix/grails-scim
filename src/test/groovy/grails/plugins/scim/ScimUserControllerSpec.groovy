package grails.plugins.scim

import grails.plugins.scim.exceptions.*
import grails.plugins.scim.messages.ListResponse
import grails.plugins.scim.messages.ErrorResponse
import grails.plugins.scim.resources.ScimUser
import grails.plugins.scim.resources.operations.PatchRequest
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

        when:
        scimUserService.list(_, _, _, _, _) >> responseObj
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
        def jsonPayload = '''
    {
        "userName": "john"
    }
    '''

        request.contentType = 'application/json'
        request.json = new JsonSlurper().parseText(jsonPayload)
        def user = new ScimUser(userName: "john")

        when:
        scimUserService.save(_) >> user
        controller.save()

        then:
        response.status == 201
    }

    void "save should return 400 on InvalidRequestDataException"() {
        given:
        def jsonPayload = '''
    {
        "userName": "john"
    }
    '''

        request.contentType = 'application/json'
        request.json = new JsonSlurper().parseText(jsonPayload)

        when:
        scimUserService.save(_) >> { throw new InvalidRequestDataException("bad data") }
        controller.save()

        then:
        response.status == 400
    }

    void "save should return 409 on ResourceConflictException"() {
        given:
        def jsonPayload = '''
    {
        "userName": "john"
    }
    '''

        request.contentType = 'application/json'
        request.json = new JsonSlurper().parseText(jsonPayload)

        when:
        scimUserService.save(_) >> { throw new ResourceConflictException("conflict") }
        controller.save()

        then:
        response.status == 409
    }

    void "save should return 500 on unknown exception"() {
        given:
        def jsonPayload = '''
    {
        "userName": "john"
    }
    '''

        request.contentType = 'application/json'
        request.json = new JsonSlurper().parseText(jsonPayload)

        when:
        scimUserService.save(_) >> { throw new RuntimeException("boom") }
        controller.save()

        then:
        response.status == 500
    }

    // ------------------------
    // UPDATE
    // ------------------------
    void "update should return 200 on success"() {
        given:
        def jsonPayload = '''
    {
        "userName": "john"
    }
    '''

        request.contentType = 'application/json'
        request.json = new JsonSlurper().parseText(jsonPayload)
        def user = new ScimUser(userName: "john")

        when:
        scimUserService.update(_) >> user
        controller.update()

        then:
        response.status == 200
    }

    void "update should return 400 on invalid request"() {
        given:
        def jsonPayload = '''
    {
        "userName": "john"
    }
    '''

        request.contentType = 'application/json'
        request.json = new JsonSlurper().parseText(jsonPayload)

        when:
        scimUserService.update(_) >> { throw new InvalidRequestDataException("bad") }
        controller.update()

        then:
        response.status == 400
    }

    void "update should return 409 when user not found"() {
        given:
        def jsonPayload = '''
    {
        "userName": "john"
    }
    '''

        request.contentType = 'application/json'
        request.json = new JsonSlurper().parseText(jsonPayload)

        when:
        scimUserService.update(_) >> { throw new ResourceNotFoundException("missing") }
        controller.update()

        then:
        response.status == 409
    }

    void "update should return 500 on unknown exception"() {
        given:
        def jsonPayload = '''
    {
        "userName": "john"
    }
    '''

        request.contentType = 'application/json'
        request.json = new JsonSlurper().parseText(jsonPayload)

        when:
        scimUserService.update(_) >> { throw new RuntimeException("boom") }
        controller.update()

        then:
        response.status == 500
    }

    // ------------------------
    // PATCH
    // ------------------------
    void "patch should return 204 on success"() {
        given:
        def patch = new PatchRequest()

        when:
        scimUserService.patch(_) >> [:]
        controller.patch("123", patch)

        then:
        response.status == 204
        patch.id == "123"
    }

    void "patch should return 400 on invalid request"() {
        given:
        def patch = new PatchRequest()

        when:
        scimUserService.patch(_) >> { throw new InvalidRequestDataException("bad") }
        controller.patch("123", patch)

        then:
        response.status == 400
    }

    void "patch should return 409 when user not found"() {
        given:
        def patch = new PatchRequest()

        when:
        scimUserService.patch(_) >> { throw new ResourceNotFoundException("missing") }
        controller.patch("123", patch)

        then:
        response.status == 409
    }

    void "patch should return 500 on unknown exception"() {
        given:
        def patch = new PatchRequest()

        when:
        scimUserService.patch(_) >> { throw new RuntimeException("boom") }
        controller.patch("123", patch)

        then:
        response.status == 500
    }

    // ------------------------
    // DELETE
    // ------------------------
    void "delete should return 204 on success"() {
        when:
        scimUserService.delete("123") >> null
        controller.delete("123")

        then:
        response.status == 204
    }

    void "delete should return 404 when user not found"() {
        when:
        scimUserService.delete("123") >> { throw new ResourceNotFoundException("missing") }
        controller.delete("123")

        then:
        response.status == 404
    }

    void "delete should return 501 when unsupported"() {
        when:
        scimUserService.delete("123") >> { throw new UnsupportedActionException("unsupported") }
        controller.delete("123")

        then:
        response.status == 501
    }

    void "delete should return 500 on unknown exception"() {
        when:
        scimUserService.delete("123") >> { throw new RuntimeException("boom") }
        controller.delete("123")

        then:
        response.status == 500
    }

    // ------------------------
    // SHOW
    // ------------------------
    void "show should return 200 on success"() {
        when:
        scimUserService.getUser(_, _, _) >> new ScimUser(userName: "john")
        controller.show("123", null, null)

        then:
        response.status == 200
    }

    void "show should return 404 when user missing"() {
        when:
        scimUserService.getUser(_, _, _) >> { throw new ResourceNotFoundException("missing") }
        controller.show("123", null, null)

        then:
        response.status == 404
    }

}
