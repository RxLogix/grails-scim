package grails.plugins.scim.resources

import grails.validation.Validateable
import groovy.transform.CompileStatic

@CompileStatic
class ScimUser implements Validateable {
    String id
    String externalId
    String userName
    String displayName
    String timezone
    String locale
    Boolean active
    List<Email> emails = []
    List<ScimGroup> groups = []
    Set<String> schemas = ['urn:ietf:params:scim:schemas:core:2.0:User'] as Set
    Meta meta
    String type = 'User'

    //In case of Group User binding
    void setValue(String value) {
        this.id = value
    }
}
