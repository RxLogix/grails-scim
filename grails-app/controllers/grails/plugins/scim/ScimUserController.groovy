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
import org.springframework.http.HttpStatus

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
        int status = HttpStatus.CREATED.value()
        try {
            result = scimUserService.save(scimUser)
        } catch (InvalidRequestDataException irde) {
            result = new ErrorResponse(detail: irde.message, status: HttpStatus.BAD_REQUEST.value() as String)
            status = HttpStatus.BAD_REQUEST.value()
        } catch (ResourceConflictException re) {
            log.error(re.message)
            result = new ErrorResponse(detail: re.message, status: HttpStatus.CONFLICT.value() as String)
            status = HttpStatus.CONFLICT.value()
        } catch (Exception ex) {
            log.error("Unknown exception due to for username save ${scimUser.userName}", ex)
            result = new ErrorResponse(detail: ex.message, status: HttpStatus.INTERNAL_SERVER_ERROR.value() as String)
            status = HttpStatus.INTERNAL_SERVER_ERROR.value()
        }
        renderScim(result, status)
    }

    def update(String id) {
        ScimUser scimUser = fromJson(request.JSON as Map)
        scimUser.id = id
        log.trace("Update request for User via SCIM ${scimUser?.properties}")
        def result
        int status = HttpStatus.OK.value()
        try {
            result = scimUserService.update(scimUser)
        } catch (InvalidRequestDataException irde) {
            result = new ErrorResponse(detail: irde.message, status: HttpStatus.BAD_REQUEST.value() as String)
            status = HttpStatus.BAD_REQUEST.value()
        } catch (ResourceNotFoundException rnfe) {
            log.error(rnfe.message)
            result = new ErrorResponse(detail: rnfe.message, status: HttpStatus.CONFLICT.value() as String)
            status = HttpStatus.CONFLICT.value()
        } catch (Exception ex) {
            log.error("Unknown exception due to for username update ${scimUser.userName}", ex)
            result = new ErrorResponse(detail: ex.message, status: HttpStatus.INTERNAL_SERVER_ERROR.value() as String)
            status = HttpStatus.INTERNAL_SERVER_ERROR.value()
        }
        renderScim(result, status)
    }

    def patch(String id) {
        PatchRequest patchRequest = new PatchRequest()
        patchRequest.id = id
        bindData(patchRequest, request.JSON as Map)
        log.trace("Patch request for User : ${id} via SCIM ${patchRequest?.properties}")
        def result
        int status = HttpStatus.NO_CONTENT.value()
        try {
            result = scimUserService.patch(patchRequest)
        } catch (InvalidRequestDataException irde) {
            result = new ErrorResponse(detail: irde.message, status: HttpStatus.BAD_REQUEST.value() as String)
            status = HttpStatus.BAD_REQUEST.value()
        } catch (ResourceNotFoundException rnfe) {
            log.error(rnfe.message)
            result = new ErrorResponse(detail: rnfe.message, status: HttpStatus.CONFLICT.value() as String)
            status = HttpStatus.CONFLICT.value()
        } catch (Exception ex) {
            log.error("Unknown exception due to for username patch ${patchRequest.id}", ex)
            result = new ErrorResponse(detail: ex.message, status: HttpStatus.INTERNAL_SERVER_ERROR.value() as String)
            status = HttpStatus.INTERNAL_SERVER_ERROR.value()
        }
        renderScim(result, status)
    }

    def delete(String id) {
        log.trace("Delete request for User : ${id} via SCIM")
        try {
            scimUserService.delete(id)
            response.status = HttpStatus.NO_CONTENT.value()
        } catch (ResourceNotFoundException rnfe) {
            log.error(rnfe.message)
            def result = new ErrorResponse(detail: rnfe.message, status: HttpStatus.NOT_FOUND.value() as String)
            renderScim(result, HttpStatus.NOT_FOUND.value())
        } catch (UnsupportedActionException uae) {
            log.error(uae.message)
            def result = new ErrorResponse(detail: uae.message, status: HttpStatus.NOT_IMPLEMENTED.value() as String)
            renderScim(result, HttpStatus.NOT_IMPLEMENTED.value())
        } catch (Exception ex) {
            log.error("Unknown exception due to for user delete ${id}", ex)
            def result = new ErrorResponse(detail: ex.message, status: HttpStatus.INTERNAL_SERVER_ERROR.value() as String)
            renderScim(result, HttpStatus.INTERNAL_SERVER_ERROR.value())
        }
    }

    def show(String id, String excludedAttributes, String attributes) {
        log.trace("Show request for User : ${id} via SCIM : ${excludedAttributes} and attributes : ${attributes}")
        def result
        int status = HttpStatus.OK.value()
        try {
            result = scimUserService.getUser(id, excludedAttributes, attributes)
        } catch (ResourceNotFoundException rnfe) {
            log.error(rnfe.message)
            result = new ErrorResponse(detail: rnfe.message, status: HttpStatus.NOT_FOUND.value() as String)
            status = HttpStatus.NOT_FOUND.value()
        }
        renderScim(result, status)
    }

    private void renderScim(def body, int status = HttpStatus.OK.value()) {
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
