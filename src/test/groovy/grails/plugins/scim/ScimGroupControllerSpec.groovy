package grails.plugins.scim

import grails.plugins.scim.exceptions.InvalidRequestDataException
import grails.plugins.scim.exceptions.ResourceConflictException
import grails.plugins.scim.exceptions.ResourceNotFoundException
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

        when:
        scimGroupService.list(_, _, _, _, _) >> responseObj
        controller.index("displayName eq 'admins'", 10, 1, null, null)

        then:
        response.status == 200
        response.contentType == "application/scim+json;charset=utf-8"
    }

    // ------------------------
    // CUSTOM EXTENSION (fromJson)
    // ------------------------
    void "save should parse tenant string from custom extension"() {
        given:
        def payload = """{
            "displayName": "Finance Team",
            "urn:ietf:params:scim:schemas:extension:custom:2.0:Group": {
                "tenant": "tenant-001"
            }
        }"""
        request.contentType = 'application/json'
        request.json = new JsonSlurper().parseText(payload)
        ScimGroup captured
        scimGroupService.save(_) >> { ScimGroup g -> captured = g; g }

        when:
        controller.save()

        then:
        response.status == 201
        captured.customExtension != null
        captured.customExtension.tenant == "tenant-001"
    }

    void "save should not set customExtension when extension block is absent"() {
        given:
        request.contentType = 'application/json'
        request.json = new JsonSlurper().parseText('{"displayName": "Finance Team"}')
        ScimGroup captured
        scimGroupService.save(_) >> { ScimGroup g -> captured = g; g }

        when:
        controller.save()

        then:
        response.status == 201
        captured.customExtension == null
    }

    // ------------------------
    // SAVE
    // ------------------------
    void "save should return 201 on success"() {
        given:
        request.contentType = 'application/json'
        request.json = new JsonSlurper().parseText('{"displayName": "admins"}')

        when:
        scimGroupService.save(_) >> new ScimGroup(displayName: "admins")
        controller.save()

        then:
        response.status == 201
    }

    void "save should return 400 on InvalidRequestDataException"() {
        given:
        request.contentType = 'application/json'
        request.json = new JsonSlurper().parseText('{"displayName": "admins"}')

        when:
        scimGroupService.save(_) >> { throw new InvalidRequestDataException("bad data") }
        controller.save()

        then:
        response.status == 400
    }

    void "save should return 409 on ResourceConflictException"() {
        given:
        request.contentType = 'application/json'
        request.json = new JsonSlurper().parseText('{"displayName": "admins"}')

        when:
        scimGroupService.save(_) >> { throw new ResourceConflictException("conflict") }
        controller.save()

        then:
        response.status == 409
    }

    void "save should return 500 on unknown exception"() {
        given:
        request.contentType = 'application/json'
        request.json = new JsonSlurper().parseText('{"displayName": "admins"}')

        when:
        scimGroupService.save(_) >> { throw new RuntimeException("boom") }
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
        request.json = new JsonSlurper().parseText('{"displayName": "admins", "id": "123"}')

        when:
        scimGroupService.update(_) >> new ScimGroup(displayName: "admins")
        controller.update("123")

        then:
        response.status == 200
    }

    void "update should return 400 on InvalidRequestDataException"() {
        given:
        request.contentType = 'application/json'
        request.json = new JsonSlurper().parseText('{"displayName": "admins","id": "123"}')

        when:
        scimGroupService.update(_) >> { throw new InvalidRequestDataException("bad") }
        controller.update("123")

        then:
        response.status == 400
    }

    void "update should return 400 when payload id does not match uri id"() {
        given:
        def jsonPayload = '''
            {
                "id": "999",
                "displayName": "admins"
            }
         '''

        request.contentType = 'application/json'
        request.json = new JsonSlurper().parseText(jsonPayload)

        when:
        controller.update("123")

        then:
        response.status == 400
        0 * scimGroupService.update(_)
    }

    void "update should return 404 when group not found"() {
        given:
        request.contentType = 'application/json'
        request.json = new JsonSlurper().parseText('{"displayName": "admins", "id": "123"}')

        when:
        scimGroupService.update(_) >> { throw new ResourceNotFoundException("missing") }
        controller.update("123")

        then:
        response.status == 404
    }

    void "update should return 500 on unknown exception"() {
        given:
        request.contentType = 'application/json'
        request.json = new JsonSlurper().parseText('{"displayName": "admins", "id": "123"}')

        when:
        scimGroupService.update(_) >> { throw new RuntimeException("boom") }
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

    void "patch should return 400 on InvalidRequestDataException"() {
        given:
        request.contentType = 'application/json'
        request.json = [:]
        scimGroupService.patch(_) >> { throw new InvalidRequestDataException("bad") }

        when:
        controller.patch("123")

        then:
        response.status == 400
    }

    void "patch should return 404 when group not found"() {
        given:
        request.contentType = 'application/json'
        request.json = [:]
        scimGroupService.patch(_) >> { throw new ResourceNotFoundException("missing") }

        when:
        controller.patch("123")

        then:
        response.status == 404
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
        when:
        scimGroupService.delete("123") >> null
        controller.delete("123")

        then:
        response.status == 204
    }

    void "delete should return 404 when group not found"() {
        when:
        scimGroupService.delete("123") >> { throw new ResourceNotFoundException("missing") }
        controller.delete("123")

        then:
        response.status == 404
    }

    void "delete should return 500 on unknown exception"() {
        when:
        scimGroupService.delete("123") >> { throw new RuntimeException("boom") }
        controller.delete("123")

        then:
        response.status == 500
    }

    // ------------------------
    // SHOW
    // ------------------------
    void "show should return 200 on success"() {
        when:
        scimGroupService.getGroup(_, _, _) >> new ScimGroup(displayName: "admins")
        controller.show("123", null, null)

        then:
        response.status == 200
    }

    void "show should return 404 when group not found"() {
        when:
        scimGroupService.getGroup(_, _, _) >> { throw new ResourceNotFoundException("missing") }
        controller.show("123", null, null)

        then:
        response.status == 404
    }

}