package grails.plugins.scim.security

import groovy.json.JsonSlurper
import groovy.transform.CompileStatic

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.Instant
import java.util.regex.Pattern

/**
 * Minimal HS256 JWS verifier built on the JDK only.
 *
 * Expected shape: {@code base64url(header).base64url(payload).base64url(signature)} where the header
 * carries {@code alg=HS256}, {@code kid} and {@code tid}. The header is integrity-protected by the
 * signature, so {@code tid} and {@code kid} are safe to use for key lookup before verification and
 * safe to trust after it. The payload is never decoded before the signature has been verified.
 */
@CompileStatic
final class ScimJwtParser {

    static final int MAX_TOKEN_LENGTH = 4096
    static final String ALGORITHM = 'HS256'
    static final String MAC_ALGORITHM = 'HmacSHA256'
    static final Pattern ID_PATTERN = Pattern.compile('^[A-Za-z0-9._-]{1,64}$')
    private static final String FAILURE_MESSAGE = 'Invalid JWT'
    private static final Base64.Decoder DECODER = Base64.urlDecoder
    private static final Base64.Encoder ENCODER = Base64.urlEncoder.withoutPadding()

    private ScimJwtParser() {}

    /** Unverified header fields needed to locate the key. */
    static final class Header {
        final String tenantId
        final String kid

        Header(String tenantId, String kid) {
            this.tenantId = tenantId
            this.kid = kid
        }
    }

    /** Verified claims. */
    static final class Claims {
        final String tenantId
        final String kid
        final String issuer
        final String subject
        final String jti
        final Long expiresAt

        Claims(String tenantId, String kid, String issuer, String subject, String jti, Long expiresAt) {
            this.tenantId = tenantId
            this.kid = kid
            this.issuer = issuer
            this.subject = subject
            this.jti = jti
            this.expiresAt = expiresAt
        }
    }

    static boolean isValidId(String value) {
        value != null && ID_PATTERN.matcher(value).matches()
    }

    /** Cheap structural check used by the interceptor to pick the JWT flow. */
    static boolean looksLikeJwt(String token) {
        if (token == null || token.length() > MAX_TOKEN_LENGTH) {
            return false
        }
        String[] parts = token.split('\\.', -1)
        parts.length == 3 && parts.every { String p -> !p.isEmpty() }
    }

    /**
     * Reads {@code tid} and {@code kid} from the header without verifying anything else.
     * Rejects any algorithm other than HS256 so that {@code none} or asymmetric algorithms
     * can never reach the HMAC path.
     */
    static Header readHeader(String token) {
        String[] parts = split(token)
        Map header = decodeJson(parts[0])
        if (header.alg != ALGORITHM) {
            throw new InvalidScimTokenException(FAILURE_MESSAGE)
        }
        String tenantId = header.tid as String
        String kid = header.kid as String
        if (!isValidId(tenantId) || !isValidId(kid)) {
            throw new InvalidScimTokenException(FAILURE_MESSAGE)
        }
        new Header(tenantId, kid)
    }

    /**
     * Verifies the signature with {@code secret}, then validates the claims.
     *
     * @param expectedIssuer when non-null the {@code iss} claim must match
     * @param clockSkewSeconds tolerance applied to {@code exp} and {@code nbf}
     */
    static Claims verify(String token, byte[] secret, String expectedIssuer, long clockSkewSeconds, Instant now = Instant.now()) {
        if (secret == null || secret.length == 0) {
            throw new InvalidScimTokenException(FAILURE_MESSAGE)
        }
        String[] parts = split(token)
        Header header = readHeader(token)

        byte[] expected = hmac(secret, (parts[0] + '.' + parts[1]).getBytes(StandardCharsets.US_ASCII))
        byte[] presented
        try {
            presented = DECODER.decode(parts[2])
        } catch (IllegalArgumentException ignored) {
            throw new InvalidScimTokenException(FAILURE_MESSAGE)
        }
        if (!MessageDigest.isEqual(expected, presented)) {
            throw new InvalidScimTokenException(FAILURE_MESSAGE)
        }

        Map payload = decodeJson(parts[1])
        long nowSeconds = now.epochSecond

        Long exp = asLong(payload.exp)
        if (exp == null || nowSeconds > exp + clockSkewSeconds) {
            throw new InvalidScimTokenException(FAILURE_MESSAGE)
        }
        Long nbf = asLong(payload.nbf)
        if (nbf != null && nowSeconds + clockSkewSeconds < nbf) {
            throw new InvalidScimTokenException(FAILURE_MESSAGE)
        }
        if (payload.containsKey('tid') && payload.tid != header.tenantId) {
            throw new InvalidScimTokenException(FAILURE_MESSAGE)
        }
        String issuer = payload.iss as String
        if (expectedIssuer != null && issuer != expectedIssuer) {
            throw new InvalidScimTokenException(FAILURE_MESSAGE)
        }
        new Claims(header.tenantId, header.kid, issuer, payload.sub as String, payload.jti as String, exp)
    }

    /** Builds a signed token. Intended for tests and host tooling. */
    static String sign(Map header, Map payload, byte[] secret) {
        String h = ENCODER.encodeToString(groovy.json.JsonOutput.toJson(header).getBytes(StandardCharsets.UTF_8))
        String p = ENCODER.encodeToString(groovy.json.JsonOutput.toJson(payload).getBytes(StandardCharsets.UTF_8))
        String s = ENCODER.encodeToString(hmac(secret, (h + '.' + p).getBytes(StandardCharsets.US_ASCII)))
        h + '.' + p + '.' + s
    }

    // ---- helpers ----

    private static String[] split(String token) {
        if (!looksLikeJwt(token)) {
            throw new InvalidScimTokenException(FAILURE_MESSAGE)
        }
        token.split('\\.', -1)
    }

    private static Map decodeJson(String part) {
        try {
            Object parsed = new JsonSlurper().parseText(new String(DECODER.decode(part), StandardCharsets.UTF_8))
            if (!(parsed instanceof Map)) {
                throw new InvalidScimTokenException(FAILURE_MESSAGE)
            }
            return (Map) parsed
        } catch (InvalidScimTokenException e) {
            throw e
        } catch (Exception ignored) {
            throw new InvalidScimTokenException(FAILURE_MESSAGE)
        }
    }

    private static byte[] hmac(byte[] secret, byte[] data) {
        Mac mac = Mac.getInstance(MAC_ALGORITHM)
        mac.init(new SecretKeySpec(secret, MAC_ALGORITHM))
        mac.doFinal(data)
    }

    private static Long asLong(Object value) {
        value instanceof Number ? ((Number) value).longValue() : null
    }
}
