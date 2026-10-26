package grails.plugins.scim.security

import groovy.transform.CompileStatic
import groovy.transform.InheritConstructors

/**
 * Raised when a bearer value cannot be parsed or verified as a tenant token.
 * The message is intentionally generic so callers cannot learn which check failed.
 */
@CompileStatic
@InheritConstructors
class InvalidScimTokenException extends RuntimeException {
}
