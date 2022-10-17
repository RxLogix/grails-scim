package grails.plugins.scim

import grails.converters.JSON
import grails.plugins.scim.exceptions.ResourceConflictException
import grails.plugins.scim.exceptions.ResourceNotFoundException
import groovy.util.logging.Slf4j

@Slf4j
class ScimHomeController {

    static allowedMethods = [schemas: 'GET', serviceProvider: 'GET', resourceTypes: 'GET']

    def index() {
        log.info('Index called')
        render([] as JSON)
    }

    def schemas(String id) {
        def schema = JSON.parse(getClass().getResource('/schemas.json').text)
        if (id) {
            schema = schema.Resources.find { it.id == id }
            if (!schema) {
                response.status = 404
                return
            }
        }
        render(schema as JSON)
    }

    def serviceProviderConfig() {
        log.info('ServiceProviderConfig called')
        render([] as JSON)
    }

    def resourceTypes() {
        log.info('ResourceTypes called')
        render([] as JSON)
    }
}
