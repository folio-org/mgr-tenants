# mgr-tenants

Tenant registry and lifecycle manager for FOLIO Eureka: tenant CRUD and attributes, one Keycloak realm per tenant, and a pre-deletion check against mgr-tenant-entitlements.

## Commands

```bash
mvn clean package -DskipTests  # build
mvn test                       # unit tests (@UnitTest, *Test.java)
mvn verify                     # unit and integration tests (@IntegrationTest, *IT.java); needs Docker
mvn test -Dtest=TenantServiceTest#getById_positive  # one unit test
mvn verify -Dit.test=TenantIT#getById_positive -Dtest=SkipSurefire -Dsurefire.failIfNoSpecifiedTests=false  # one IT, unit tests skipped
mvn checkstyle:check           # style only
```

Checkstyle also runs at `process-classes`, so every build above fails on a violation, test sources included. The base rules come from `folio-java-checkstyle`; this repository limits methods to 21 lines (`checkstyle/checkstyle-checker.properties`).

## Conventions

- The OpenAPI spec `src/main/resources/swagger.api/mgr-tenants.yaml` generates `org.folio.tm.rest.resource` and `org.folio.tm.domain.dto`. Change the spec, not the generated sources. A new field also needs the entity and a Liquibase changeset under `src/main/resources/changelog/`.
- The module descriptor declares no Kafka events, because the module uses Kafka only through an admin client to purge topics. `ModuleDescriptorTest` fails if the descriptor declares `producesEvents` or `consumesEvents`.
- API gateway self-registration comes from the `folio-integration-kong` and `folio-integration-apisix` libraries in applications-poc-tools, selected by `APIGW_TYPE`. Change registration behavior there.

## Gotchas

- Tenant deletion in `TenantService` runs in this order: entitlements check, `repository.delete`, Keycloak listener (only when `application.keycloak.enabled`), Kafka purge. Keep the check out of `TenantServiceListener` implementations, because listeners run after the delete.
- A failure rolls back only the database. Keycloak realm and Kafka topic changes are not compensated, and the SQL `DELETE` executes at commit, after the realm and topics are already removed.
- The `TenantEntitlementsService` javadoc says "fail-close", but a 404 from mgr-tenant-entitlements counts as "no entitlements" and the deletion proceeds (`checkTenantCanBeDeleted_positive_notFound`). Any other error blocks the deletion with a 400.
- Kafka topics are purged only when the request sets `purgeKafkaTopics=true`. Purge errors are logged and do not fail the deletion.
- Gateway integration tests opt in per class with `@EnableKongGateway` or `@EnableApisixGateway`. The APISIX image is amd64-only: on arm64, rely on emulation or set `TESTCONTAINERS_APISIX_IMAGE`.
