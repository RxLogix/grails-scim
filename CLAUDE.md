# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

`grails-scim` is a Grails 7 **plugin** (not an application) that exposes SCIM 2.0 REST endpoints for User and Group provisioning. Host applications depend on the published jar, implement two `ScimResourceRepository` beans, enable it via config, and add URL mappings (see README.md for the exact mapping block and `docs/API_USAGE.md` for the wire contract). The plugin itself has no persistence; all storage is delegated to the host's repository beans.

Stack: Grails 7.0.16 / Spring Boot 3.5 / Groovy 4 / Java 17 / Gradle 8.14 wrapper / Spock + JUnit Platform. Versions live in `gradle.properties`; `build.gradle` pulls everything else from `grails-bom` (no explicit versions — keep it that way, see `settings.gradle`/BOM usage).

## Commands

Java 17 is required. If `JAVA_HOME` is not already Java 17, pass `-Dorg.gradle.java.home=<jdk17 path>` to gradlew (the local SDKMAN Zulu 17 path has been used before).

```bash
./gradlew test                                              # all Spock specs
./gradlew test --tests 'grails.plugins.scim.ScimUserControllerSpec'          # one spec
./gradlew test --tests 'grails.plugins.scim.ScimUserControllerSpec."index should return list response"'  # one feature method
./gradlew compileGroovy                                     # quick compile check
./gradlew assemble                                          # builds build/libs/grails-scim-<version>.jar
./gradlew publish                                           # publishes to Nexus; needs nexusUrl/nexusUsername/nexusPassword props or NEXUS_* env vars
```

Test reports: `build/reports/tests/test/index.html`. There is no lint task configured. `bootRun` and `bootJar` are disabled (plugin, not runnable).

Build quirks to be aware of:
- `groovyOptions.optimizationOptions.indy = false` is set deliberately (grails-core issue 15321). Don't remove it.
- `byte-buddy` / `byte-buddy-agent` are explicit test deps because Spock needs them to mock classes and they stopped arriving transitively after the Grails 7 upgrade.
- Several vulnerable transitive modules are excluded globally in `configurations.all`; new deps that re-introduce them will silently be stripped.
- `generateGitProperties` always runs and feeds `Git-Commit`/`Git-Branch` into the jar manifest, so builds require a `.git` directory.

## Architecture

Request flow for every SCIM call:

1. **`ScimControllerInterceptor`** (`grails-app/controllers/.../ScimControllerInterceptor.groovy`) matches the `scimHome|scimUser|scimGroup` controllers. It returns 403 if `grails.scim.enabled` is false, otherwise compares the `Authorization: Bearer <token>` value against `grails.scim.api_token` and returns a SCIM-formatted 401 on mismatch. This is the only auth in the plugin — a single static shared secret.
2. **Data binding**: `GrailsScimGrailsPlugin.doWithSpring()` (only when enabled) registers `JsonScimApiDataBindingSourceCreator`, which teaches Grails to parse `application/scim+json` bodies as JSON (`request.JSON`). `DataBindingSourceRegistryUpdater` adds it to the registry on `ContextRefreshedEvent` because the registry is built before plugin beans exist. Mime types are overridable via `grails.scim.mime.types`.
3. **Controllers** (`ScimUserController`, `ScimGroupController`) bind the body into POJOs in `src/main/groovy/.../resources` (`ScimUser`, `ScimGroup`, `PatchRequest`), call the matching service, and map the plugin's exceptions (`src/main/groovy/.../exceptions`) to HTTP status + `ErrorResponse`: `InvalidRequestDataException`→400, `ResourceNotFoundException`→404, `ResourceConflictException`→409, `UnsupportedActionException`→501, anything else→500. PUT enforces body `id` == path `id`. Everything renders as `application/scim+json` via a private `renderScim` helper.
4. **Services** (`ScimUserService`, `ScimGroupService`) are thin `@ReadOnly` pass-throughs to the host-provided `scimUserRepository` / `scimGroupRepository` beans (`ScimResourceRepository<T>` interface). Their one piece of logic is `getExcludedProperties`, which converts the SCIM `attributes` include-list into an `excludedAttributes` list by reflecting over the resource class's declared fields.
5. **`ScimHomeController`** serves discovery endpoints: `/Schemas` (from `src/main/resources/schemas.json`), `/ResourceTypes`, `/ServiceProviderConfig`, and `/docs/scim-auth` (HTML from `src/main/resources/docs/scim-auth.html` with `{{BASE_URL}}` substitution). Base URL honours `X-Forwarded-Proto/Host`.

**Serialization**: `grails-app/init/grails/scim/BootStrap.groovy` registers a custom `JSON` object marshaller for `ScimUser` and `ScimGroup`. It drops null fields and `customExtension`, and re-emits the extension under its URN key (`urn:ietf:params:scim:schemas:extension:custom:2.0:User` / `:Group`). The User extension carries `tenants` as a delimited string (separator from `grails.scim.separator`, default `,`); controllers split it back on the way in. `ScimUser.getSchemas()` appends the extension URN only when tenants are present. Any new resource field must be a non-synthetic declared field to be serialized.

**Note on `PatchRequest.Operations` / `ListResponse.Resources`**: these are deliberately capitalised public fields to match the SCIM wire format.

## Configuration keys (host app)

| Key | Purpose |
|---|---|
| `grails.scim.enabled` | Master switch; gates bean registration and the interceptor |
| `grails.scim.api_token` | Static bearer token compared by the interceptor |
| `grails.scim.separator` | Delimiter for the `tenants` extension string (default `,`) |
| `grails.scim.mime.types` | Optional list overriding the SCIM JSON mime types |

## Testing conventions

Specs live in `src/test/groovy/grails/plugins/scim/` and use `ControllerUnitTest<...>` / `InterceptorUnitTest<...>` from grails-testing-support. Services are replaced with Spock `Mock(...)` and assigned directly on `controller.<service>`. Response bodies are asserted by parsing `response.text` with `JsonSlurper`, and status via `response.status`. Follow this pattern rather than spinning up integration tests; there is no GORM or database in this plugin.

## Repo conventions

- Branches are named `task/PVR-<ticket>` and commit messages start with the ticket id (e.g. `PVR-106322 ...`). PRs target `master` and must complete `.github/PULL_REQUEST_TEMPLATE.md` (RxLogix GDL-005 naming, explicit types over `def`, logging, tests).
- Bump `version` in `gradle.properties` and add an entry to `changelog.md` for releases.
- `docs/API_USAGE.md` is the client-facing contract; update it when endpoints or payload shapes change. README's Grails version text is stale (says 6.2.0) — the plugin now targets Grails 7.
