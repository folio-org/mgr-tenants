# AGENTS.md - Coding Agent Guidelines for mgr-tenants

## Project Overview

Tenant management service for the FOLIO library services platform. Java 21 / Spring Boot 3.5.7 / Maven project with PostgreSQL, Keycloak, Kong, and Kafka integrations.

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

**Unit tests** - Use Mockito:
```java
@UnitTest
@ExtendWith(MockitoExtension.class)
class TenantServiceTest {
    @InjectMocks private TenantService tenantService;
    @Mock private TenantMapper mapper;
    @Mock private TenantRepository repository;

    @AfterEach
    void tearDown() {
        TestUtils.verifyNoMoreInteractions(this);
    }

    @Test
    void getById_positive() {
        when(repository.findById(TENANT_ID)).thenReturn(Optional.of(entity));
        assertThat(result).isEqualTo(expected);
    }
}
```

**Integration tests** - Use MockMvc + WireMock:
```java
@IntegrationTest
class TenantIT extends BaseIntegrationTest {
    @Test
    @Sql("classpath:/sql/populate_tenants.sql")
    @WireMockStub(scripts = {"/wiremock/stubs/okapi/create-tenant.json"})
    void createTenant_positive() throws Exception {
        mockMvc.perform(post("/tenants")
            .contentType(APPLICATION_JSON)
            .header(TOKEN, AUTH_TOKEN)
            .content(TestUtils.asJsonString(tenant)))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.id", is(valueOf(tenant.getId()))));
    }
}
```

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
