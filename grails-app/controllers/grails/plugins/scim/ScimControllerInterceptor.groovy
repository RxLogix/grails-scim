package grails.plugins.scim

import grails.util.Holders
import groovy.util.logging.Slf4j
import org.springframework.http.HttpStatus

import javax.servlet.http.HttpServletRequest

@Slf4j
class ScimControllerInterceptor {

    String apiToken = Holders.config.grails.scim.api_token

    ScimControllerInterceptor() {
        match controller: ~/(scimHome|scimUser|scimGroup|)/
    }

    boolean before() {
        if (!Holders.config.grails.scim.enabled) {
            log.debug('Scim is not enabled for this env.')
            render text: 'SCIM is not enabled', status: HttpStatus.FORBIDDEN
            return false
        }
        String[] bearer = request.getHeader("Authorization")?.split(" ") ?: []
        if (bearer.size() > 1 && bearer.last() == apiToken) {
            log.trace("Valid scim access token by Ip Address: {}", getClientIP(request))
            true
        } else {
            log.warn("Invalid scim token for accessing via {} by Ip Address: {}", request.getHeader("Authorization"), getClientIP(request))
            render text: "UNAUTHORIZED access on scim endpoint", status: HttpStatus.UNAUTHORIZED
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
}
