# mgr-tenants

Central Tenant Management Service for FOLIO: tenant registry and lifecycle manager in a multi-tenant environment. Java 21, Spring Boot, PostgreSQL/Liquibase.

Responsibilities: tenant CRUD (unique names, immutable fields), key-value tenant attributes, Keycloak realm sync via event listeners, optional Kafka topic purge on deletion, pre-deletion validation (blocks deletion when active entitlements exist).

## Build & Test

```bash
mvn clean package              # build fat jar
mvn clean package -DskipTests  # skip tests
mvn test                       # unit tests (@UnitTest, *Test.java)
mvn verify                     # integration tests (@IntegrationTest, *IT.java)
mvn test -Dtest=TenantServiceTest#getById_positive    # single unit test
mvn verify -Dit.test=TenantIT#getById_positive -Dtest=SkipSurefire -Dsurefire.failIfNoSpecifiedTests=false  # single IT, skips unit tests
mvn checkstyle:check           # auto-runs at process-classes
```

## Style

Match the repository's existing style: use maintained implementations and tests as templates for new code, and keep the structure, naming, and conventions of neighboring files. The checkstyle configuration in `checkstyle/` (`checkstyle-checker.properties`, suppressions) is the authority for style limits.

## Pitfalls

- Only `KeycloakTenantListener` implements `TenantServiceListener`. The entitlements check and the Kafka purge are direct calls in `TenantService`. **Ordering constraint:** the entitlements check runs *before* the delete, listeners run *after* it; do not move the check into a listener. Listeners run sequentially inside the transaction; a failing listener rolls back the change.
- A 404 from `mgr-tenant-entitlements` counts as "no entitlements" and lets the deletion proceed; any other failure blocks the deletion with a 400 (`TenantEntitlementsService`, pinned by `checkTenantCanBeDeleted_positive_notFound`).
- Kafka topics are purged on deletion only when `purgeKafkaTopics=true` (no purge by default). Kafka is admin-client only: no events are produced or consumed, and `ModuleDescriptorTest` guards that the module descriptor declares none.
- The API Gateway integration comes from libraries (`folio-integration-kong` / `folio-integration-apisix`, selected by `APIGW_TYPE`); there is no gateway module code in this repository.
- HTTP clients are `@HttpExchange` interfaces built with `HttpClientTlsUtils.buildHttpServiceClient`; no Feign in this module.
- Generated code: the spec `src/main/resources/swagger.api/mgr-tenants.yaml` (field schemas in `schemas/`) generates `org.folio.tm.rest.resource` and `.domain.dto`: do not edit generated sources; change the spec and regenerate.
- Gateway ITs opt in per class: `@EnableKongGateway` or `@EnableApisixGateway` (the APISIX image is amd64-only; on arm64 rely on emulation or set `TESTCONTAINERS_APISIX_IMAGE`).

Env vars: see `README.md`.
