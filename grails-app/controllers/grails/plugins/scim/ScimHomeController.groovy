package grails.plugins.scim

import grails.converters.JSON
import grails.plugins.scim.exceptions.ResourceConflictException
import grails.plugins.scim.exceptions.ResourceNotFoundException
import groovy.util.logging.Slf4j

@Slf4j
class ScimHomeController {

    static allowedMethods = [schemas: 'GET', serviceProvider: 'GET', resourceTypes: 'GET']

    def index() {
        log.warn('Reached to empty Index action of scim home')
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
        log.warn('ServiceProviderConfig needs to be implemented.') //TODO
        render([] as JSON)
    }

    def resourceTypes() {
        log.warn('ResourceTypes needs to be implemented.') //TODO
        render([] as JSON)
    }
}
