# AGENTS.md - Coding Agent Guidelines for mgr-tenants

## Project Overview

**mgr-tenants** is the central **Tenant Management Service** for the FOLIO library services platform. It serves as the tenant registry and lifecycle manager in a multi-tenant FOLIO environment.

**Tech Stack**: Java 21 / Spring Boot 3.5.7 / Maven / PostgreSQL / Liquibase

### Core Responsibilities

- **Tenant CRUD**: Create, read, update, delete tenants with validation (unique names, immutable fields)
- **Tenant Attributes**: Key-value metadata storage for tenants
- **Multi-System Sync**: Synchronizes tenant lifecycle across Keycloak, Okapi, and Kafka via event-driven listeners
- **Pre-deletion Validation**: Blocks deletion if tenant has active application entitlements

### Domain Model

| Entity | Description |
|--------|-------------|
| `Tenant` | Core entity with id, name (immutable), description, type (DEFAULT/VIRTUAL), secure flag |
| `TenantAttribute` | Key-value pairs for custom tenant metadata |

### External Integrations

| Integration | Purpose | Location |
|-------------|---------|----------|
| **Keycloak** | Creates realm + OAuth2 clients (login, impersonation, password-reset, M2M) per tenant | `integration/keycloak/` |
| **Okapi** | Syncs tenant descriptors with FOLIO API gateway | `integration/okapi/` |
| **Kafka** | Purges tenant-specific topics on deletion (`{env}.{tenant}.*`) | `integration/kafka/` |
| **Entitlements** | Validates no active app entitlements before tenant deletion | `integration/entitlements/` |

All integrations implement `TenantServiceListener` interface and react to tenant create/update/delete events.

## Build & Test Commands

### Building
```bash
mvn clean compile              # Compile only
mvn clean package              # Build fat jar
mvn clean package -DskipTests  # Skip all tests
```

### Testing

**Unit tests** (tagged with `@UnitTest` / `@Tag("unit")`):
```bash
mvn test                                    # Run all unit tests
mvn test -Dtest=TenantServiceTest           # Single unit test class
mvn test -Dtest=TenantServiceTest#getById_positive  # Single test method
```

**Integration tests** (tagged with `@IntegrationTest` / `@Tag("integration")`):
```bash
mvn verify                                  # Run integration tests
mvn verify -Dit.test=TenantIT               # Single integration test class
mvn verify -Dit.test=TenantIT#getById_positive      # Single IT method
```

**All tests**:
```bash
mvn clean verify               # Unit + integration tests
```

### Code Quality
```bash
mvn checkstyle:check           # Run checkstyle (auto-runs during build)
```

## Code Style Guidelines

### Checkstyle Rules
- Uses `folio-java-checkstyle` library with configuration at `checkstyle/checkstyle-checker.properties`
- **Method length limit**: 21 lines (suppressed for test files)
- Checkstyle runs automatically during `process-classes` phase and fails on violations

### Import Order
Standard Java import organization:
1. Static imports (grouped)
2. `java.*` and `javax.*`
3. Third-party libraries (Spring, Lombok, etc.)
4. Project imports (`org.folio.*`)

Always use static imports for:
- Test assertions: `import static org.assertj.core.api.Assertions.assertThat`
- Mockito: `import static org.mockito.Mockito.*`
- Constants: `import static org.folio.tm.support.TestConstants.*`

### Naming Conventions

| Type | Convention | Example |
|------|------------|---------|
| Entities | Suffix `Entity` | `TenantEntity`, `TenantAttributeEntity` |
| DTOs | No suffix (generated) | `Tenant`, `TenantAttributes` |
| Repositories | Suffix `Repository` | `TenantRepository` |
| Services | Suffix `Service` | `TenantService`, `KeycloakRealmService` |
| Mappers | Suffix `Mapper` | `TenantMapper`, `TenantAttributeMapper` |
| Controllers | Suffix `Controller` | `TenantController` |
| Configuration | Suffix `Configuration` or `Properties` | `KeycloakConfiguration` |
| Unit Tests | Suffix `Test` | `TenantServiceTest` |
| Integration Tests | Suffix `IT` | `TenantIT`, `TenantKeycloakIT` |
| Exceptions | Suffix `Exception` | `RequestValidationException` |

### Class Annotations Pattern

**Services**:
```java
@Log4j2
@Service
@RequiredArgsConstructor
@Transactional
public class TenantService { ... }
```

**Entities**:
```java
@Data
@Entity
@Table(name = "tenant")
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class TenantEntity extends Auditable implements Identifiable { ... }
```

**Unit Tests**:
```java
@UnitTest
@ExtendWith(MockitoExtension.class)
class TenantServiceTest { ... }
```

**Integration Tests**:
```java
@IntegrationTest
@SqlMergeMode(MERGE)
@Sql(scripts = "classpath:/sql/clear_tenants.sql", executionPhase = AFTER_TEST_METHOD)
class TenantIT extends BaseIntegrationTest { ... }
```

### Error Handling

Use domain-specific exceptions:
- `RequestValidationException` - for validation errors (returns 400)
- `EntityNotFoundException` - for missing resources (returns 404)
- `KeycloakException` - for Keycloak integration errors
- `OkapiRequestException` - for Okapi integration errors

All exceptions are handled centrally in `ApiExceptionHandler`.

```java
// Throwing validation error with parameters
throw new RequestValidationException("Tenant id doesn't match", "id", tenantId);

// Simple validation error
throw new RequestValidationException("Tenant's name already taken: " + name);

// Not found
throw new EntityNotFoundException("Tenant is not found: id = " + id);
```

### Lombok Usage

Standard annotations:
- `@Data` for entities and DTOs
- `@RequiredArgsConstructor` for dependency injection
- `@Log4j2` for logging
- `@ToString.Exclude` and `@EqualsAndHashCode.Exclude` for collections in entities

### MapStruct Mappers

```java
@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR, uses = MappingMethods.class)
public interface TenantMapper {
    @AuditableMapping
    Tenant toDto(TenantEntity entity);
}
```

### Testing Patterns

**Unit tests** - Use Mockito with `@UnitTest`, `@ExtendWith(MockitoExtension.class)`:
- Use `@InjectMocks` for the class under test, `@Mock` for dependencies
- Always call `TestUtils.verifyNoMoreInteractions(this)` in `@AfterEach`

**Integration tests** - Use MockMvc + WireMock with `@IntegrationTest`:
- Extend `BaseIntegrationTest`
- Use `@Sql` for test data setup, `@WireMockStub` for external service mocks

### OpenAPI Code Generation

- **Input**: `src/main/resources/swagger.api/mgr-tenants.yaml`
- **Generated API interfaces**: `org.folio.tm.rest.resource`
- **Generated DTOs**: `org.folio.tm.domain.dto`
- Controllers implement generated interfaces

### Database

- PostgreSQL with Liquibase migrations in `src/main/resources/changelog/`
- Entities extend `Auditable` for automatic created/updated timestamps
- Use `@JdbcTypeCode(SqlTypes.NAMED_ENUM)` for PostgreSQL enums

### Key Directories

```
src/main/java/org/folio/tm/
  controller/     # REST controllers
  service/        # Business logic
  repository/     # Spring Data JPA repositories
  domain/entity/  # JPA entities
  domain/dto/     # Generated DTOs (don't modify)
  mapper/         # MapStruct mappers
  integration/    # External service integrations (Keycloak, Okapi, Kong, Kafka)
  exception/      # Custom exceptions

src/test/java/org/folio/tm/
  it/             # Integration tests (*IT.java)
  service/        # Unit tests (*Test.java)
  support/        # Test utilities and constants

src/test/resources/
  sql/            # SQL scripts for tests
  wiremock/       # WireMock stubs for external services
  json/           # JSON test fixtures
```

### Test Naming Convention

Use `methodName_outcome_condition` pattern:
- `getById_positive`
- `createTenant_negative_nameExists`
- `deleteTenant_positive_notPresent`

## Architecture Patterns

### Event-Driven Listener Pattern

The service layer decouples business logic from integration concerns:
- `TenantService` focuses on core CRUD logic
- `TenantEventsPublisher` broadcasts events to all registered `TenantServiceListener` implementations
- Listeners react independently (Keycloak, Okapi)

### Conditional Bean Registration

Integration components use `@ConditionalOnProperty` for feature toggles:
```java
@ConditionalOnProperty(name = "application.keycloak.enabled", havingValue = "true")
```

Key feature toggles:
- `KC_INTEGRATION_ENABLED` (default: true) - Keycloak integration
- `OKAPI_INTEGRATION_ENABLED` (default: false) - Legacy Okapi integration
- `KONG_INTEGRATION_ENABLED` (default: true) - Kong gateway integration

### Feign Client Pattern

External HTTP integrations use Spring Cloud OpenFeign:
- `KeycloakClient`, `OkapiClient`, `TenantEntitlementsClient`
- Declarative interface-based clients with `@FeignClient` annotation

### Fail-Close Security

When checking entitlements before deletion:
- If `mgr-tenant-entitlements` service is unreachable → BLOCK deletion
- If service returns error → BLOCK deletion
- Only allow deletion if service explicitly confirms no entitlements

## Common Development Scenarios

### Adding a New Tenant Field
1. Update OpenAPI spec: `src/main/resources/swagger.api/mgr-tenants.yaml`
2. Regenerate code: `mvn clean compile`
3. Add field to `TenantEntity` (if persistent)
4. Create Liquibase changeset if database schema changed
5. Update `TenantMapper` if mapping logic needed

### Adding a New Integration Listener
1. Implement `TenantServiceListener` interface
2. Annotate with `@Component` (or `@ConditionalOnProperty` for optional)
3. Spring will auto-register with `TenantEventsPublisher`
4. Implement `onTenantCreate()`, `onTenantUpdate()`, `onTenantDelete()`

### Adding a New Keycloak Client Type
1. Extend `AbstractKeycloakClientService` in `integration/keycloak/service/clients/`
2. Implement `getClientId()`, `buildClientRepresentation()`
3. Register as `@Bean` in `KeycloakConfiguration`
4. Wire into `KeycloakRealmService.createRealm()`
