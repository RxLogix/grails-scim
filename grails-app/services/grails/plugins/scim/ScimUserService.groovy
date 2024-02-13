package grails.plugins.scim

import grails.gorm.transactions.ReadOnly
import grails.plugins.scim.exceptions.ResourceConflictException
import grails.plugins.scim.exceptions.ResourceNotFoundException
import grails.plugins.scim.messages.ListResponse
import grails.plugins.scim.repositories.ScimResourceRepository
import grails.plugins.scim.resources.operations.PatchRequest
import grails.plugins.scim.resources.ScimUser
import groovy.transform.CompileStatic
import org.springframework.beans.factory.annotation.Autowired

@ReadOnly
@CompileStatic
class ScimUserService {

    @Autowired
    ScimResourceRepository scimUserRepository

    ScimUser getUser(String scimId, String excludedAttributes, String includeAttributes) {
        scimUserRepository.get(scimId, getExcludedProperties(excludedAttributes, includeAttributes))
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

    ListResponse list(String filter, Integer count, Integer startIndex, String excludedAttributes, String includeAttributes) {
        scimUserRepository.findAll(filter, count, startIndex, getExcludedProperties(excludedAttributes, includeAttributes))
    }


    private String getExcludedProperties(String excludedAttributes, String includeAttributes) {
        if (includeAttributes) {
            String additionalExcludes = (ScimUser.declaredFields.findAll {
                !it.synthetic && !(it.name in ['meta', 'type', 'schemas','id','externalId']) && !it.name.contains('$') && !it.name.contains('_')
            }*.name - includeAttributes.split(',').toList()).join(',')
            excludedAttributes = excludedAttributes ? (excludedAttributes + ',' + additionalExcludes) : additionalExcludes
        }
        return excludedAttributes
    }

}
