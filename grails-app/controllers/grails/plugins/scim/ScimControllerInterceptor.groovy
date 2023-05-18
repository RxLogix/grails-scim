package grails.plugins.scim

import grails.util.Holders
import groovy.util.logging.Slf4j

@Slf4j
class ScimControllerInterceptor {

    String apiToken = Holders.config.grails.scim.api_token

    ScimControllerInterceptor() {
        match controller: ~/(scimHome|scimUser|scimGroup|)/
    }

    boolean before() {
        String[] bearer = request.getHeader("Authorization")?.split(" ") ?: []
        if (bearer.size() > 1 && bearer.last() == apiToken) {
            true
        } else {
            log.trace("Invalid scim token for accessing via ${request.getHeader("Authorization")}")
            false
        }
    }


    boolean after() { true }

    void afterView() {
        // no-op
    }
}
