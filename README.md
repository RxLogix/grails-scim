# Grails Scim

Grails Scim is a Grails 6.2.0 plugin library for dealing with scim interface integration for user/group resources. It does expose rest endpoint which does consume by SCIM provider to send User/Group onboarding details on the server using SCIM contracts.

## Installation

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

##### Enable via configuration file.
application.groovy
```groovy
grails.scim.enabled = true
```


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

## Contributing
Pull requests are welcome. For major changes, please open an issue first to discuss what you would like to change.

Please make sure to update tests as appropriate.

## License
[MIT](https://choosealicense.com/licenses/mit/)