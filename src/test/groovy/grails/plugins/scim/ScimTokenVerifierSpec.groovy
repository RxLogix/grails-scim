package grails.plugins.scim

import grails.plugins.scim.auth.ScimTokenVerifier
import grails.plugins.scim.exceptions.ScimAuthenticationException
import grails.plugins.scim.exceptions.ScimAuthenticationException.Reason
import spock.lang.Specification

class ScimTokenVerifierSpec extends Specification {

    ScimTokenVerifier verifier = new ScimTokenVerifier()
    String secret = ScimTestTokens.newSecret()

    // ------------------------
    // VALID TOKEN
    // ------------------------
    void "token minted like New Admin is verified and yields its tenantId"() {
        given:
        String token = ScimTestTokens.mint(secret, 5L)

        when:
        Long unverifiedTenantId = verifier.readUnverifiedTenantId(token)
        Long verifiedTenantId = verifier.verify(token, secret, unverifiedTenantId)

        then:
        unverifiedTenantId == 5L
        verifiedTenantId == 5L
    }

    void "hand-built HS256 token with the right key is accepted"() {
        given:
        String token = ScimTestTokens.raw([alg: 'HS256'], [sub: 'scim', iss: 'Rx-NewPvAdmin', tenantId: 7], secret, 'HmacSHA256')

        expect:
        verifier.verify(token, secret, 7L) == 7L
    }

    void "isJwt distinguishes compact JWTs from legacy tokens"() {
        expect:
        verifier.isJwt(token) == expected

        where:
        token          || expected
        'aaa.bbb.ccc'  || true
        'static-token' || false
        'a.b'          || false
        'a.b.c.d'      || false
        null           || false
    }

    // ------------------------
    // SIGNATURE / ALGORITHM
    // ------------------------
    void "token signed with another secret is rejected"() {
        given: 'a token signed with the previous (rotated) secret'
        String token = ScimTestTokens.mint(ScimTestTokens.newSecret(), 5L)

        when:
        verifier.verify(token, secret, 5L)

        then:
        ScimAuthenticationException e = thrown()
        e.reason == Reason.INVALID_TOKEN
    }

    void "unsigned token (alg none) is rejected"() {
        given:
        String token = ScimTestTokens.raw([alg: 'none'], [sub: 'scim', iss: 'Rx-NewPvAdmin', tenantId: 5], secret, null)

        when:
        verifier.verify(token, secret, 5L)

        then:
        ScimAuthenticationException e = thrown()
        e.reason == Reason.INVALID_TOKEN
    }

    void "non-HS256 algorithm is rejected even when signed with the tenant secret"() {
        given:
        String token = ScimTestTokens.raw([alg: alg], [sub: 'scim', iss: 'Rx-NewPvAdmin', tenantId: 5], secret, mac)

        when:
        verifier.verify(token, secret, 5L)

        then:
        ScimAuthenticationException e = thrown()
        e.reason == Reason.INVALID_TOKEN

        where:
        alg     | mac
        'HS384' | 'HmacSHA384'
        'HS512' | 'HmacSHA512'
    }

    // ------------------------
    // CLAIMS
    // ------------------------
    void "wrong subject or issuer is rejected"() {
        given:
        String token = ScimTestTokens.mint(secret, 5L, subject, issuer)

        when:
        verifier.verify(token, secret, 5L)

        then:
        ScimAuthenticationException e = thrown()
        e.reason == Reason.INVALID_TOKEN

        where:
        subject | issuer
        'user'  | 'Rx-NewPvAdmin'
        'scim'  | 'someone-else'
    }

    void "verified tenantId must match the tenantId used to pick the secret"() {
        given:
        String token = ScimTestTokens.mint(secret, 5L)

        when:
        verifier.verify(token, secret, 6L)

        then:
        ScimAuthenticationException e = thrown()
        e.reason == Reason.INVALID_TOKEN
    }

    void "missing or non-numeric tenantId is malformed"() {
        given:
        String token = ScimTestTokens.mint(secret, tenantId)

        when:
        verifier.readUnverifiedTenantId(token)

        then:
        ScimAuthenticationException e = thrown()
        e.reason == Reason.MALFORMED_TOKEN

        where:
        tenantId << [null, '5', 5.5d]
    }

    void "unparseable token is malformed"() {
        when:
        verifier.readUnverifiedTenantId(token)

        then:
        ScimAuthenticationException e = thrown()
        e.reason == Reason.MALFORMED_TOKEN

        where:
        token << ['static-token', 'a.b.c', 'eyJhbGciOiJIUzI1NiJ9.bm90LWpzb24.sig']
    }

    void "exception message never contains the token"() {
        given:
        String token = ScimTestTokens.mint(ScimTestTokens.newSecret(), 5L)

        when:
        verifier.verify(token, secret, 5L)

        then:
        ScimAuthenticationException e = thrown()
        !e.message.contains(token)
        e.cause == null
    }

}
