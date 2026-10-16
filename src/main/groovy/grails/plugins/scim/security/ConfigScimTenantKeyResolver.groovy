package grails.plugins.scim.security

import grails.core.GrailsApplication
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

import java.util.concurrent.ConcurrentHashMap

/**
 * Default {@link ScimTenantKeyResolver} that reads {@code <tenantId>.grails.scim.api_token}
 * from application configuration. The value must be the hex or base64 encoded SHA-256 digest
 * of the tenant secret; plaintext secrets are not accepted. A malformed value is logged once
 * and treated as absent.
 */
@CompileStatic
@Slf4j
class ConfigScimTenantKeyResolver implements ScimTenantKeyResolver {

    static final String KEY_SUFFIX = '.grails.scim.api_token'
    private static final int SHA256_LENGTH = 32

    GrailsApplication grailsApplication

    private final Set<String> reportedInvalid = ConcurrentHashMap.newKeySet()

    @Override
    byte[] resolveSecretHash(String tenantId) {
        if (!tenantId || !ScimTenantTokenParser.TENANT_ID_PATTERN.matcher(tenantId).matches()) {
            return null
        }
        String key = tenantId + KEY_SUFFIX
        String configured = grailsApplication.config.getProperty(key, String)
        if (!configured) {
            return null
        }
        byte[] digest = decodeDigest(configured.trim())
        if (digest == null && reportedInvalid.add(tenantId)) {
            log.error('Configured value for {} is not a valid SHA-256 digest; tenant will be rejected', key)
        }
        return digest
    }

    private static byte[] decodeDigest(String value) {
        try {
            if (value.length() == SHA256_LENGTH * 2 && value ==~ /^[0-9a-fA-F]+$/) {
                return value.decodeHex()
            }
            byte[] decoded = Base64.decoder.decode(value)
            return decoded.length == SHA256_LENGTH ? decoded : null
        } catch (IllegalArgumentException ignored) {
            return null
        }
    }
}
