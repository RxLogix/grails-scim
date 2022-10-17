package grails.plugins.scim

import grails.gorm.transactions.ReadOnly
import grails.plugins.scim.exceptions.ResourceConflictException
import grails.plugins.scim.exceptions.ResourceNotFoundException
import grails.plugins.scim.messages.ListResponse
import grails.plugins.scim.resources.operations.PatchRequest
import grails.plugins.scim.resources.ScimUser

@ReadOnly
class ScimUserService {

    def scimUserRepository

    ScimUser getUser(String scimId, String excludedAttributes) {
        scimUserRepository.get(scimId, excludedAttributes)
    }

    ScimUser save(ScimUser user) throws ResourceConflictException {
        scimUserRepository.save(user)
    }

    ScimUser update(ScimUser user) throws ResourceNotFoundException {
        scimUserRepository.update(user)
    }

    ScimUser patch(PatchRequest patchRequest) throws ResourceNotFoundException {
        scimUserRepository.patch(patchRequest)
    }

    void delete(String id) {
        scimUserRepository.delete(id)
    }

    ListResponse list(String filter, Integer count, Integer startIndex, String excludedAttributes) {
        scimUserRepository.findAll(filter, count, startIndex, excludedAttributes)
    }
}
