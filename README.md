# Grails Scim

Grails Scim is a Grails 3.3.x plugin library for dealing with scim interface integration for user/group resources. It does expose rest endpoint which does consume by SCIM provider to send User/Group onboarding details on the server using SCIM contracts.

## Installation


```groovy
compile 'org.grails.plugins:grails-scim:1.0-M1'
```

## Usage

### Declare and implement beans implementing interface ScimResourceRepositry for User and Group.

resources.groovy
```groovy

    scimUserRepository(ScimUserRepositoryImpl)

    scimGroupRepository(ScimGroupRepositoryImpl)

```

## Contributing
Pull requests are welcome. For major changes, please open an issue first to discuss what you would like to change.

Please make sure to update tests as appropriate.

## License
[MIT](https://choosealicense.com/licenses/mit/)