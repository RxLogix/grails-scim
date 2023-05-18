package grails.plugins.scim

import grails.converters.JSON
import grails.plugins.scim.exceptions.InvalidRequestDataException
import grails.plugins.scim.exceptions.ResourceConflictException
import grails.plugins.scim.exceptions.ResourceNotFoundException
import grails.plugins.scim.exceptions.UnsupportedActionException
import grails.plugins.scim.messages.ErrorResponse
import grails.plugins.scim.messages.ListResponse
import grails.plugins.scim.resources.operations.PatchRequest
import grails.plugins.scim.resources.ScimUser
import groovy.util.logging.Slf4j


@Slf4j
class ScimUserController {

    def scimUserService

    def index(String filter, Integer count, Integer startIndex, String excludedAttributes) {
        log.trace("User search via SCIM for filter: ${filter}, ${excludedAttributes}")
        ListResponse listResponse = scimUserService.list(filter, count, startIndex, excludedAttributes)
        render(contentType: "application/scim+json", listResponse as JSON)
    }

    def save(ScimUser scimUser) {
        log.trace("Save request for User via SCIM : ${scimUser?.properties}")
        def result
        try {
            result = scimUserService.save(scimUser)
            response.status = 201
        } catch (InvalidRequestDataException irde) {
            result = new ErrorResponse(detail: irde.message, status: '400')
            response.status = 400
        } catch (ResourceConflictException re) {
            log.error(re.message)
            result = new ErrorResponse(detail: re.message, status: '409')
            response.status = 409
        } catch (Exception ex) {
            log.error("Unknown exception due to for username save ${scimUser.userName}", ex)
            result = new ErrorResponse(detail: ex.message, status: '500')
            response.status = 500
        }
        render(contentType: "application/scim+json", result as JSON)
    }

    def update(ScimUser scimUser) {
        log.trace("Update request for User via SCIM ${scimUser?.properties}")
        def result
        try {
            result = scimUserService.update(scimUser)
            response.status = 200
        } catch (InvalidRequestDataException irde) {
            result = new ErrorResponse(detail: irde.message, status: '400')
            response.status = 400
        }  catch (ResourceNotFoundException rnfe) {
            log.error(rnfe.message)
            result = new ErrorResponse(detail: rnfe.message, status: '409')
            response.status = 409
        } catch (Exception ex) {
            log.error("Unknown exception due to for username update ${scimUser.userName}", ex)
            result = new ErrorResponse(detail: ex.message, status: '500')
            response.status = 500
        }
        render(contentType: "application/scim+json", result as JSON)
    }

    def patch(String id, PatchRequest patchRequest) {
        log.trace("Patch request for User : ${id} via SCIM ${patchRequest?.properties}")
        patchRequest.id = id
        def result
        try {
            result = scimUserService.patch(patchRequest)
            response.status = 204
        } catch (InvalidRequestDataException irde) {
            result = new ErrorResponse(detail: irde.message, status: '400')
            response.status = 400
        } catch (ResourceNotFoundException rnfe) {
            log.error(rnfe.message)
            result = new ErrorResponse(detail: rnfe.message, status: '409')
            response.status = 409
        } catch (Exception ex) {
            log.error("Unknown exception due to for username patch ${patchRequest.id}", ex)
            result = new ErrorResponse(detail: ex.message, status: '500')
            response.status = 500
        }
        render(contentType: "application/scim+json", result as JSON)
    }

    def delete(String id) {
        log.trace("Delete request for User : ${id} via SCIM")
        try {
            scimUserService.delete(id)
            response.status = 204
        } catch (ResourceNotFoundException rnfe) {
            log.error(rnfe.message)
            def result = new ErrorResponse(detail: rnfe.message, status: '404')
            response.status = 404
            render(contentType: "application/scim+json", result as JSON)
        } catch (UnsupportedActionException uae) {
            log.error(uae.message)
            def result = new ErrorResponse(detail: uae.message, status: '501')
            response.status = 501
            render(contentType: "application/scim+json", result as JSON)
        } catch (Exception ex) {
            log.error("Unknown exception due to for user delete ${id}", ex)
            def result = new ErrorResponse(detail: ex.message, status: '500')
            response.status = 500
            render(contentType: "application/scim+json", result as JSON)
        }
    }

    def show(String id, String excludedAttributes) {
        log.trace("Show request for User : ${id} via SCIM : ${excludedAttributes}")
        def result
        try {
            result = scimUserService.getUser(id, excludedAttributes)
            response.status = 200
        } catch (ResourceNotFoundException rnfe) {
            log.error(rnfe.message)
            result = new ErrorResponse(detail: rnfe.message, status: '404')
            response.status = 404
        }
        render(contentType: "application/scim+json", result as JSON)
    }


}
