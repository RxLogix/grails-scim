package grails.plugins.scim

import grails.gorm.transactions.ReadOnly
import grails.plugins.scim.exceptions.ResourceConflictException
import grails.plugins.scim.exceptions.ResourceNotFoundException
import grails.plugins.scim.messages.ListResponse
import grails.plugins.scim.repositories.ScimResourceRepository
import grails.plugins.scim.resources.ScimUser
import grails.plugins.scim.resources.operations.PatchRequest
import grails.plugins.scim.resources.ScimGroup
import groovy.transform.CompileStatic
import org.springframework.beans.factory.annotation.Autowired

@ReadOnly
@CompileStatic
class ScimGroupService {

    @Autowired
    ScimResourceRepository scimGroupRepository

    ScimGroup getGroup(String groupId, String excludedAttributes, String includeAttributes) {
        scimGroupRepository.get(groupId, getExcludedProperties(excludedAttributes, includeAttributes))
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

    ListResponse list(String filter, Integer count, Integer startIndex, String excludedAttributes, String includeAttributes) {
        scimGroupRepository.findAll(filter, count, startIndex, getExcludedProperties(excludedAttributes, includeAttributes))
    }

    private String getExcludedProperties(String excludedAttributes, String includeAttributes) {
        if (includeAttributes) {
            String additionalExcludes = (ScimGroup.declaredFields.findAll {
                !it.synthetic && !(it.name in ['meta', 'type', 'schemas', 'id', 'externalId']) && !it.name.contains('$') && !it.name.contains('_')
            }*.name - includeAttributes.split(',').toList()).join(',')
            excludedAttributes = excludedAttributes ? (excludedAttributes + ',' + additionalExcludes) : additionalExcludes
        }
        return excludedAttributes
    }
}
