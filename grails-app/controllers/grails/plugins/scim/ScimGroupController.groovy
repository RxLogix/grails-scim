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
        int status = 201
        try {
            result = scimGroupService.save(scimGroup)
        } catch (InvalidRequestDataException irde) {
            result = new ErrorResponse(detail: irde.message, status: '400')
            status = 400
        } catch (ResourceConflictException re) {
            log.error(re.message)
            result = new ErrorResponse(detail: re.message, status: '409')
            status = 409
        } catch (Exception ex) {
            log.error("Unknown exception due to for group name save ${scimGroup.displayName}", ex)
            result = new ErrorResponse(detail: ex.message, status: '500')
            status = 500
        }
        renderScim(result, status)
    }

    def update() {
        ScimGroup scimGroup = fromJson(request.JSON as Map)
        log.trace("Update request for Group : ${scimGroup?.id} via SCIM: ${scimGroup?.properties}")
        def result
        int status = 200
        try {
            result = scimGroupService.update(scimGroup)
        } catch (InvalidRequestDataException irde) {
            result = new ErrorResponse(detail: irde.message, status: '400')
            status = 400
        } catch (ResourceNotFoundException rnfe) {
            log.error(rnfe.message)
            result = new ErrorResponse(detail: rnfe.message, status: '409')
            status = 409
        } catch (Exception ex) {
            log.error("Unknown exception due to for group name update ${scimGroup.displayName}", ex)
            result = new ErrorResponse(detail: ex.message, status: '500')
            status = 500
        }
        renderScim(result, status)
    }

    def patch(String id, PatchRequest patchRequest) {
        log.trace("Patch request for Group : ${id} via SCIM: ${patchRequest?.properties}")
        patchRequest.id = id
        def result
        int status = 204
        try {
            result = scimGroupService.patch(patchRequest)
        } catch (InvalidRequestDataException irde) {
            result = new ErrorResponse(detail: irde.message, status: '400')
            status = 400
        } catch (ResourceNotFoundException rnfe) {
            log.error(rnfe.message)
            result = new ErrorResponse(detail: rnfe.message, status: '409')
            status = 409
        } catch (Exception ex) {
            log.error("Unknown exception due to for group patch update ${patchRequest.id}", ex)
            result = new ErrorResponse(detail: ex.message, status: '500')
            status = 500
        }
        renderScim(result, status)
    }

    def delete(String id) {
        log.trace("Delete request for Group : ${id} via SCIM")
        try {
            scimGroupService.delete(id)
            response.status = 204
        } catch (ResourceNotFoundException rnfe) {
            log.error(rnfe.message)
            def result = new ErrorResponse(detail: rnfe.message, status: '404')
            renderScim(result, 404)
        } catch (Exception ex) {
            log.error("Unknown exception due to for group delete ${id}", ex)
            def result = new ErrorResponse(detail: ex.message, status: '500')
            renderScim(result, 500)
        }
    }

    def show(String id, String excludedAttributes, String attributes) {
        log.trace("Show request for Group : ${id} via SCIM : ${excludedAttributes} and attributes : ${attributes}")
        def result
        int status = 200
        try {
            result = scimGroupService.getGroup(id, excludedAttributes, attributes)
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
