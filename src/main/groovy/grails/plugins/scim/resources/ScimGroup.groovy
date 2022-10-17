package grails.plugins.scim.resources

import grails.validation.Validateable
import groovy.transform.CompileStatic

@CompileStatic
class ScimGroup implements Validateable {
    String id
    String externalId
    String displayName
    String type = 'Direct'
    List<ScimUser> members = []
    Meta meta
    Set<String> schemas = ['urn:ietf:params:scim:schemas:core:2.0:Group'] as Set
}
