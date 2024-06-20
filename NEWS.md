## v1.3.2 In Progress

## v1.3.1 2024-06-20

* Add FIPS compliance check and lost Kong configuration parameters (#66)
* Put application to Docker Image and upload to ECR (#69) ([RANCHER-1515](https://issues.folio.org/browse/RANCHER-1515))

---

## v1.3.0 2024-05-25

* Add Dockerfile that is based on the FIPS-140-2 compliant base image (#63) ([MGRTENANT-19](https://issues.folio.org/browse/MGRTENANT-19))
* Secure mgr-tenants HTTP endpoints with SSL (#62) ([MGRTENANT-18](https://issues.folio.org/browse/MGRTENANT-18))
* Keycloak client: add support TLS certificates issued by trusted certificate authorities (#60) ([MGRTENANT-26](https://issues.folio.org/browse/MGRTENANT-26))
---

## v1.2.0 2024-04-16

* update keycloak related tests (#50)
* ([KONG-6](https://issues.folio.org/browse/KONG-6)): Kong timeouts should be extended (#48)
* ([MGRTENANT-12](https://issues.folio.org/browse/MGRTENANT-12)): support TLS for keycloak clients (#46)

---

## v1.1.0 2024-02-28
### Features
* Self-register routes in Kong ([MGRAPPS-2](https://issues.folio.org/browse/MGRAPPS-2))
* Upgrade to Keycloak 23.0.6 ([KEYCLOAK-6](https://issues.folio.org/browse/KEYCLOAK-6))
* Implement router prefix for the generated endpoints ([MGRAPPS-8](https://issues.folio.org/browse/MGRAPPS-8))

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

## v1.0.0 2024-01-22
### New APIs versions
* Provides `tenants v1.0`
* Provides `tenant-attributes v1.0`

### Features
* Upgrade Spring Boot to v3.2.1 (#4) ([MODROLESKC-21](https://issues.folio.org/browse/MODROLESKC-21))
* Upgrade to Keycloak 23.0.6 ([KEYCLOAK-6](https://issues.folio.org/browse/KEYCLOAK-6))

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

