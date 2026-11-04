package org.folio.tm.integration.keycloak.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.NotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.folio.test.types.UnitTest;
import org.folio.tm.domain.entity.TenantEntity;
import org.folio.tm.integration.keycloak.migration.AbstractRealmMigration.Summary;
import org.folio.tm.repository.TenantRepository;
import org.folio.tm.support.TestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class AbstractRealmMigrationTest {

  @Mock private TenantRepository tenantRepository;

  @AfterEach
  void tearDown() {
    TestUtils.verifyNoMoreInteractions(this);
  }

  @Test
  void migrate_positive_allOutcomes() {
    var migration = migration(AbstractRealmMigrationTest::outcomeByRealm);
    when(tenantRepository.findAll()).thenReturn(tenants("tenant1", "tenant2", "tenant3", "tenant4"));

    var result = migration.migrate();

    assertThat(result).isEqualTo(new Summary(4, 1, 1, 1, 1));
    assertThat(migration.enforcedRealms).containsExactly("tenant1", "tenant2", "tenant3", "tenant4");
  }

  @Test
  void migrate_positive_allCompliant() {
    var migration = migration(realm -> false);
    when(tenantRepository.findAll()).thenReturn(tenants("tenant1", "tenant2"));

    var result = migration.migrate();

    assertThat(result).isEqualTo(new Summary(2, 0, 2, 0, 0));
  }

  @Test
  void migrate_positive_masterTenantExcluded() {
    var migration = migration(realm -> true);
    when(tenantRepository.findAll()).thenReturn(tenants("master", "tenant1"));

    var result = migration.migrate();

    assertThat(result).isEqualTo(new Summary(1, 1, 0, 0, 0));
    assertThat(migration.enforcedRealms).containsExactly("tenant1");
  }

  @Test
  void migrate_positive_noTenants() {
    var migration = migration(realm -> true);
    when(tenantRepository.findAll()).thenReturn(List.of());

    var result = migration.migrate();

    assertThat(result).isEqualTo(new Summary(0, 0, 0, 0, 0));
    assertThat(migration.enforcedRealms).isEmpty();
  }

  @Test
  void run_positive_migrationExecuted() {
    var migration = migration(realm -> true);
    when(tenantRepository.findAll()).thenReturn(tenants("tenant1"));

    assertThatNoException().isThrownBy(() -> migration.run(null));

    assertThat(migration.enforcedRealms).containsExactly("tenant1");
  }

  @Test
  void run_negative_totalFailureSwallowed() {
    var migration = migration(realm -> true);
    when(tenantRepository.findAll()).thenThrow(new RuntimeException("database is unavailable"));

    assertThatNoException().isThrownBy(() -> migration.run(null));

    assertThat(migration.enforcedRealms).isEmpty();
  }

  private TestMigration migration(Function<String, Boolean> enforcer) {
    return new TestMigration(tenantRepository, enforcer);
  }

  private static Boolean outcomeByRealm(String realmName) {
    return switch (realmName) {
      case "tenant1" -> true;
      case "tenant2" -> throw new RuntimeException("error");
      case "tenant3" -> throw new NotFoundException();
      default -> false;
    };
  }

  private static List<TenantEntity> tenants(String... names) {
    return List.of(names).stream().map(name -> {
      var entity = new TenantEntity();
      entity.setName(name);
      return entity;
    }).toList();
  }

  private static final class TestMigration extends AbstractRealmMigration {

    private final Function<String, Boolean> enforcer;
    private final List<String> enforcedRealms = new ArrayList<>();

    private TestMigration(TenantRepository tenantRepository, Function<String, Boolean> enforcer) {
      super(tenantRepository, "TEST MIGRATION", "test setting");
      this.enforcer = enforcer;
    }

    @Override
    protected boolean enforce(String realmName) {
      enforcedRealms.add(realmName);
      return enforcer.apply(realmName);
    }
  }
}
