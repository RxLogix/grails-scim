# Changelog

All notable changes to this project will be documented in this file.

## 1.0
- Initial version

## 2.0.0
- [Merge Grails 6 upgrade](https://github.com/RxLogix/grails-scim/pull/4)

## 3.0.0
- Add Tenants custom support to scim and also refactor code.

## 7.0.0
- Upgrade plugin to Grails 7.0.16 (Spring Boot 3.5.16, Java 17, Jakarta EE).
- M2: Fix JSON marshalling failure (`Error converting Bean with class grails.validation.ConstrainedDelegate`) when rendering validated User/Group resources; Validateable trait fields and static constants are no longer serialized.
- M3 (PVADMIN-50831): Multi-tenant SCIM authentication.
  - New host extension point `ScimTenantConfigProvider` (bean `scimTenantConfigProvider`). When present, requests are authenticated with the per-tenant HS256 JWT minted by New Admin (`tenantId` claim, verified with the tenant's `grails.scim.jwt.secret`); a non-JWT bearer is accepted as the legacy token of the default tenant.
  - New `grails.scim.legacyToken.enabled` (default `true`) switches the legacy token path off in multi-tenant mode.
  - `ScimTenantContext.currentTenantId()` exposes the verified tenant to the host's repositories.
  - Without the provider bean the previous static-token behaviour is kept (`grails.scim.enabled` + `grails.scim.api_token`), now compared in constant time.
  - The `Authorization` header must use the `Bearer` scheme (case-insensitive) in both modes; previously any scheme was accepted and only the last space-separated word was compared. A missing token always returns `401`.
  - 403 responses are now SCIM error JSON (previously plain text); the bearer token is no longer logged on failed authentication.
  - The `application/scim+json` binding beans are registered regardless of `grails.scim.enabled`.
  - New dependency: `io.jsonwebtoken:jjwt-api` / `jjwt-impl` / `jjwt-jackson` 0.12.6.
