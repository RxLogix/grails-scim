package grails.plugins.scim.security

import grails.config.Config
import grails.core.GrailsApplication
import spock.lang.Specification

class ConfigScimTenantKeyResolverSpec extends Specification {

    Config config = Mock(Config)
    GrailsApplication application = Mock(GrailsApplication) { getConfig() >> config }
    ConfigScimTenantKeyResolver resolver = new ConfigScimTenantKeyResolver(grailsApplication: application)

    static final byte[] DIGEST = ScimTenantTokenParser.sha256('secret'.bytes)

    void "returns digest from hex value under tenantId.grails.scim.api_token"() {
        given:
        config.getProperty('tenant-001.grails.scim.api_token', String) >> DIGEST.encodeHex().toString()

        expect:
        resolver.resolveSecretHash('tenant-001') == DIGEST
    }

    void "returns digest from base64 value"() {
        given:
        config.getProperty('tenant-002.grails.scim.api_token', String) >> Base64.encoder.encodeToString(DIGEST)

        expect:
        resolver.resolveSecretHash('tenant-002') == DIGEST
    }

    void "returns null for unknown tenant"() {
        given:
        config.getProperty(_, String) >> null

        expect:
        resolver.resolveSecretHash('unknown') == null
    }

    void "treats plaintext or malformed value as absent"() {
        given:
        config.getProperty('tenant-003.grails.scim.api_token', String) >> value

        expect:
        resolver.resolveSecretHash('tenant-003') == null

        where:
        value << ['plaintext-secret', 'abcd', Base64.encoder.encodeToString(new byte[16]), '!!not-base64!!']
    }

    void "never builds a config key from an invalid tenant id"() {
        when:
        byte[] result = resolver.resolveSecretHash(tenantId)

        then:
        0 * config.getProperty(_, _)
        result == null

        where:
        tenantId << [null, '', 'bad tenant', '../grails', 'a' * 65]
    }
}
