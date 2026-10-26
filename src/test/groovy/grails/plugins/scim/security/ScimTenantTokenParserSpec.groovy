package grails.plugins.scim.security

import spock.lang.Specification
import spock.lang.Unroll

import java.nio.charset.StandardCharsets

class ScimTenantTokenParserSpec extends Specification {

    void "build and parse round trip"() {
        given:
        byte[] secret = new byte[32]
        new Random(42).nextBytes(secret)

        when:
        String token = ScimTenantTokenParser.build('tenant-001', secret)
        def parsed = ScimTenantTokenParser.parse(token)

        then:
        parsed.tenantId == 'tenant-001'
        parsed.secret == secret
        !token.contains('=')
        token.count('.') == 1
    }

    @Unroll
    void "parse rejects malformed token: #label"() {
        when:
        ScimTenantTokenParser.parse(token)

        then:
        def ex = thrown(InvalidScimTokenException)
        ex.message == 'Invalid tenant token'

        where:
        label                    | token
        'null'                   | null
        'empty'                  | ''
        'no separator'           | b64('tenant-001')
        'empty secret part'      | b64('tenant-001') + '.'
        'empty tenant part'      | '.' + b64('secret')
        'three parts'            | b64('tenant-001') + '.' + b64('secret') + '.' + b64('extra')
        'invalid base64 tenant'  | '!!!.' + b64('secret')
        'invalid base64 secret'  | b64('tenant-001') + '.!!!'
        'tenant with space'      | b64('tenant 001') + '.' + b64('secret')
        'tenant with slash'      | b64('tenant/001') + '.' + b64('secret')
        'tenant too long'        | b64('a' * 65) + '.' + b64('secret')
        'oversized token'        | b64('tenant-001') + '.' + ('A' * 1100)
    }

    void "build rejects invalid tenant id or empty secret"() {
        when:
        ScimTenantTokenParser.build(tenantId, secret)

        then:
        thrown(IllegalArgumentException)

        where:
        tenantId     | secret
        'bad tenant' | 'x'.bytes
        ''           | 'x'.bytes
        'tenant-001' | new byte[0]
        'tenant-001' | null
    }

    void "sha256Hex produces a 64 char lowercase hex digest"() {
        when:
        String hex = ScimTenantTokenParser.sha256Hex('abc'.getBytes(StandardCharsets.UTF_8))

        then:
        hex == 'ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad'
    }

    private static String b64(String s) {
        Base64.urlEncoder.withoutPadding().encodeToString(s.getBytes(StandardCharsets.UTF_8))
    }
}
