# Grails Scim

Grails Scim is a Grails 7 plugin library for SCIM 2.0 interface integration for User and Group resources. It exposes REST endpoints that a SCIM identity provider calls to provision, update, and deprovision users and groups using standard SCIM contracts.

The plugin has no persistence of its own. The host application implements two `ScimResourceRepository` beans and the plugin delegates all storage to them.

## Requirements

| Plugin version | Grails |
|---|---|
| 7.x | 7.0.x (Java 17+, Spring Boot 3.5) |
| 3.x | 6.x |
| 1.x | 3.x |

## Installation

Grails 7.x:
```groovy
implementation 'org.grails.plugins:grails-scim:7.0.0-M2'
```

Grails 3.x:
```groovy
compile 'org.grails.plugins:grails-scim:1.0-M3'
```

Grails 6.x:
```groovy
implementation 'org.grails.plugins:grails-scim:3.0-M1'
```

## Usage

### Declare and implement beans implementing interface ScimResourceRepository for User and Group.

resources.groovy
```groovy

    scimUserRepository(ScimUserRepositoryImpl)

    scimGroupRepository(ScimGroupRepositoryImpl)

```

### Configuration

application.groovy
```groovy
grails.scim.enabled = true                  // master switch; endpoints return 403 when false
grails.scim.api_token = '<shared secret>'   // static bearer token expected in the Authorization header
grails.scim.separator = ','                 // optional; delimiter for the tenants extension value (default ",")
grails.scim.mime.types = ['application/scim+json', 'application/scim+json;charset=utf-8'] // optional override
```

Store the API token in an environment variable or external config, never in source control.

### Authentication

Every request must carry `Authorization: Bearer <api_token>`. A missing or mismatched token returns a SCIM-formatted `401 Unauthorized`. Request bodies must use `Content-Type: application/scim+json`.

### URL mappings


URLMapping.groovy
```groovy

     group '/scim/v2', {
    
                '/docs/scim-auth'(controller: 'scimHome', action: 'scimAuthDocs')
         
                '/Schemas'(controller: 'scimHome', action: 'schemas')
         
                "/Schemas/$id"(controller: 'scimHome', action: 'schemas')
    
                '/ResourceTypes'(controller: 'scimHome', action: 'resourceTypes')
                "/ResourceTypes/$id"(controller: 'scimHome', action: 'resourceTypes')
    
                '/ServiceProviderConfig'(controller: 'scimHome', action: 'serviceProviderConfig')
                            
                 '/ServiceConfiguration'(controller: 'scimHome', action: 'serviceProviderConfig') 
    
                '/Users'(controller: 'scimUser') {
                    action = [GET: 'index', POST: 'save']
                }
                "/Users/$id"(controller: 'scimUser') {
                    action = [GET: 'show', DELETE: 'delete', PATCH: 'patch', PUT: 'update', POST: 'patch']
                }
    
                '/Groups'(controller: 'scimGroup') {
                    action = [GET: 'index', POST: 'save']
                }
                "/Groups/$id"(controller: 'scimGroup') {
                    action = [GET: 'show', DELETE: 'delete', PATCH: 'patch', PUT: 'update', POST: 'patch']
                }
    
            }

```

## Endpoints

| Path | Description |
|---|---|
| `/scim/v2/Users`, `/scim/v2/Users/{id}` | User CRUD, search (`filter`, `count`, `startIndex`, `attributes`, `excludedAttributes`) and PATCH |
| `/scim/v2/Groups`, `/scim/v2/Groups/{id}` | Group CRUD, search and PATCH |
| `/scim/v2/Schemas`, `/scim/v2/Schemas/{id}` | SCIM schema discovery |
| `/scim/v2/ResourceTypes`, `/scim/v2/ResourceTypes/{id}` | Resource type discovery |
| `/scim/v2/ServiceProviderConfig` | Service provider capabilities |
| `/scim/v2/docs/scim-auth` | Authentication documentation (HTML, or JSON with `Accept: application/json`) |

Full request and response examples, including the custom tenants extension
(`urn:ietf:params:scim:schemas:extension:custom:2.0:User`), are in [docs/API_USAGE.md](docs/API_USAGE.md).

## Error Handling

All endpoints return SCIM-compliant error responses with `application/scim+json` content type.

| HTTP Status | Condition |
|-------------|-----------|
| 400 Bad Request | Invalid request data, or the `id` in the request body does not match the `id` in the URI for PUT requests |
| 404 Not Found | Resource does not exist |
| 409 Conflict | Resource already exists (POST/save) |
| 500 Internal Server Error | Unexpected server error |
| 501 Not Implemented | Operation not supported (User delete only) |

> **Note:** PUT `/Users/$id` and PUT `/Groups/$id` validate that the `id` field in the JSON body matches the `$id` path parameter. A mismatch returns `400 Bad Request`.

## Development

Requires Java 17.

```bash
./gradlew test        # run Spock specs
./gradlew assemble    # build build/libs/grails-scim-<version>.jar
./gradlew publish     # publish to Nexus (needs nexusUrl/nexusUsername/nexusPassword or NEXUS_* env vars)
```

## Contributing
Pull requests are welcome. For major changes, please open an issue first to discuss what you would like to change.

Please make sure to update tests, `changelog.md`, and `docs/API_USAGE.md` as appropriate.

## License
[MIT](https://choosealicense.com/licenses/mit/)