package grails.plugins.scim

import grails.converters.JSON
import grails.plugins.scim.exceptions.InvalidRequestDataException
import grails.plugins.scim.exceptions.ResourceConflictException
import grails.plugins.scim.exceptions.ResourceNotFoundException
import grails.plugins.scim.messages.ErrorResponse
import grails.plugins.scim.messages.ListResponse
import grails.plugins.scim.resources.CustomGroupExtension
import grails.plugins.scim.resources.operations.PatchRequest
import grails.plugins.scim.resources.ScimGroup
import groovy.util.logging.Slf4j
import org.springframework.http.HttpStatus

@Slf4j
class ScimGroupController {

    def scimGroupService

    def index(String filter, Integer count, Integer startIndex, String excludedAttributes, String attributes) {
        log.trace("Group search via SCIM for filter: ${filter}, ${excludedAttributes}")
        ListResponse listResponse = scimGroupService.list(filter, count, startIndex, excludedAttributes, attributes)
        renderScim(listResponse)
    }

    def save() {
        ScimGroup scimGroup = fromJson(request.JSON as Map)
        log.trace("Save request for Group via SCIM: ${scimGroup?.properties}")
        def result
        HttpStatus status = HttpStatus.CREATED
        try {
            result = scimGroupService.save(scimGroup)
        } catch (InvalidRequestDataException irde) {
            status = HttpStatus.BAD_REQUEST
            result = new ErrorResponse(detail: irde.message, status: status.value().toString())
        } catch (ResourceConflictException re) {
            log.error(re.message)
            status = HttpStatus.CONFLICT
            result = new ErrorResponse(detail: re.message, status: status.value().toString())
        } catch (Exception ex) {
            log.error("Unknown exception due to for group name save ${scimGroup.displayName}", ex)
            status = HttpStatus.INTERNAL_SERVER_ERROR
            result = new ErrorResponse(detail: ex.message, status: status.value().toString())
        }
        renderScim(result, status)
    }

    def update(String id) {
        ScimGroup scimGroup = fromJson(request.JSON as Map)
        log.trace("Update request for Group : ${id} via SCIM: ${scimGroup?.properties}")
        def result
        HttpStatus status = HttpStatus.OK
        try {
            if (scimGroup.id != id) {
                throw new InvalidRequestDataException("There is mismatch between group json payload id and reference id passed in URI")
            }
            result = scimGroupService.update(scimGroup)
        } catch (InvalidRequestDataException irde) {
            status = HttpStatus.BAD_REQUEST
            result = new ErrorResponse(detail: irde.message, status: status.value().toString())
        } catch (ResourceNotFoundException rnfe) {
            log.error(rnfe.message)
            status = HttpStatus.NOT_FOUND
            result = new ErrorResponse(detail: rnfe.message, status: status.value().toString())
        } catch (Exception ex) {
            log.error("Unknown exception due to for group name update ${scimGroup.displayName}", ex)
            status = HttpStatus.INTERNAL_SERVER_ERROR
            result = new ErrorResponse(detail: ex.message, status: status.value().toString())
        }
        renderScim(result, status)
    }

    def patch(String id) {
        PatchRequest patchRequest = new PatchRequest()
        patchRequest.id = id
        bindData(patchRequest, request.JSON as Map)
        log.trace("Patch request for Group : ${id} via SCIM: ${patchRequest?.properties}")
        def result = null
        HttpStatus status = HttpStatus.NO_CONTENT
        try {
            result = scimGroupService.patch(patchRequest)
        } catch (InvalidRequestDataException irde) {
            status = HttpStatus.BAD_REQUEST
            result = new ErrorResponse(detail: irde.message, status: status.value().toString())
        } catch (ResourceNotFoundException rnfe) {
            log.error(rnfe.message)
            status = HttpStatus.NOT_FOUND
            result = new ErrorResponse(detail: rnfe.message, status: status.value().toString())
        } catch (Exception ex) {
            log.error("Unknown exception due to for group patch update ${patchRequest.id}", ex)
            status = HttpStatus.INTERNAL_SERVER_ERROR
            result = new ErrorResponse(detail: ex.message, status: status.value().toString())
        }
        renderScim(result, status)
    }

    def delete(String id) {
        log.trace("Delete request for Group : ${id} via SCIM")
        def result = null
        HttpStatus status = HttpStatus.NO_CONTENT
        try {
            scimGroupService.delete(id)
        } catch (ResourceNotFoundException rnfe) {
            log.error(rnfe.message)
            status = HttpStatus.NOT_FOUND
            result = new ErrorResponse(detail: rnfe.message, status: status.value().toString())
        } catch (Exception ex) {
            log.error("Unknown exception due to for group delete ${id}", ex)
            status = HttpStatus.INTERNAL_SERVER_ERROR
            result = new ErrorResponse(detail: ex.message, status: status.value().toString())
        }
        renderScim(result, status)
    }

    def show(String id, String excludedAttributes, String attributes) {
        log.trace("Show request for Group : ${id} via SCIM : ${excludedAttributes} and attributes : ${attributes}")
        def result
        HttpStatus status = HttpStatus.OK
        try {
            result = scimGroupService.getGroup(id, excludedAttributes, attributes)
        } catch (ResourceNotFoundException rnfe) {
            log.error(rnfe.message)
            status = HttpStatus.NOT_FOUND
            result = new ErrorResponse(detail: rnfe.message, status: status.value().toString())
        }
        renderScim(result, status)
    }

    private void renderScim(def body, HttpStatus status = HttpStatus.OK) {
        response.status = status.value()
        if(body != null) {
            render text: (body as JSON).toString(),
                    contentType: "application/scim+json"
        }
    }

    private ScimGroup fromJson(Map json) {
        ScimGroup group = new ScimGroup()
        bindData(group, json)
        def ext = json[ScimGroup.EXT_URN]
        if (ext instanceof Map) {
            group.customExtension = new CustomGroupExtension(
                    tenant: ext.tenant as String
            )
        }
        return group
    }

}
