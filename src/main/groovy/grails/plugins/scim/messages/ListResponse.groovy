package grails.plugins.scim.messages

import groovy.transform.CompileStatic

@CompileStatic
class ListResponse {
    Long totalResults
    Integer startIndex
    Integer itemsPerPage
    Set<String> schemas = ['urn:ietf:params:scim:api:messages:2.0:ListResponse'] as Set
    public List<Object> Resources
}
