package grails.plugins.scim.exceptions


/**
 * Signals the specified request is invalid due to validation of incoming data.
 *
 * This exception corresponds to HTTP response code 400 Invalid Request.
 */

class InvalidRequestDataException extends Exception
{

    private static final long serialVersionUID = -7605955982293892224L;

    /**
     * Create a new <code>InvalidRequestDataException</code> from the provided
     * information.
     *
     * @param errorMessage  The error message for this SCIM exception.
     */
    public InvalidRequestDataException(final String errorMessage) {
        super(errorMessage);
    }

}
