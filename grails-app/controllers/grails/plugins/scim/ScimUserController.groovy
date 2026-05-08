package grails.plugins.scim

import grails.converters.JSON
import grails.plugins.scim.exceptions.InvalidRequestDataException
import grails.plugins.scim.exceptions.ResourceConflictException
import grails.plugins.scim.exceptions.ResourceNotFoundException
import grails.plugins.scim.exceptions.UnsupportedActionException
import grails.plugins.scim.messages.ErrorResponse
import grails.plugins.scim.messages.ListResponse
import grails.plugins.scim.resources.CustomUserExtension
import grails.plugins.scim.resources.operations.PatchRequest
import grails.plugins.scim.resources.ScimUser
import groovy.util.logging.Slf4j

@Slf4j
class ScimUserController {

    def scimUserService

    def index(String filter, Integer count, Integer startIndex, String excludedAttributes, String attributes) {
        log.trace("User search via SCIM for filter: ${filter}, ${excludedAttributes}")
        ListResponse listResponse = scimUserService.list(filter, count, startIndex, excludedAttributes, attributes)
        renderScim(listResponse)
    }

    def save() {
        ScimUser scimUser = fromJson(request.JSON as Map)
        log.trace("Save request for User via SCIM : ${scimUser?.properties}")
        def result
        int status = 201
        try {
            result = scimUserService.save(scimUser)
        } catch (InvalidRequestDataException irde) {
            result = new ErrorResponse(detail: irde.message, status: '400')
            status = 400
        } catch (ResourceConflictException re) {
            log.error(re.message)
            result = new ErrorResponse(detail: re.message, status: '409')
            status = 409
        } catch (Exception ex) {
            log.error("Unknown exception due to for username save ${scimUser.userName}", ex)
            result = new ErrorResponse(detail: ex.message, status: '500')
            status = 500
        }
        renderScim(result, status)
    }

    def update(String id) {
        ScimUser scimUser = fromJson(request.JSON as Map)
        scimUser.id = id
        log.error("Update request for User via SCIM ${scimUser?.properties}")
        def result
        int status = 200
        try {
            result = scimUserService.update(scimUser)
        } catch (InvalidRequestDataException irde) {
            result = new ErrorResponse(detail: irde.message, status: '400')
            status = 400
        } catch (ResourceNotFoundException rnfe) {
            log.error(rnfe.message)
            result = new ErrorResponse(detail: rnfe.message, status: '409')
            status = 409
        } catch (Exception ex) {
            log.error("Unknown exception due to for username update ${scimUser.userName}", ex)
            result = new ErrorResponse(detail: ex.message, status: '500')
            status = 500
        }
        renderScim(result, status)
    }

    def patch(String id) {
        PatchRequest patchRequest = new PatchRequest()
        patchRequest.id = id
        bindData(patchRequest, request.JSON as Map)
        log.trace("Patch request for User : ${id} via SCIM ${patchRequest?.properties}")
        def result
        int status = 204
        try {
            result = scimUserService.patch(patchRequest)
        } catch (InvalidRequestDataException irde) {
            result = new ErrorResponse(detail: irde.message, status: '400')
            status = 400
        } catch (ResourceNotFoundException rnfe) {
            log.error(rnfe.message)
            result = new ErrorResponse(detail: rnfe.message, status: '409')
            status = 409
        } catch (Exception ex) {
            log.error("Unknown exception due to for username patch ${patchRequest.id}", ex)
            result = new ErrorResponse(detail: ex.message, status: '500')
            status = 500
        }
        renderScim(result, status)
    }

    def delete(String id) {
        log.trace("Delete request for User : ${id} via SCIM")
        try {
            scimUserService.delete(id)
            response.status = 204
        } catch (ResourceNotFoundException rnfe) {
            log.error(rnfe.message)
            def result = new ErrorResponse(detail: rnfe.message, status: '404')
            renderScim(result, 404)
        } catch (UnsupportedActionException uae) {
            log.error(uae.message)
            def result = new ErrorResponse(detail: uae.message, status: '501')
            renderScim(result, 501)
        } catch (Exception ex) {
            log.error("Unknown exception due to for user delete ${id}", ex)
            def result = new ErrorResponse(detail: ex.message, status: '500')
            renderScim(result, 500)
        }
    }

    def show(String id, String excludedAttributes, String attributes) {
        log.trace("Show request for User : ${id} via SCIM : ${excludedAttributes} and attributes : ${attributes}")
        def result
        int status = 200
        try {
            result = scimUserService.getUser(id, excludedAttributes, attributes)
        } catch (ResourceNotFoundException rnfe) {
            log.error(rnfe.message)
            result = new ErrorResponse(detail: rnfe.message, status: '404')
            status = 404
        }
        renderScim(result, status)
    }

    private void renderScim(def body, int status = 200) {
        response.status = status
        render text: (body as JSON).toString(),
                contentType: "application/scim+json"
    }

    private ScimUser fromJson(Map json) {
        ScimUser user = new ScimUser()
        bindData(user, json)
        def ext = json[ScimUser.EXT_URN]
        if (ext instanceof Map) {
            user.customExtension = new CustomUserExtension(
                    tenants: ext.tenants.split(grailsApplication.config.getProperty('grails.scim.separator', ","))
            )
        }
        return user
    }

}
