package grails.plugins.scim.exceptions

import groovy.transform.CompileStatic

/**
 * Thrown when a SCIM bearer token is rejected (PVADMIN-50831).
 *
 * The message only names the {@link Reason}; it never contains the token, the secret or the
 * underlying parser message, so it is safe to log. The client always gets the same generic 401.
 */
@CompileStatic
class ScimAuthenticationException extends RuntimeException {

    enum Reason {
        /** No {@code Authorization: Bearer <token>} header. */
        MISSING_TOKEN,
        /** Not a parseable JWT, or no usable numeric tenantId in the payload. */
        MALFORMED_TOKEN,
        /** No config / no signing secret for the tenant in the token. */
        UNKNOWN_TENANT,
        /** Signature, algorithm, subject, issuer or tenantId check failed. */
        INVALID_TOKEN,
        /** Non-JWT token that does not match the default tenant's legacy token. */
        INVALID_LEGACY_TOKEN
    }

    final Reason reason

    ScimAuthenticationException(Reason reason) {
        super(reason.name())
        this.reason = reason
    }

}
