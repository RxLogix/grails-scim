package grails.plugins.scim

import grails.gorm.transactions.ReadOnly
import grails.plugins.scim.exceptions.ResourceConflictException
import grails.plugins.scim.exceptions.ResourceNotFoundException
import grails.plugins.scim.messages.ListResponse
import grails.plugins.scim.resources.operations.PatchRequest
import grails.plugins.scim.resources.ScimGroup

@ReadOnly
class ScimGroupService {

    def scimGroupRepository

    ScimGroup getGroup(String groupId, String excludedAttributes) {
        scimGroupRepository.get(groupId, excludedAttributes)
    }

    ScimGroup save(ScimGroup user) throws ResourceConflictException {
        scimGroupRepository.save(user)
    }

    ScimGroup update(ScimGroup group) throws ResourceNotFoundException {
        scimGroupRepository.update(group)
    }

    ScimGroup patch(PatchRequest patchRequest) throws ResourceNotFoundException {
        scimGroupRepository.patch(patchRequest)
    }

    void delete(String id) {
        scimGroupRepository.delete(id)
    }

    ListResponse list(String filter, Integer count, Integer startIndex, String excludedAttributes) {
        scimGroupRepository.findAll(filter, count, startIndex, excludedAttributes)
    }
}
