package grails.plugins.scim.security

import grails.config.Config
import grails.core.GrailsApplication
import spock.lang.Specification

class ConfigScimTenantJwtSecretResolverSpec extends Specification {

    Config config = Mock(Config)
    GrailsApplication application = Mock(GrailsApplication) { getConfig() >> config }
    ConfigScimTenantJwtSecretResolver resolver = new ConfigScimTenantJwtSecretResolver(grailsApplication: application)

    static final byte[] SECRET = new byte[32].with { new Random(7).nextBytes(it); it }

    void "returns secret from tenantId.grails.scim.jwt.kid"() {
        given:
        config.getProperty('tenant-001.grails.scim.jwt.kid-1', String) >> Base64.encoder.encodeToString(SECRET)

        expect:
        resolver.resolveSecret('tenant-001', 'kid-1') == SECRET
    }

    void "returns null for unknown tenant or kid"() {
        given:
        config.getProperty(_, String) >> null

        expect:
        resolver.resolveSecret('tenant-001', 'kid-x') == null
    }

    void "treats short or malformed secret as absent"() {
        given:
        config.getProperty('tenant-001.grails.scim.jwt.kid-1', String) >> value

        expect:
        resolver.resolveSecret('tenant-001', 'kid-1') == null

        where:
        value << [Base64.encoder.encodeToString(new byte[31]), 'not base64 !!', 'short']
    }

    void "never builds a key from invalid ids or the reserved issuer kid"() {
        when:
        byte[] result = resolver.resolveSecret(tenantId, kid)

        then:
        0 * config.getProperty(_, _)
        result == null

        where:
        tenantId     | kid
        'bad tenant' | 'kid-1'
        null         | 'kid-1'
        'tenant-001' | 'bad kid'
        'tenant-001' | null
        'tenant-001' | 'issuer'
    }

    void "expectedIssuer reads optional issuer key"() {
        given:
        config.getProperty('tenant-001.grails.scim.jwt.issuer', String) >> 'https://idp.example'
        config.getProperty('tenant-002.grails.scim.jwt.issuer', String) >> null

        expect:
        resolver.expectedIssuer('tenant-001') == 'https://idp.example'
        resolver.expectedIssuer('tenant-002') == null
        resolver.expectedIssuer('bad tenant') == null
    }
}
