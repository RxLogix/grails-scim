package grails.plugins.scim

import groovy.json.JsonOutput
import io.jsonwebtoken.JwtBuilder
import io.jsonwebtoken.Jwts

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import java.nio.charset.StandardCharsets
import java.security.SecureRandom

/**
 * Builds SCIM tokens for specs the same way New Admin's JwtTokenUtility does
 * (32-byte base64 secret, HS256, sub=scim, iss=Rx-NewPvAdmin, numeric tenantId, no exp).
 */
class ScimTestTokens {

    static String newSecret() {
        byte[] keyBytes = new byte[32]
        new SecureRandom().nextBytes(keyBytes)
        return Base64.getEncoder().encodeToString(keyBytes)
    }

    static String mint(String base64Secret, Object tenantId, String subject = 'scim', String issuer = 'Rx-NewPvAdmin') {
        JwtBuilder builder = Jwts.builder()
                .subject(subject)
                .issuer(issuer)
                .issuedAt(new Date())
        if (tenantId != null) {
            builder.claim('tenantId', tenantId)
        }
        return builder.signWith(new SecretKeySpec(Base64.getDecoder().decode(base64Secret), 'HmacSHA256')).compact()
    }

    /**
     * Hand-built token for algorithm-confusion cases jjwt refuses to create.
     * {@code macAlgorithm == null} produces an unsigned token (empty signature part).
     */
    static String raw(Map header, Map payload, String base64Secret, String macAlgorithm) {
        String signingInput = encode(JsonOutput.toJson(header).getBytes(StandardCharsets.UTF_8)) + '.' +
                encode(JsonOutput.toJson(payload).getBytes(StandardCharsets.UTF_8))
        if (macAlgorithm == null) {
            return signingInput + '.'
        }
        Mac mac = Mac.getInstance(macAlgorithm)
        mac.init(new SecretKeySpec(Base64.getDecoder().decode(base64Secret), macAlgorithm))
        return signingInput + '.' + encode(mac.doFinal(signingInput.getBytes(StandardCharsets.US_ASCII)))
    }

    private static String encode(byte[] bytes) {
        Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

}
