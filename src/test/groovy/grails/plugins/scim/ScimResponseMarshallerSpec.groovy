package grails.plugins.scim

import grails.plugins.scim.resources.CustomUserExtension
import grails.plugins.scim.resources.ScimGroup
import grails.plugins.scim.resources.ScimUser
import grails.scim.BootStrap
import grails.util.Holders
import org.grails.testing.GrailsUnitTest
import spock.lang.Specification

class ScimResponseMarshallerSpec extends Specification implements GrailsUnitTest {

    void setup() {
        // BootStrap reads grails.scim.separator via Holders, which the unit runtime does not populate
        Holders.config = grailsApplication.config
    }

    void cleanup() {
        Holders.config = null
    }

    void "marshaller ignores Validateable trait state after validate() has been called"() {
        given:
        ScimUser user = new ScimUser(id: 'u1', userName: 'john')
        user.validate()
        ScimGroup group = new ScimGroup(id: 'g1', displayName: 'Admins', members: [user])
        group.validate()

        when:
        Map userMap = BootStrap.scimResponseMarshaller(user) as Map
        Map groupMap = BootStrap.scimResponseMarshaller(group) as Map

        then:
        userMap.keySet().every { !it.toString().contains('__') }
        !userMap.containsKey('EXT_URN')
        !userMap.containsKey('errors')
        !userMap.containsKey('constraintsMap')
        userMap.userName == 'john'
        groupMap.displayName == 'Admins'
        groupMap.members == [user]
    }

    void "marshaller emits custom extension under its URN and drops nulls"() {
        given:
        ScimUser user = new ScimUser(id: 'u1', userName: 'john',
                customExtension: new CustomUserExtension(tenants: ['t1', 't2'] as Set))

        when:
        Map map = BootStrap.scimResponseMarshaller(user) as Map

        then:
        map[ScimUser.EXT_URN].tenants == 't1,t2'
        !map.containsKey('customExtension')
        !map.containsKey('displayName')
    }
}
