package grails.plugins.scim

import grails.converters.JSON
import grails.plugins.scim.resources.ScimUser
import groovy.util.logging.Slf4j

@Slf4j
class ScimHomeController {

    static allowedMethods = [
            schemas              : 'GET',
            serviceProviderConfig: 'GET',
            resourceTypes        : 'GET'
    ]

    // ---- SCIM URN Constants ----
    private static final String LIST_RESPONSE_URN =
            'urn:ietf:params:scim:api:messages:2.0:ListResponse'

    private static final String SERVICE_PROVIDER_URN =
            'urn:ietf:params:scim:schemas:core:2.0:ServiceProviderConfig'

    private static final String USER_SCHEMA_URN =
            'urn:ietf:params:scim:schemas:core:2.0:User'

    private static final String GROUP_SCHEMA_URN =
            'urn:ietf:params:scim:schemas:core:2.0:Group'

    private static final String USER_EXT_URN =
            ScimUser.EXT_URN

    private static final String GROUP_EXT_URN =
            ScimUser.EXT_URN

    // ---- Public Endpoints ----

    def index() {
        log.warn('Reached to empty Index action of scim home')
        render([] as JSON)
    }

    def schemas(String id) {
        def doc = loadSchemas()
        def baseUrl = getBaseUrl()

        if (id) {
            def schema = doc.Resources.find { it.id == id }
            if (!schema) {
                response.status = 404
                return
            }
            schema.meta.location = "${baseUrl}/scim/Schemas/${id}"
            render(schema as JSON)
            return
        }

        doc.Resources.each { s ->
            s.meta.location = "${baseUrl}/scim/Schemas/${s.id}"
        }

        render(doc as JSON)
    }

    def serviceProviderConfig() {
        def baseUrl = getBaseUrl()
        render(buildServiceProviderConfig(baseUrl) as JSON)
    }

    def resourceTypes() {
        def baseUrl = getBaseUrl()
        render(buildResourceTypes(baseUrl) as JSON)
    }

    // ---- Helpers ----

    private String getBaseUrl() {
        def proto = request.getHeader('X-Forwarded-Proto') ?: request.scheme
        def host = request.getHeader('X-Forwarded-Host') ?: request.serverName
        def port = request.serverPort

        return "${proto}://${host}" +
                ((port in [80, 443]) ? '' : ":${port}")
    }

    private Object loadSchemas() {
        JSON.parse(getClass().getResource('/schemas.json').text)
    }

    private Map buildServiceProviderConfig(String baseUrl) {
        [
                schemas              : [SERVICE_PROVIDER_URN],
                patch                : [supported: true],
                bulk                 : [supported: false, maxOperations: 0, maxPayloadSize: 0],
                filter               : [supported: true, maxResults: 200],
                changePassword       : [supported: false],
                sort                 : [supported: true],
                etag                 : [supported: false],
                authenticationSchemes: [[
                                                type            : "bearertoken",
                                                name            : "Static Bearer Token",
                                                description     : "Authorization via shared secret bearer token",
                                                specUri         : "https://datatracker.ietf.org/doc/html/rfc6750",
                                                documentationUri: "${baseUrl}/docs/scim-auth"
                                        ]],
                meta                 : [
                        resourceType: 'ServiceProviderConfig',
                        location    : "${baseUrl}/scim/ServiceProviderConfig"
                ]
        ]
    }

    private Map buildResourceTypes(String baseUrl) {
        [
                schemas     : [LIST_RESPONSE_URN],
                totalResults: 2,
                startIndex  : 1,
                itemsPerPage: 2,
                Resources   : [
                        buildResourceType(
                                baseUrl,
                                'User',
                                '/scim/Users',
                                'User Account',
                                USER_SCHEMA_URN,
                                USER_EXT_URN
                        ),
                        buildResourceType(
                                baseUrl,
                                'Group',
                                '/scim/Groups',
                                'Group',
                                GROUP_SCHEMA_URN,
                                GROUP_EXT_URN
                        )
                ]
        ]
    }

    // --------------------
    // /docs/scim-auth
    // --------------------
    def scimAuthDocs() {
        def baseUrl = baseUrl()

        // JSON support
        if (request.getHeader('Accept')?.contains('application/json')) {
            render([
                    authentication: "bearer",
                    header        : "Authorization: Bearer <secret_token>",
                    tokenFlow     : "static_credentials",
                    contentType   : "application/scim+json"
            ] as JSON)
            return
        }

        // Load HTML from classpath
        def stream = getClass().getResourceAsStream('/docs/scim-auth.html')
        if (!stream) {
            response.status = 404
            render "SCIM auth documentation not found"
            return
        }

        def html = stream.text.replace('{{BASE_URL}}', baseUrl)
        render(text: html, contentType: 'text/html')
    }

    private Map buildResourceType(
            String baseUrl,
            String id,
            String endpoint,
            String description,
            String schemaUrn,
            String extensionUrn
    ) {
        [
                id              : id,
                name            : id,
                endpoint        : endpoint,
                description     : description,
                schema          : schemaUrn,
                schemaExtensions: [[
                                           schema  : extensionUrn,
                                           required: false
                                   ]],
                meta            : [
                        resourceType: 'ResourceType',
                        location    : "${baseUrl}/scim/ResourceTypes/${id}"
                ]
        ]
    }
}

