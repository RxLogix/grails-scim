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
implementation 'org.grails.plugins:grails-scim:7.0.0-M4'
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
grails.scim.enabled = true                       // master switch; endpoints return 403 when false
grails.scim.api_token = '<shared secret>'        // legacy static bearer token (single tenant)
grails.scim.legacyStaticToken.enabled = true     // optional; set false to accept tenant tokens only
grails.scim.separator = ','                      // optional; delimiter for the tenants extension value (default ",")
grails.scim.mime.types = ['application/scim+json', 'application/scim+json;charset=utf-8'] // optional override

// Multi-tenant (tenant token): one entry per tenant, value is the SHA-256 (hex or base64) of that tenant's secret
'tenant-001.grails.scim.api_token' = '<sha256 of tenant-001 secret>'
'tenant-002.grails.scim.api_token' = '<sha256 of tenant-002 secret>'

// Multi-tenant (JWT): one raw HMAC secret per tenant and key id (base64, at least 32 bytes), optional issuer
'tenant-001.grails.scim.jwt.kid-2026a' = '<base64 raw secret>'
'tenant-001.grails.scim.jwt.issuer'    = 'https://idp.client-one.example'   // optional, enforced when set
grails.scim.jwt.clockSkewSeconds = 60                                       // optional, default 60
```

Store secrets and hashes in environment variables or external config, never in source control.

### Authentication

Every request must carry `Authorization: Bearer <token>`. Request bodies must use `Content-Type: application/scim+json`. Three token styles are accepted, tried in this order:

**Legacy static token.** The bearer value equals `grails.scim.api_token`. No tenant is derived from the token; the `tenants` / `tenant` custom extension in the payload is passed to your repository exactly as sent. This is the default and matches earlier plugin versions. Disable it with `grails.scim.legacyStaticToken.enabled = false`.

**Tenant JWT.** The bearer value is a standard HS256 JWS whose header carries `tid` (tenant id) and `kid` (key id):

```json
{ "alg": "HS256", "typ": "JWT", "kid": "kid-2026a", "tid": "tenant-001" }
```

The plugin reads `tid` and `kid` from the header, resolves the raw secret from `<tenantId>.grails.scim.jwt.<kid>`, verifies the signature, then validates `exp` (mandatory), `nbf`, `tid` in the payload if present, and `iss` when `<tenantId>.grails.scim.jwt.issuer` is configured. Only `HS256` is accepted; `none` and asymmetric algorithms are rejected before any lookup. The header is covered by the signature, so `tid` is safe to trust once verification succeeds. Multiple `kid` entries per tenant allow rotation. Because the secret must be stored raw, supply it from environment variables or a vault. To resolve secrets elsewhere or add custom behaviour (vault lookup, tenant enable/disable checks, caching, per-tenant issuer rules), declare your own bean named `scimTenantJwtSecretResolver` implementing `grails.plugins.scim.security.ScimTenantJwtSecretResolver` in `resources.groovy`; it replaces the plugin default. Return `null` for unknown or disabled tenants rather than throwing. A worked example is in [docs/PLUGIN_DESIGN.md](docs/PLUGIN_DESIGN.md#101-overriding-scimtenantjwtsecretresolver).

**Tenant token.** The bearer value is `base64url(tenantId).base64url(secret)`. The plugin hashes the presented secret with SHA-256 and compares it, in constant time, with the hash configured under `<tenantId>.grails.scim.api_token`. On success the tenant id is available to your repository via `ScimTenantContext.currentTenantId()`.

For both tenant flows, any tenant values in the payload are ignored and replaced by the authenticated tenant, and PATCH operations targeting the tenant extension are dropped. A missing or mismatched token returns a SCIM-formatted `401 Unauthorized`; unknown and wrong-secret cases produce an identical response.

To onboard a tenant, generate a 32-byte random secret, store its SHA-256 under the tenant key, and hand the client the token. `ScimTenantTokenParser.build(tenantId, secretBytes)` and `ScimTenantTokenParser.sha256Hex(secretBytes)` are provided for administrator tooling.

To resolve hashes from a database or vault instead of config, declare your own bean named `scimTenantKeyResolver` implementing `grails.plugins.scim.security.ScimTenantKeyResolver`. Return `null` for unknown tenants rather than throwing.

Repositories must scope every read and write to `ScimTenantContext.currentTenantId()` when it is non-null, and must throw `ResourceNotFoundException` for ids belonging to another tenant. When it is null the request came through the legacy flow and existing behaviour applies. See [docs/PLUGIN_DESIGN.md](docs/PLUGIN_DESIGN.md) for the plugin architecture with flow diagrams and [docs/MULTI_TENANT_TOKEN_DESIGN.md](docs/MULTI_TENANT_TOKEN_DESIGN.md) for the authentication design decisions.

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