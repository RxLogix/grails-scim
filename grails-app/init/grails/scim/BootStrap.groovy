package grails.scim

import grails.converters.JSON
import grails.plugins.scim.resources.ScimGroup
import grails.plugins.scim.resources.ScimUser

class BootStrap {

    def init = { servletContext ->
        JSON.registerObjectMarshaller(ScimUser, scimResponseMarshaller)
        JSON.registerObjectMarshaller(ScimGroup, scimResponseMarshaller)
    }

    def destroy = {
    }

    static scimResponseMarshaller = { Object object ->
        List<String> fields = object.getClass()?.getDeclaredFields()?.grep { !it.synthetic }?.collect { it.name } ?: []
        def map = object.properties.findAll { (it.key in fields) && it.value != null }
        map.remove('class')
        return map
    }
}
