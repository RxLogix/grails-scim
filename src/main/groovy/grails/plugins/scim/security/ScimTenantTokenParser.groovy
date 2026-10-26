package grails.plugins.scim.security

import groovy.transform.CompileStatic

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.regex.Pattern

/**
 * Builds and parses the two-part tenant token: {@code base64url(tenantId).base64url(secret)}.
 * All validation happens here before any lookup or comparison takes place.
 */
@CompileStatic
final class ScimTenantTokenParser {

    static final int MAX_TOKEN_LENGTH = 1024
    static final Pattern TENANT_ID_PATTERN = Pattern.compile('^[A-Za-z0-9._-]{1,64}$')
    private static final String FAILURE_MESSAGE = 'Invalid tenant token'

    private static final Base64.Encoder ENCODER = Base64.urlEncoder.withoutPadding()
    private static final Base64.Decoder DECODER = Base64.urlDecoder

    private ScimTenantTokenParser() {}

    /** Immutable result of a successful parse. The raw secret is kept only long enough to hash it. */
    static final class ParsedToken {
        final String tenantId
        final byte[] secret

        ParsedToken(String tenantId, byte[] secret) {
            this.tenantId = tenantId
            this.secret = secret
        }
    }

    /** Builds a token for host administrator tooling. */
    static String build(String tenantId, byte[] secret) {
        if (!tenantId || !TENANT_ID_PATTERN.matcher(tenantId).matches()) {
            throw new IllegalArgumentException('tenantId does not match the allowed pattern')
        }
        if (secret == null || secret.length == 0) {
            throw new IllegalArgumentException('secret must not be empty')
        }
        ENCODER.encodeToString(tenantId.getBytes(StandardCharsets.UTF_8)) + '.' + ENCODER.encodeToString(secret)
    }

    /**
     * Parses a bearer value. Throws {@link InvalidScimTokenException} for any malformed input
     * without revealing which check failed.
     */
    static ParsedToken parse(String token) {
        if (token == null || token.isEmpty() || token.length() > MAX_TOKEN_LENGTH) {
            throw new InvalidScimTokenException(FAILURE_MESSAGE)
        }
        int dot = token.indexOf('.')
        if (dot <= 0 || dot == token.length() - 1 || token.indexOf('.', dot + 1) != -1) {
            throw new InvalidScimTokenException(FAILURE_MESSAGE)
        }
        String tenantId
        byte[] secret
        try {
            tenantId = new String(DECODER.decode(token.substring(0, dot)), StandardCharsets.UTF_8)
            secret = DECODER.decode(token.substring(dot + 1))
        } catch (IllegalArgumentException ignored) {
            throw new InvalidScimTokenException(FAILURE_MESSAGE)
        }
        if (!TENANT_ID_PATTERN.matcher(tenantId).matches() || secret.length == 0) {
            throw new InvalidScimTokenException(FAILURE_MESSAGE)
        }
        new ParsedToken(tenantId, secret)
    }

    static byte[] sha256(byte[] input) {
        MessageDigest.getInstance('SHA-256').digest(input)
    }

    /** Hex encoding of {@code sha256(secret)}, the form stored in configuration. */
    static String sha256Hex(byte[] input) {
        sha256(input).encodeHex().toString()
    }
}
