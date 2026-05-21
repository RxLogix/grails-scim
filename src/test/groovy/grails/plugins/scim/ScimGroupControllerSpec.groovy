package grails.plugins.scim

import grails.plugins.scim.exceptions.*
import grails.plugins.scim.messages.ListResponse
import grails.plugins.scim.resources.ScimGroup
import grails.testing.web.controllers.ControllerUnitTest
import groovy.json.JsonSlurper
import spock.lang.Specification

class ScimGroupControllerSpec extends Specification implements ControllerUnitTest<ScimGroupController> {

    def scimGroupService = Mock(ScimGroupService)

    void setup() {
        controller.scimGroupService = scimGroupService
    }

    // ------------------------
    // INDEX
    // ------------------------
    void "index should return list response"() {
        given:
        def responseObj = new ListResponse(totalResults: 1)
        scimGroupService.list(_, _, _, _, _) >> responseObj

        when:
        controller.index("displayName eq 'admins'", 10, 1, null, null)

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
        request.json = new JsonSlurper().parseText('{"displayName": "admins"}')
        def group = new ScimGroup(displayName: "admins")
        scimGroupService.save(_) >> group

        when:
        controller.save()

        then:
        response.status == 201
    }

    void "save should return 400 on InvalidRequestDataException"() {
        given:
        request.contentType = 'application/json'
        request.json = new JsonSlurper().parseText('{"displayName": "admins"}')
        scimGroupService.save(_) >> { throw new InvalidRequestDataException("bad data") }

        when:
        controller.save()

        then:
        response.status == 400
    }

    void "save should return 409 on ResourceConflictException"() {
        given:
        request.contentType = 'application/json'
        request.json = new JsonSlurper().parseText('{"displayName": "admins"}')
        scimGroupService.save(_) >> { throw new ResourceConflictException("conflict") }

        when:
        controller.save()

        then:
        response.status == 409
    }

    void "save should return 500 on unknown exception"() {
        given:
        request.contentType = 'application/json'
        request.json = new JsonSlurper().parseText('{"displayName": "admins"}')
        scimGroupService.save(_) >> { throw new RuntimeException("boom") }

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
        request.json = new JsonSlurper().parseText('{"displayName": "admins"}')
        def group = new ScimGroup(displayName: "admins")
        scimGroupService.update(_) >> group

        when:
        controller.update("123")

        then:
        response.status == 200
    }

    void "update should return 400 on invalid request"() {
        given:
        request.contentType = 'application/json'
        request.json = new JsonSlurper().parseText('{"displayName": "admins"}')
        scimGroupService.update(_) >> { throw new InvalidRequestDataException("bad") }

        when:
        controller.update("123")

        then:
        response.status == 400
    }

    void "update should return 409 when group not found"() {
        given:
        request.contentType = 'application/json'
        request.json = new JsonSlurper().parseText('{"displayName": "admins"}')
        scimGroupService.update(_) >> { throw new ResourceNotFoundException("missing") }

        when:
        controller.update("123")

        then:
        response.status == 409
    }

    void "update should return 500 on unknown exception"() {
        given:
        request.contentType = 'application/json'
        request.json = new JsonSlurper().parseText('{"displayName": "admins"}')
        scimGroupService.update(_) >> { throw new RuntimeException("boom") }

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
        scimGroupService.patch(_) >> [:]

        when:
        controller.patch("123")

        then:
        response.status == 204
    }

    void "patch should return 400 on invalid request"() {
        given:
        request.contentType = 'application/json'
        request.json = [:]
        scimGroupService.patch(_) >> { throw new InvalidRequestDataException("bad") }

        when:
        controller.patch("123")

        then:
        response.status == 400
    }

    void "patch should return 409 when group not found"() {
        given:
        request.contentType = 'application/json'
        request.json = [:]
        scimGroupService.patch(_) >> { throw new ResourceNotFoundException("missing") }

        when:
        controller.patch("123")

        then:
        response.status == 409
    }

    void "patch should return 500 on unknown exception"() {
        given:
        request.contentType = 'application/json'
        request.json = [:]
        scimGroupService.patch(_) >> { throw new RuntimeException("boom") }

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
        scimGroupService.delete("123") >> null

        when:
        controller.delete("123")

        then:
        response.status == 204
    }

    void "delete should return 404 when group not found"() {
        given:
        scimGroupService.delete("123") >> { throw new ResourceNotFoundException("missing") }

        when:
        controller.delete("123")

        then:
        response.status == 404
    }

    void "delete should return 500 on unknown exception"() {
        given:
        scimGroupService.delete("123") >> { throw new RuntimeException("boom") }

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
        scimGroupService.getGroup(_, _, _) >> new ScimGroup(displayName: "admins")

        when:
        controller.show("123", null, null)

        then:
        response.status == 200
    }

    void "show should return 404 when group missing"() {
        given:
        scimGroupService.getGroup(_, _, _) >> { throw new ResourceNotFoundException("missing") }

        when:
        controller.show("123", null, null)

        then:
        response.status == 404
    }

}