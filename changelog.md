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
- M3: Multi-tenant SCIM via per-tenant bearer tokens (`base64url(tenantId).base64url(secret)`) verified against `<tenantId>.grails.scim.api_token` SHA-256 hashes. Authenticated tenant is exposed through `ScimTenantContext` and overrides payload tenant fields. Legacy static token unchanged and still default; `grails.scim.legacyStaticToken.enabled=false` disables it. Interceptor now requires the `Bearer` scheme, uses constant-time comparison, and no longer logs the Authorization header on failure.
