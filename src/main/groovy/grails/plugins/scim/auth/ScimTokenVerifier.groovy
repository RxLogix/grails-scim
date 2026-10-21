package grails.plugins.scim.auth

import grails.plugins.scim.exceptions.ScimAuthenticationException
import grails.plugins.scim.exceptions.ScimAuthenticationException.Reason
import groovy.json.JsonException
import groovy.json.JsonSlurper
import groovy.transform.CompileStatic
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jws
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts

import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec
import java.nio.charset.StandardCharsets

/**
 * Verifies the per-tenant SCIM JWT minted by New Admin (PVADMIN-50831).
 *
 * Token contract, from New Admin {@code JwtTokenUtility.generateScimToken}:
 * HS256, signed with the tenant's base64-decoded {@code grails.scim.jwt.secret};
 * claims {@code sub = "scim"}, {@code iss = "Rx-NewPvAdmin"}, numeric {@code tenantId}, {@code iat};
 * no {@code exp} (revocation is by rotating the secret).
 *
 * Verification is two-step because the signing key depends on the tenant:
 * {@link #readUnverifiedTenantId} only picks which secret to use, and nothing from the token is
 * trusted until {@link #verify} has checked the signature with that secret.
 */
@CompileStatic
class ScimTokenVerifier {

    static final String ALGORITHM = 'HS256'

    static final String SUBJECT = 'scim'

    static final String ISSUER = 'Rx-NewPvAdmin'

    static final String TENANT_ID_CLAIM = 'tenantId'

    private static final String HMAC_SHA256 = 'HmacSHA256'

    /** A compact JWS has exactly three dot-separated parts; anything else is treated as a legacy token. */
    boolean isJwt(String token) {
        token != null && token.count('.') == 2
    }

    /**
     * Reads {@code tenantId} from the payload WITHOUT verifying the signature.
     * Use the result only to look up the tenant's secret.
     */
    Long readUnverifiedTenantId(String token) {
        if (!isJwt(token)) {
            throw new ScimAuthenticationException(Reason.MALFORMED_TOKEN)
        }
        try {
            String payloadPart = token.split('\\.')[1]
            byte[] payloadJson = Base64.getUrlDecoder().decode(payloadPart)
            Object payload = new JsonSlurper().parseText(new String(payloadJson, StandardCharsets.UTF_8))
            Object tenantId = payload instanceof Map ? ((Map) payload).get(TENANT_ID_CLAIM) : null
            return toTenantId(tenantId, Reason.MALFORMED_TOKEN)
        } catch (IllegalArgumentException | JsonException | ArrayIndexOutOfBoundsException ignored) {
            throw new ScimAuthenticationException(Reason.MALFORMED_TOKEN)
        }
    }

    /**
     * Verifies the token with the tenant's secret and returns the verified tenantId.
     *
     * @param base64Secret     the tenant's {@code grails.scim.jwt.secret}, base64 as stored by New Admin
     * @param expectedTenantId the tenantId returned by {@link #readUnverifiedTenantId} for the same token
     */
    Long verify(String token, String base64Secret, Long expectedTenantId) {
        Claims claims
        try {
            // jjwt rejects unsigned tokens (alg=none) in parseSignedClaims, and rejects HS384/HS512
            // here because the 256-bit tenant key is too short for them; the explicit header check
            // below pins HS256 regardless of the key length.
            Jws<Claims> jws = Jwts.parser()
                    .verifyWith(signingKey(base64Secret))
                    .requireSubject(SUBJECT)
                    .requireIssuer(ISSUER)
                    .build()
                    .parseSignedClaims(token)
            // Explicit getters: JwsHeader and Claims are Maps, so Groovy property syntax
            // (jws.header.algorithm) would do a map lookup of "algorithm" instead of reading "alg".
            if (jws.getHeader().getAlgorithm() != ALGORITHM) {
                throw new ScimAuthenticationException(Reason.INVALID_TOKEN)
            }
            claims = jws.getPayload()
        } catch (JwtException | IllegalArgumentException ignored) {
            // The jjwt message is dropped on purpose: it can echo token content.
            throw new ScimAuthenticationException(Reason.INVALID_TOKEN)
        }

        Long tenantId = toTenantId(claims.get(TENANT_ID_CLAIM), Reason.INVALID_TOKEN)
        // Defensive: the secret was chosen from the unverified tenantId, so the signed claim must match it.
        if (tenantId != expectedTenantId) {
            throw new ScimAuthenticationException(Reason.INVALID_TOKEN)
        }
        return tenantId
    }

    private static SecretKey signingKey(String base64Secret) {
        // Same derivation as New Admin's JwtTokenUtility.scimSigningKey.
        byte[] keyBytes = Base64.getDecoder().decode(base64Secret)
        return new SecretKeySpec(keyBytes, HMAC_SHA256)
    }

    /** Accepts integral JSON numbers only; a string or decimal tenantId is rejected. */
    private static Long toTenantId(Object value, Reason reason) {
        if (value instanceof Integer || value instanceof Long) {
            return ((Number) value).longValue()
        }
        if (value instanceof BigInteger && ((BigInteger) value).bitLength() < 64) {
            return ((BigInteger) value).longValue()
        }
        throw new ScimAuthenticationException(reason)
    }

}
