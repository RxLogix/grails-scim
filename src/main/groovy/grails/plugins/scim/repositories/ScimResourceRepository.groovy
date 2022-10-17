package grails.plugins.scim.repositories

import grails.plugins.scim.messages.ListResponse
import grails.plugins.scim.resources.operations.PatchRequest
import groovy.transform.CompileStatic

@CompileStatic
interface ScimResourceRepository<T> {

    T get(String groupId, String excludedAttributes);

    T save(T resource);

    T update(T resource);

    T patch(PatchRequest patch);

    void delete(String id)

    ListResponse findAll(String filter, Integer count, Integer startIndex, String excludedAttributes)

}