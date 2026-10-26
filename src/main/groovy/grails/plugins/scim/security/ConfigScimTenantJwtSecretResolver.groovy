package grails.plugins.scim.security

import grails.core.GrailsApplication
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

import java.util.concurrent.ConcurrentHashMap

/**
 * Default {@link ScimTenantJwtSecretResolver} reading
 * {@code <tenantId>.grails.scim.jwt.<kid>} (base64 raw secret, at least 32 bytes) and the optional
 * {@code <tenantId>.grails.scim.jwt.issuer}. A secret that is not valid base64 or is too short is
 * logged once and treated as absent.
 */
@CompileStatic
@Slf4j
class ConfigScimTenantJwtSecretResolver implements ScimTenantJwtSecretResolver {

    static final String KEY_INFIX = '.grails.scim.jwt.'
    static final String ISSUER_SUFFIX = KEY_INFIX + 'issuer'
    static final int MIN_SECRET_LENGTH = 32

    GrailsApplication grailsApplication

    private final Set<String> reportedInvalid = ConcurrentHashMap.newKeySet()

    @Override
    byte[] resolveSecret(String tenantId, String kid) {
        if (!ScimJwtParser.isValidId(tenantId) || !ScimJwtParser.isValidId(kid) || kid == 'issuer') {
            return null
        }
        String key = tenantId + KEY_INFIX + kid
        String configured = grailsApplication.config.getProperty(key, String)
        if (!configured) {
            return null
        }
        byte[] secret = decode(configured.trim())
        if (secret == null && reportedInvalid.add(key)) {
            log.error('Configured value for {} is not a base64 secret of at least {} bytes; key will be rejected', key, MIN_SECRET_LENGTH)
        }
        return secret
    }

    @Override
    String expectedIssuer(String tenantId) {
        if (!ScimJwtParser.isValidId(tenantId)) {
            return null
        }
        grailsApplication.config.getProperty(tenantId + ISSUER_SUFFIX, String) ?: null
    }

    private static byte[] decode(String value) {
        try {
            byte[] decoded = Base64.decoder.decode(value)
            return decoded.length >= MIN_SECRET_LENGTH ? decoded : null
        } catch (IllegalArgumentException ignored) {
            return null
        }
    }
}
