package grails.plugins.scim.resources

import groovy.transform.CompileStatic

@CompileStatic
class CustomUserExtension {
    Set<String> tenants = [] as Set
}