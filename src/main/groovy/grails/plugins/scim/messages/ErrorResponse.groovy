package grails.plugins.scim.messages

import groovy.transform.CompileStatic

@CompileStatic
class ErrorResponse {
    String status
    String detail
    Set<String> schemas = ['urn:ietf:params:scim:api:messages:2.0:Error'] as Set
}
