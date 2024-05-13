package grails.plugins.scim.resources

import grails.plugins.scim.utils.ScimUtil
import groovy.transform.CompileStatic

import java.text.SimpleDateFormat

@CompileStatic
class Meta {

    String created
    String lastModified
    String resourceType = "User"
    String location


    public void setCreated(Date date) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(ScimUtil.ISO_DATE_TIME_FORMAT);
        this.created = dateFormat.format(date);
    }

    void setLastModified(Date date) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(ScimUtil.ISO_DATE_TIME_FORMAT)
        this.lastModified = dateFormat.format(date);
    }

    void setLocation(String id) {
        this.location = "/scim/v2/${resourceType}s/$id"
    }

}
