# mgr-tenants

Central Tenant Management Service for FOLIO — tenant registry and lifecycle manager in a multi-tenant environment. Java 21, Spring Boot 3.5.7, PostgreSQL/Liquibase.

Responsibilities: tenant CRUD (unique names, immutable fields), key-value tenant attributes, multi-system sync (Keycloak/Okapi/Kafka via event listeners), pre-deletion validation (blocks deletion when active entitlements exist).

## Build & Test

```bash
mvn clean package              # build fat jar
mvn clean package -DskipTests  # skip tests
mvn test                       # unit tests (@UnitTest, *Test.java)
mvn verify                     # integration tests (@IntegrationTest, *IT.java)
mvn test -Dtest=TenantServiceTest#getById_positive    # single unit test
mvn verify -Dit.test=TenantIT#getById_positive        # single IT
mvn checkstyle:check           # auto-runs at process-classes; max method length 21 lines
```

## Architecture

Layers: Controllers (implement OpenAPI interfaces) → Services → Repositories → Entities; integrations react via listeners.

**Domain** (`org.folio.tm`): `Tenant` (id, immutable name, description, type DEFAULT/VIRTUAL, secure flag), `TenantAttribute` (key-value metadata). Entities extend `Auditable`; use `@JdbcTypeCode(SqlTypes.NAMED_ENUM)` for PG enums.

**Integrations** (all implement `TenantServiceListener`, react to create/update/delete):
| Integration | Purpose | Toggle (default) |
|---|---|---|
| Keycloak (`integration/keycloak/`) | realm + OAuth2 clients (login, impersonation, password-reset, M2M) per tenant | `KC_INTEGRATION_ENABLED` (true) |
| Okapi (`integration/okapi/`) | sync tenant descriptors with legacy gateway | `OKAPI_INTEGRATION_ENABLED` (false) |
| Kong | gateway routing | `KONG_INTEGRATION_ENABLED` (true) |
| Kafka (`integration/kafka/`) | purge `{env}.{tenant}.*` topics on deletion | — |
| Entitlements (`integration/entitlements/`) | validate no active entitlements before deletion | — |

**Patterns**:
- Event-driven listeners: `TenantService` (CRUD) → `TenantEventsPublisher` broadcasts to all `TenantServiceListener`s; listeners react independently.
- Conditional beans via `@ConditionalOnProperty`.
- Feign clients (`KeycloakClient`, `OkapiClient`, `TenantEntitlementsClient`).
- **Fail-close security**: if `mgr-tenant-entitlements` is unreachable or errors → BLOCK deletion; only allow when it explicitly confirms no entitlements.

## Conventions

- **Codegen**: spec `src/main/resources/swagger.api/mgr-tenants.yaml` → `org.folio.tm.rest.resource` + `.domain.dto` (do not edit). Add field: edit spec → `mvn clean compile` → update entity/Liquibase/mapper.
- **DB**: Liquibase under `src/main/resources/changelog/`.
- **Naming**: `*Entity` entities, `*Repository`, `*Service`, `*Controller`, `*Mapper`, `*Test` (unit), `*IT` (integration), `*Exception`. Test methods: `methodName_outcome_condition` (e.g. `createTenant_negative_nameExists`).
- **Lombok**: `@Data`, `@RequiredArgsConstructor`, `@Log4j2`; `@ToString.Exclude`/`@EqualsAndHashCode.Exclude` on entity collections.
- **MapStruct**: `@Mapper(componentModel="spring", injectionStrategy=CONSTRUCTOR, uses=MappingMethods.class)`.
- **Exceptions** (handled centrally in `ApiExceptionHandler`): `RequestValidationException` (400), `EntityNotFoundException` (404), `KeycloakException`, `OkapiRequestException`.
- **Tests**: unit = Mockito (`@InjectMocks`/`@Mock`, `TestUtils.verifyNoMoreInteractions(this)` in `@AfterEach`); integration = MockMvc + WireMock, extend `BaseIntegrationTest`, `@Sql` setup, `@WireMockStub` mocks.

## Layout

```
src/main/java/org/folio/tm/{controller,service,repository,domain/entity,domain/dto,mapper,integration,exception}
src/test/java/org/folio/tm/{it,service,support}
src/test/resources/{sql,wiremock,json}
```
Env vars: see `README.md`.
