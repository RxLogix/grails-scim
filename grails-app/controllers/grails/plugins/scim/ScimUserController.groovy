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
        HttpStatus status = HttpStatus.CREATED
        try {
            result = scimUserService.save(scimUser)
        } catch (InvalidRequestDataException irde) {
            status = HttpStatus.BAD_REQUEST
            result = new ErrorResponse(detail: irde.message, status: status.value().toString())
        } catch (ResourceConflictException re) {
            log.error(re.message)
            status = HttpStatus.CONFLICT
            result = new ErrorResponse(detail: re.message, status: status.value().toString())
        } catch (Exception ex) {
            log.error("Unknown exception due to for username save ${scimUser.userName}", ex)
            status = HttpStatus.INTERNAL_SERVER_ERROR
            result = new ErrorResponse(detail: ex.message, status: status.value().toString())
        }
        renderScim(result, status)
    }

    def update(String id) {
        ScimUser scimUser = fromJson(request.JSON as Map)
        log.trace("Update request for User id ${id} via SCIM ${scimUser?.properties}")
        def result
        HttpStatus status = HttpStatus.OK
        try {
            if (scimUser.id != id) {
                throw new InvalidRequestDataException("There is mismatch between user json payload id and reference id passed in URI")
            }
            result = scimUserService.update(scimUser)
        } catch (InvalidRequestDataException irde) {
            status = HttpStatus.BAD_REQUEST
            result = new ErrorResponse(detail: irde.message, status: status.value().toString())
        } catch (ResourceNotFoundException rnfe) {
            log.error(rnfe.message)
            status = HttpStatus.NOT_FOUND
            result = new ErrorResponse(detail: rnfe.message, status: status.value().toString())
        } catch (Exception ex) {
            log.error("Unknown exception due to for username update ${scimUser.userName}", ex)
            status = HttpStatus.INTERNAL_SERVER_ERROR
            result = new ErrorResponse(detail: ex.message, status: status.value().toString())
        }
        renderScim(result, status)
    }

    def patch(String id) {
        PatchRequest patchRequest = new PatchRequest()
        patchRequest.id = id
        bindData(patchRequest, request.JSON as Map)
        log.trace("Patch request for User : ${id} via SCIM ${patchRequest?.properties}")
        def result
        HttpStatus status = HttpStatus.NO_CONTENT
        try {
            result = scimUserService.patch(patchRequest)
        } catch (InvalidRequestDataException irde) {
            status = HttpStatus.BAD_REQUEST
            result = new ErrorResponse(detail: irde.message, status: status.value().toString())
        } catch (ResourceNotFoundException rnfe) {
            log.error(rnfe.message)
            status = HttpStatus.NOT_FOUND
            result = new ErrorResponse(detail: rnfe.message, status: status.value().toString())
        } catch (Exception ex) {
            log.error("Unknown exception due to for username patch ${patchRequest.id}", ex)
            status = HttpStatus.INTERNAL_SERVER_ERROR
            result = new ErrorResponse(detail: ex.message, status: status.value().toString())
        }
        renderScim(result, status)
    }

    def delete(String id) {
        log.trace("Delete request for User : ${id} via SCIM")
        def result = null
        HttpStatus status = HttpStatus.NO_CONTENT
        try {
            scimUserService.delete(id)
        } catch (ResourceNotFoundException rnfe) {
            log.error(rnfe.message)
            status = HttpStatus.NOT_FOUND
            result = new ErrorResponse(detail: rnfe.message, status: status.value().toString())
        } catch (UnsupportedActionException uae) {
            log.error(uae.message)
            status = HttpStatus.NOT_IMPLEMENTED
            result = new ErrorResponse(detail: uae.message, status: status.value().toString())
        } catch (Exception ex) {
            log.error("Unknown exception due to for user delete ${id}", ex)
            status = HttpStatus.INTERNAL_SERVER_ERROR
            result = new ErrorResponse(detail: ex.message, status: status.value().toString())
        }
        renderScim(result, status)
    }

    def show(String id, String excludedAttributes, String attributes) {
        log.trace("Show request for User : ${id} via SCIM : ${excludedAttributes} and attributes : ${attributes}")
        def result
        HttpStatus status = HttpStatus.OK
        try {
            result = scimUserService.getUser(id, excludedAttributes, attributes)
        } catch (ResourceNotFoundException rnfe) {
            log.error(rnfe.message)
            status = HttpStatus.NOT_FOUND
            result = new ErrorResponse(detail: rnfe.message, status: status.value().toString())
        }
        renderScim(result, status)
    }

    private void renderScim(def body, HttpStatus status = HttpStatus.OK) {
        response.status = status.value()
        if (body != null) {
            render text: (body as JSON).toString(),
                    contentType: "application/scim+json"
        }
    }

    private ScimUser fromJson(Map json) {
        ScimUser user = new ScimUser()
        bindData(user, json)
        def ext = json[ScimUser.EXT_URN]
        if (ext instanceof Map) {
            user.customExtension = new CustomUserExtension(
                    tenants: ext.tenants.toString().split(grailsApplication.config.getProperty('grails.scim.separator', ",").toString())
            )
        }
        return user
    }

}
