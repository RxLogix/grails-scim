package grails.plugins.scim

import grails.converters.JSON
import grails.plugins.scim.exceptions.InvalidRequestDataException
import grails.plugins.scim.exceptions.ResourceConflictException
import grails.plugins.scim.exceptions.ResourceNotFoundException
import grails.plugins.scim.messages.ErrorResponse
import grails.plugins.scim.messages.ListResponse
import grails.plugins.scim.resources.operations.PatchRequest
import grails.plugins.scim.resources.ScimGroup
import groovy.util.logging.Slf4j


@Slf4j
class ScimGroupController {

    def scimGroupService

    def index(String filter, Integer count, Integer startIndex, String excludedAttributes) {
        log.trace("Group search via SCIM for filter: ${filter}, ${excludedAttributes}")
        ListResponse listResponse = scimGroupService.list(filter, count, startIndex, excludedAttributes)
        render(contentType: "application/scim+json", listResponse as JSON)
    }

    def save(ScimGroup scimGroup) {
        log.trace("Save request for Group via SCIM: ${scimGroup?.properties}")
        def result
        try {
            result = scimGroupService.save(scimGroup)
            response.status = 201
        } catch (InvalidRequestDataException irde) {
            result = new ErrorResponse(detail: irde.message, status: '400')
            response.status = 400
        } catch (ResourceConflictException re) {
            log.error(re.message)
            result = new ErrorResponse(detail: re.message, status: '409')
            response.status = 409
        } catch (Exception ex) {
            log.error("Unknown exception due to for group name save ${scimGroup.displayName}", ex)
            result = new ErrorResponse(detail: ex.message, status: '500')
            response.status = 500
        }
        render(contentType: "application/scim+json", result as JSON)
    }

    def update(ScimGroup scimGroup) {
        log.trace("Update request for Group : ${scimGroup?.id} via SCIM: ${scimGroup?.properties}")
        def result
        try {
            result = scimGroupService.update(scimGroup)
            response.status = 200
        } catch (InvalidRequestDataException irde) {
            result = new ErrorResponse(detail: irde.message, status: '400')
            response.status = 400
        } catch (ResourceNotFoundException rnfe) {
            log.error(rnfe.message)
            result = new ErrorResponse(detail: rnfe.message, status: '409')
            response.status = 409
        } catch (Exception ex) {
            log.error("Unknown exception due to for group name update ${scimGroup.displayName}", ex)
            result = new ErrorResponse(detail: ex.message, status: '500')
            response.status = 500
        }
        render(contentType: "application/scim+json", result as JSON)
    }

    def patch(String id, PatchRequest patchRequest) {
        log.trace("Patch request for Group : ${id} via SCIM: ${patchRequest?.properties}")
        patchRequest.id = id
        def result
        try {
            result = scimGroupService.patch(patchRequest)
            response.status = 204
        } catch (InvalidRequestDataException irde) {
            result = new ErrorResponse(detail: irde.message, status: '400')
            response.status = 400
        } catch (ResourceNotFoundException rnfe) {
            log.error(rnfe.message)
            result = new ErrorResponse(detail: rnfe.message, status: '409')
            response.status = 409
        } catch (Exception ex) {
            log.error("Unknown exception due to for group patch update ${patchRequest.id}", ex)
            result = new ErrorResponse(detail: ex.message, status: '500')
            response.status = 500
        }
        render(contentType: "application/scim+json", result as JSON)
    }


    def delete(String id) {
        log.trace("Delete request for Group : ${id} via SCIM")
        try {
            scimGroupService.delete(id)
            response.status = 204
        } catch (ResourceNotFoundException rnfe) {
            log.error(rnfe.message)
            def result = new ErrorResponse(detail: rnfe.message, status: '404')
            response.status = 404
            render(contentType: "application/scim+json", result as JSON)
        } catch (Exception ex) {
            log.error("Unknown exception due to for group delete ${id}", ex)
            def result = new ErrorResponse(detail: ex.message, status: '500')
            response.status = 500
            render(contentType: "application/scim+json", result as JSON)
        }

    }

    def show(String id, String excludedAttributes) {
        log.trace("Show request for Group : ${id} via SCIM : ${excludedAttributes}")
        def result
        try {
            result = scimGroupService.getGroup(id, excludedAttributes)
            response.status = 200
        } catch (ResourceNotFoundException rnfe) {
            log.error(rnfe.message)
            result = new ErrorResponse(detail: rnfe.message, status: '404')
            response.status = 404
        }
        render(contentType: "application/scim+json", result as JSON)
    }


}
