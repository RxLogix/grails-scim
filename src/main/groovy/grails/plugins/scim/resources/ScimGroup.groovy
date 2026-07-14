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
    // Extension
    CustomGroupExtension customExtension

    Set<String> schemas = [
            'urn:ietf:params:scim:schemas:core:2.0:Group'
    ] as Set

    static final String EXT_URN =
            'urn:ietf:params:scim:schemas:extension:custom:2.0:Group'

    Set<String> getSchemas() {
        if (customExtension?.tenant) {
            return schemas + [EXT_URN]
        }
        return schemas
    }


}
