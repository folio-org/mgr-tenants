# mgr-tenants

Central Tenant Management Service for FOLIO — tenant registry and lifecycle manager in a multi-tenant environment. Java, Spring Boot, PostgreSQL/Liquibase, Keycloak, Kafka (admin client for topic purge), Kong or APISIX.

Responsibilities: tenant CRUD (unique names, immutable name and `secure` flag), key-value tenant attributes, one Keycloak realm per tenant (via a listener), Kafka topic purge on deletion, pre-deletion check against mgr-tenant-entitlements.

## Build & Test

```bash
mvn clean package              # build fat jar
mvn clean package -DskipTests  # skip tests
mvn test                       # unit tests (@UnitTest, *Test.java)
mvn verify                     # unit and integration tests (@IntegrationTest, *IT.java); needs Docker
mvn test -Dtest=TenantServiceTest#getById_positive    # single unit test
mvn verify -Dit.test=TenantIT#getById_positive -Dtest=SkipSurefire -Dsurefire.failIfNoSpecifiedTests=false  # single IT
mvn checkstyle:check           # auto-runs at process-classes; max method length 21 lines
```

## Architecture

Layers: Controllers (implement OpenAPI interfaces) → Services → Repositories → Entities; Keycloak reacts via a listener.

**Domain** (`org.folio.tm`): `Tenant` (id, immutable name, description, type DEFAULT/VIRTUAL, secure flag), `TenantAttribute` (key-value metadata). Entities extend `Auditable`; use `@JdbcTypeCode(SqlTypes.NAMED_ENUM)` for PG enums.

**Integrations** (only Keycloak implements `TenantServiceListener`; `TenantService` calls the entitlements check and the Kafka purge directly):
| Integration | Purpose | Toggle (default) |
|---|---|---|
| Keycloak (`integration/keycloak/`) | realm + OAuth2 clients (login, impersonation, password-reset, M2M) per tenant | `KC_INTEGRATION_ENABLED` (true) |
| API Gateway (Kong / APISIX) | module self-registration via `folio-integration-kong` or `folio-integration-apisix` library auto-configuration, selected by `APIGW_TYPE` (`kong` default, `apisix` — requires `APIGW_API_KEY`); not a `TenantServiceListener` — no module code here | `APIGW_ENABLED` (true, deprecated: `KONG_INTEGRATION_ENABLED`) |
| Kafka (`integration/kafka/`) | purge `{env}.{tenant}.*` topics on deletion; admin client only — no events produced or consumed, so the module descriptor declares none (`ModuleDescriptorTest` guards this) | — |
| Entitlements (`integration/entitlements/`) | validate no active entitlements before deletion | — |

**Patterns**:
- Event-driven listeners: `TenantService` (CRUD) → `TenantEventsPublisher` broadcasts to all `TenantServiceListener`s; listeners react independently.
- Conditional beans via `@ConditionalOnProperty`.
- HTTP clients are `@HttpExchange` interfaces (`KeycloakClient`, `TenantEntitlementsClient`).

## Conventions

- **Codegen**: spec `src/main/resources/swagger.api/mgr-tenants.yaml` → `org.folio.tm.rest.resource` + `.domain.dto` (do not edit). Add field: edit spec → `mvn clean compile` → update entity/Liquibase/mapper.
- **DB**: Liquibase under `src/main/resources/changelog/`.
- **Naming**: `*Entity` entities, `*Repository`, `*Service`, `*Controller`, `*Mapper`, `*Test` (unit), `*IT` (integration), `*Exception`. Test methods: `methodName_outcome_condition` (e.g. `createTenant_negative_nameExists`).
- **Lombok**: `@Data`, `@RequiredArgsConstructor`, `@Log4j2`; `@ToString.Exclude`/`@EqualsAndHashCode.Exclude` on entity collections.
- **MapStruct**: `@Mapper(componentModel="spring", injectionStrategy=CONSTRUCTOR, uses=MappingMethods.class)`.
- **Exceptions** (handled centrally in `ApiExceptionHandler`): `RequestValidationException` (400), `EntityNotFoundException` (404), `KeycloakException`.
- **Tests**: unit = Mockito (`@InjectMocks`/`@Mock`, `TestUtils.verifyNoMoreInteractions(this)` in `@AfterEach`); integration = MockMvc + WireMock, extend `BaseIntegrationTest`, `@Sql` setup, `@WireMockStub` mocks. Gateway ITs opt in per class: `@EnableKongGateway` (`KongRegistrationIT`) or `@EnableApisixGateway` (`ApisixRegistrationIT`; etcd + `folioci/folio-apisix:latest`, amd64-only — on arm64 rely on emulation or set `TESTCONTAINERS_APISIX_IMAGE`).

## Layout

```
src/main/java/org/folio/tm/{controller,service,repository,domain/entity,domain/dto,mapper,integration,exception}
src/test/java/org/folio/tm/{it,service,support}
src/test/resources/{sql,wiremock,json}
```
Env vars: see `README.md`.
