package grails.plugins.scim

import grails.converters.JSON
import grails.core.GrailsApplication
import groovy.util.logging.Slf4j
import org.springframework.http.HttpStatus
import javax.servlet.http.HttpServletRequest

@Slf4j
class ScimControllerInterceptor {

    GrailsApplication grailsApplication

    ScimControllerInterceptor() {
        match controller: ~/(scimHome|scimUser|scimGroup|)/
    }

    boolean before() {
        if (!grailsApplication.config.getProperty("grails.scim.enabled", Boolean.class)) {
            log.debug('Scim is not enabled for this env.')
            render text: 'SCIM is not enabled', status: HttpStatus.FORBIDDEN
            return false
        }
        String[] bearer = request.getHeader("Authorization")?.split(" ") ?: []
        String apiToken = grailsApplication.getConfig().getProperty("grails.scim.api_token", String.class)
        if (bearer.size() > 1 && bearer.last() == apiToken) {
            log.trace("Valid scim access token by Ip Address: {}", getClientIP(request))
            true
        } else {
            log.warn("Invalid scim token for accessing via {} by Ip Address: {}", request.getHeader("Authorization"), getClientIP(request))
            unauthorized("UNAUTHORIZED access on scim endpoint")
            return false
        }
    }


    boolean after() { true }

    void afterView() {
        // no-op
    }

    private String getClientIP(HttpServletRequest request) {
        String ip = request.getHeader("X-FORWARDED-FOR")
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr()
        }
        return ip
    }

    private void unauthorized(String message) {
        response.status = HttpStatus.UNAUTHORIZED.value()
        render([
                schemas: ["urn:ietf:params:scim:api:messages:2.0:Error"],
                status : "401",
                detail : message
        ] as JSON)
    }

}
