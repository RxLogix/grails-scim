# Grails Scim

Grails Scim is a Grails 3.3.x plugin library for dealing with scim interface integration for user/group resources. It does expose rest endpoint which does consume by SCIM provider to send User/Group onboarding details on the server using SCIM contracts.

## Installation


```groovy
compile 'org.grails.plugins:grails-scim:1.0-M2'
```

## Usage

### Declare and implement beans implementing interface ScimResourceRepositry for User and Group.

resources.groovy
```groovy

    scimUserRepository(ScimUserRepositoryImpl)

    scimGroupRepository(ScimGroupRepositoryImpl)

```

URLMapping.groovy
```groovy

     group '/scim/v2', {
    
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

## Contributing
Pull requests are welcome. For major changes, please open an issue first to discuss what you would like to change.

Please make sure to update tests as appropriate.

## License
[MIT](https://choosealicense.com/licenses/mit/)