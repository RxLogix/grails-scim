package grails.plugins.scim.binding

import grails.web.mime.MimeType
import groovy.transform.CompileStatic
import org.grails.web.databinding.bindingsource.JsonApiDataBindingSourceCreator

@CompileStatic
class JsonScimApiDataBindingSourceCreator extends JsonApiDataBindingSourceCreator {

    private static final MimeType SCIM_JSON = new MimeType('application/scim+json', 'json')
    private static final MimeType SCIM_JSON_UTF8 = new MimeType('application/scim+json;charset=utf-8', 'json')

    @Override
    MimeType[] getMimeTypes() {
        return [SCIM_JSON, SCIM_JSON_UTF8] as MimeType[]
    }
}
