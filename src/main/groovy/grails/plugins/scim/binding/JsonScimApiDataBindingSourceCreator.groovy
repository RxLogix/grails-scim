package grails.plugins.scim.binding

import grails.core.GrailsApplication
import grails.web.mime.MimeType
import groovy.transform.CompileStatic
import org.grails.web.databinding.bindingsource.JsonApiDataBindingSourceCreator

@CompileStatic
class JsonScimApiDataBindingSourceCreator extends JsonApiDataBindingSourceCreator {

    GrailsApplication grailsApplication

    private static final MimeType SCIM_JSON = new MimeType('application/scim+json', 'json')
    private static final MimeType SCIM_JSON_UTF8 = new MimeType('application/scim+json;charset=utf-8', 'json')

    @Override
    MimeType[] getMimeTypes() {
        List<String> configuredMimeTypes = (List<String>) grailsApplication.config.getProperty(
                "grails.scim.mime.types", List
        )

        if (configuredMimeTypes != null && !configuredMimeTypes.isEmpty()) {
            List<MimeType> mimeTypes = []
            for (String mime : configuredMimeTypes) {
                mimeTypes << new MimeType(mime, "json")
            }
            return mimeTypes as MimeType[]
        }
        return [SCIM_JSON, SCIM_JSON_UTF8] as MimeType[]
    }
}
