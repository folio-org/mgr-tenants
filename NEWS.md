## Version `v3.0.4` (15.10.2025)
* Add support for custom Keycloak base URL for JWKS endpoint, new ENV variable `KC_JWKS_BASE_URL` (MODSIDECAR-148)
* bump up applications-poc-tools dependencies to 3.0.8
---

## Version `v3.0.3` (25.09.2025)
* Use SECURE\_STORE\_ENV, not ENV, for secure store key (MGRTENANT-64)

---

## Version `v3.0.2` (22.08.2025)
* Ensure that lightweight tokens are enabled for all clients when a new tenant is created (MGRTENANT-62)

---

## Version `v3.0.1` (07.07.2025)
* Enable security by default (MGRTENANT-50)

---

## Version `v3.0.0` (11.03.2025)
* Upgrade Java to version 21. (MGRTENANT-49)
* Entitlement issue for newly created tenant due to default keycloak configurations (MGRTENANT-47)
* Update version of keycloak-admin-client to v26.0.3 (KEYCLOAK-25)

---

## Version `v2.0.0` (01.11.2024)
* bump up applications-poc-tools dependencies to 2.0.0

---

## Version `v1.4.1` (02.10.2024)
* Fix password reset policy name (MGRTENANT-34)
* Enable option in Keycloak making username editable (MGRTENANT-27)
* Increase keycloak-admin-client to v25.0.6 (KEYCLOAK-24)

---

## Version `v1.4.0` (30.09.2024)
* Use folio-auth-openid library for JWT validation (APPPOCTOOL-28)
* Use keycloak client instead of Feign client

---

## Version `v1.3.3` (14.08.2024)
* Implement the ability to designate a tenant as "secure" (MGRTENANT-32)

---

## Version `v1.3.2` (10.07.2024)
* upgrade kong version to 3.7.1 (KONG-10)
* Upgrade keycloak client to v25.0.1 (MGRTENANT-28)

---

## Version `v1.3.1` (20.06.2024)
* Add FIPS compliance check and lost Kong configuration parameters
* Put application to Docker Image and upload to ECR (RANCHER-1515)

---

## Version `v1.3.0` (25.05.2024)
* Add Dockerfile that is based on the FIPS-140-2 compliant base image (MGRTENANT-19)
* Secure mgr-tenants HTTP endpoints with SSL (MGRTENANT-18)
* Keycloak client: add support TLS certificates issued by trusted certificate authorities (MGRTENANT-26)

---

## Version `v1.2.0` (16.04.2024)
* update keycloak related tests
* Kong timeouts should be extended (KONG-6)
* support TLS for keycloak clients (MGRTENANT-12)

---

## Version `v1.1.0` (28.02.2024)
### Features
* Self-register routes in Kong (MGRAPPS-2)
* Upgrade to Keycloak 23.0.6 (KEYCLOAK-6)
* Implement router prefix for the generated endpoints (MGRAPPS-8)

### Dependencies
* Add `org.folio:folio-integration-kong` `1.3.0`
* Add `org.folio:folio-backend-common` `1.3.0`
* Bump `org.springframework.boot:spring-boot-starter-parent` from `3.2.1` to `3.2.3`
* Bump `org.openapitools:openapi-generator-maven-plugin` from `6.4.0` to `7.3.0`
* Bump `org.folio:folio-spring-cql` from `7.2.2` to `8.0.0`
* Bump `com.puppycrawl.tools:checkstyle` from `10.12.7` to `10.13.0`
* Bump `org.folio:cql2pgjson` from `35.0.4` to `35.1.2`
* Bump `org.testcontainers:testcontainers-bom` from `1.19.3` to `1.19.6`
* Bump `io.hypersistence:hypersistence-utils-hibernate-63` from `3.7.0` to `3.7.3`
* Bump `org.folio:folio-security` from `1.0.0` to `1.3.0`
* Bump `org.folio:folio-integration-kafka` from `1.0.0` to `1.3.0`
* Bump `org.folio:folio-backend-testing` from `1.0.0` to `1.3.0`
* Bump `io.swagger.core.v3:swagger-annotations` from `2.2.8` to `2.2.20`

---

## Version `v1.0.0` (22.01.2024)
### New APIs versions
* Provides `tenants v1.0`
* Provides `tenant-attributes v1.0`

### Features
* Upgrade Spring Boot to v3.2.1 (MODROLESKC-21)
* Upgrade to Keycloak 23.0.6 (KEYCLOAK-6)

### Tech Dept
* Fix maven deploy/build issues
* Add dependabot to track and upgrade project dependencies
* Use alpine version for postgres docker container in integration tests

### Dependencies
* Remove `io.hypersistence:hypersistence-utils-hibernate-60`
* Add `io.hypersistence:hypersistence-utils-hibernate-63` `3.7.0`
* Bump `org.projectlombok:lombok` from `1.18.26` to `1.18.30`
* Bump `org.springframework.boot:spring-boot-starter-parent` from `3.0.4` to `3.2.1`
* Bump `org.apache.commons:commons-lang3` from `3.12.0` to `3.14.0`
* Bump `org.mapstruct:mapstruct` from `1.5.3.Final` to `1.5.5.Final`
* Bump `org.mapstruct:mapstruct-processor` from `1.5.3.Final` to `1.5.5.Final`
* Bump `com.puppycrawl.tools:checkstyle` from `10.12.3` to `10.12.7`
* Bump `org.folio:folio-spring-cql` from `7.0.0` to `7.2.2`
