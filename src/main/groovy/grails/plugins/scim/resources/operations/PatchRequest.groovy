package grails.plugins.scim.resources.operations

import grails.validation.Validateable
import groovy.transform.CompileStatic

@CompileStatic
class PatchRequest implements Validateable {
    String id
    public List<Operation> Operations
}
