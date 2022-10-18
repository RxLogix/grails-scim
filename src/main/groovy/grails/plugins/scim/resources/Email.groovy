package grails.plugins.scim.resources

import groovy.transform.CompileStatic

@CompileStatic
class Email {
    String value
    String type = 'work'
    Boolean primary
}
