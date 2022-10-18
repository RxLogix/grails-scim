package grails.plugins.scim.resources

import grails.plugins.scim.utils.ScimUtil
import groovy.transform.CompileStatic

@CompileStatic
class Meta {

    String created
    String lastModified
    String resourceType = "User"
    String location

    void setCreated(Date date) {
        this.created = date.format(ScimUtil.ISO_DATE_TIME_FORMAT)
    }

    void setLastModified(Date date) {
        this.lastModified = date.format(ScimUtil.ISO_DATE_TIME_FORMAT)
    }

    void setLocation(String id) {
        this.location = "/scim/v2/${resourceType}s/$id"
    }

}
