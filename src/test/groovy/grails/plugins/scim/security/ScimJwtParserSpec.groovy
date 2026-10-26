package grails.plugins.scim.security

import spock.lang.Specification
import spock.lang.Unroll

import java.nio.charset.StandardCharsets
import java.time.Instant

class ScimJwtParserSpec extends Specification {

    static final byte[] SECRET = 'jwt-secret-for-tenant-001-0123456789abcdef'.getBytes(StandardCharsets.UTF_8)
    static final Instant NOW = Instant.ofEpochSecond(1_800_000_000L)

    private static Map header(Map overrides = [:]) {
        [alg: 'HS256', typ: 'JWT', kid: 'kid-1', tid: 'tenant-001'] + overrides
    }

    private static Map payload(Map overrides = [:]) {
        [iss: 'https://idp.example', sub: 'scim', iat: NOW.epochSecond - 10, exp: NOW.epochSecond + 3600, jti: 'j1'] + overrides
    }

    void "looksLikeJwt distinguishes three-part tokens"() {
        expect:
        ScimJwtParser.looksLikeJwt('a.b.c')
        !ScimJwtParser.looksLikeJwt('a.b')
        !ScimJwtParser.looksLikeJwt('a..c')
        !ScimJwtParser.looksLikeJwt('a.b.c.d')
        !ScimJwtParser.looksLikeJwt(null)
        !ScimJwtParser.looksLikeJwt('a.' + ('b' * 5000) + '.c')
    }

    void "sign then verify round trip returns claims"() {
        given:
        String token = ScimJwtParser.sign(header(), payload(), SECRET)

        when:
        def claims = ScimJwtParser.verify(token, SECRET, 'https://idp.example', 60, NOW)

        then:
        claims.tenantId == 'tenant-001'
        claims.kid == 'kid-1'
        claims.issuer == 'https://idp.example'
        claims.subject == 'scim'
        claims.jti == 'j1'
        claims.expiresAt == NOW.epochSecond + 3600
    }

    void "readHeader exposes tid and kid without a secret"() {
        when:
        def h = ScimJwtParser.readHeader(ScimJwtParser.sign(header(), payload(), SECRET))

        then:
        h.tenantId == 'tenant-001'
        h.kid == 'kid-1'
    }

    @Unroll
    void "readHeader rejects #label"() {
        when:
        ScimJwtParser.readHeader(ScimJwtParser.sign(header(h), payload(), SECRET))

        then:
        thrown(InvalidScimTokenException)

        where:
        label               | h
        'alg none'          | [alg: 'none']
        'alg RS256'         | [alg: 'RS256']
        'alg HS512'         | [alg: 'HS512']
        'missing kid'       | [kid: null]
        'missing tid'       | [tid: null]
        'tid with space'    | [tid: 'bad tenant']
        'kid too long'      | [kid: 'k' * 65]
    }

    void "verify rejects wrong secret"() {
        given:
        String token = ScimJwtParser.sign(header(), payload(), SECRET)

        when:
        ScimJwtParser.verify(token, 'another-secret-another-secret-another'.bytes, null, 60, NOW)

        then:
        thrown(InvalidScimTokenException)
    }

    void "verify rejects tampered payload"() {
        given:
        String token = ScimJwtParser.sign(header(), payload(), SECRET)
        String[] parts = token.split('\\.')
        String forged = Base64.urlEncoder.withoutPadding().encodeToString('{"exp":9999999999,"tid":"tenant-001"}'.bytes)

        when:
        ScimJwtParser.verify(parts[0] + '.' + forged + '.' + parts[2], SECRET, null, 60, NOW)

        then:
        thrown(InvalidScimTokenException)
    }

    void "verify rejects tampered header even though tid is in the header"() {
        given:
        String token = ScimJwtParser.sign(header(), payload(), SECRET)
        String[] parts = token.split('\\.')
        String forgedHeader = Base64.urlEncoder.withoutPadding().encodeToString('{"alg":"HS256","kid":"kid-1","tid":"tenant-999"}'.bytes)

        when:
        ScimJwtParser.verify(forgedHeader + '.' + parts[1] + '.' + parts[2], SECRET, null, 60, NOW)

        then:
        thrown(InvalidScimTokenException)
    }

    @Unroll
    void "verify rejects claim problem: #label"() {
        given:
        String token = ScimJwtParser.sign(header(), payload(p), SECRET)

        when:
        ScimJwtParser.verify(token, SECRET, expectedIssuer, 60, NOW)

        then:
        thrown(InvalidScimTokenException)

        where:
        label                     | p                                       | expectedIssuer
        'missing exp'             | [exp: null]                             | null
        'expired beyond skew'     | [exp: NOW.epochSecond - 61]             | null
        'exp not numeric'         | [exp: 'tomorrow']                       | null
        'nbf in future'           | [nbf: NOW.epochSecond + 120]            | null
        'payload tid mismatch'    | [tid: 'tenant-002']                     | null
        'issuer mismatch'         | [iss: 'https://other']                  | 'https://idp.example'
        'issuer missing'          | [iss: null]                             | 'https://idp.example'
    }

    void "verify tolerates exp and nbf within clock skew"() {
        given:
        String token = ScimJwtParser.sign(header(), payload(exp: NOW.epochSecond - 30, nbf: NOW.epochSecond + 30), SECRET)

        expect:
        ScimJwtParser.verify(token, SECRET, null, 60, NOW).tenantId == 'tenant-001'
    }

    void "verify accepts matching payload tid and ignores issuer when not enforced"() {
        given:
        String token = ScimJwtParser.sign(header(), payload(tid: 'tenant-001', iss: 'anything'), SECRET)

        expect:
        ScimJwtParser.verify(token, SECRET, null, 60, NOW).issuer == 'anything'
    }

    @Unroll
    void "verify rejects structurally invalid token: #label"() {
        when:
        ScimJwtParser.verify(token, SECRET, null, 60, NOW)

        then:
        thrown(InvalidScimTokenException)

        where:
        label                 | token
        'two parts'           | 'a.b'
        'non json header'     | b64('nope') + '.' + b64('{}') + '.' + b64('sig')
        'json array header'   | b64('[1]') + '.' + b64('{}') + '.' + b64('sig')
        'bad base64 sig'      | b64('{"alg":"HS256","kid":"k","tid":"t"}') + '.' + b64('{}') + '.!!!'
    }

    void "verify rejects empty secret"() {
        when:
        ScimJwtParser.verify(ScimJwtParser.sign(header(), payload(), SECRET), new byte[0], null, 60, NOW)

        then:
        thrown(InvalidScimTokenException)
    }

    private static String b64(String s) {
        Base64.urlEncoder.withoutPadding().encodeToString(s.getBytes(StandardCharsets.UTF_8))
    }
}
