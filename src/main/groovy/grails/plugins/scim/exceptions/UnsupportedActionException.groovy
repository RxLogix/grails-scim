package grails.plugins.scim.exceptions

import groovy.transform.CompileStatic
import org.grails.core.exceptions.GrailsRuntimeException

/**
 * Signals the specified action is not supported or doesn't exist.
 *
 * This exception corresponds to HTTP response code 501 NOT Supported.
 */

@CompileStatic
class UnsupportedActionException extends GrailsRuntimeException {

    UnsupportedActionException(String message) {
        super(message)
    }
}
