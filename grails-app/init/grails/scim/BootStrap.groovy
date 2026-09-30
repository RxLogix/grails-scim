package grails.scim

import grails.converters.JSON
import grails.plugins.scim.resources.ScimGroup
import grails.plugins.scim.resources.ScimUser
import grails.util.Holders

import java.lang.reflect.Modifier

class BootStrap {

    static final String USER_EXT_URN =
            ScimUser.EXT_URN

    static final String GROUP_EXT_URN =
            ScimGroup.EXT_URN

    def init = { servletContext ->
        JSON.registerObjectMarshaller(ScimUser, scimResponseMarshaller)
        JSON.registerObjectMarshaller(ScimGroup, scimResponseMarshaller)
    }

    def destroy = {
    }

    static scimResponseMarshaller = { Object object ->

        // Only marshal real instance state: skip synthetic/static fields and the
        // fields woven in by Groovy traits such as grails.validation.Validateable
        // (grails_validation_Validateable__constraintsMapInternal etc.), which hold
        // ConstrainedDelegate instances the JSON converter cannot serialize.
        List<String> fields = object.getClass()
                ?.getDeclaredFields()
                ?.grep { !it.synthetic && !Modifier.isStatic(it.modifiers) && !it.name.contains('__') }
                ?.collect { it.name } ?: []

        fields = fields - ['customExtension']

        def map = object.properties
                .findAll { (it.key in fields) && it.value != null }

        map.remove('class')

        // ---- User Extension ----
        if (object instanceof ScimUser) {
            def ext = object.customExtension
            if (ext?.tenants) {
                map[USER_EXT_URN] = [
                        tenants: ext.tenants.join(Holders.config.getProperty('grails.scim.separator', ","))
                ]
            }
        }

        // ---- Group Extension ----
        if (object instanceof ScimGroup) {
            def ext = object.customExtension
            if (ext?.tenant) {
                map[GROUP_EXT_URN] = [
                        tenant: ext.tenant
                ]
            }
        }
        return map
    }

}
