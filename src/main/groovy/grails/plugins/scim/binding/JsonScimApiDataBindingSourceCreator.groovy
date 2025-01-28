package grails.plugins.scim.binding

import grails.web.mime.MimeType
import groovy.transform.CompileStatic
import org.grails.web.databinding.bindingsource.JsonApiDataBindingSourceCreator

@CompileStatic
class JsonScimApiDataBindingSourceCreator extends JsonApiDataBindingSourceCreator {

    MimeType SCIM_JSON_API = new MimeType('application/scim+json', "json")

    @Override
    MimeType[] getMimeTypes() {
        [SCIM_JSON_API] as MimeType[]
    }

}
