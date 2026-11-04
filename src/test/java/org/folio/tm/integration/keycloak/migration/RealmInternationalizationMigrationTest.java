package org.folio.tm.integration.keycloak.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.NotFoundException;
import java.util.List;
import org.folio.test.types.UnitTest;
import org.folio.tm.domain.entity.TenantEntity;
import org.folio.tm.integration.keycloak.KeycloakRealmService;
import org.folio.tm.integration.keycloak.migration.AbstractRealmMigration.Summary;
import org.folio.tm.repository.TenantRepository;
import org.folio.tm.support.TestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class RealmInternationalizationMigrationTest {

  @InjectMocks private RealmInternationalizationMigration migration;

  @Mock private TenantRepository tenantRepository;
  @Mock private KeycloakRealmService keycloakRealmService;

  @AfterEach
  void tearDown() {
    TestUtils.verifyNoMoreInteractions(this);
  }

  @Test
  void migrate_positive_realmUpdated() {
    when(tenantRepository.findAll()).thenReturn(tenants("tenant1"));
    when(keycloakRealmService.ensureInternationalization("tenant1")).thenReturn(true);

    var result = migration.migrate();

    assertThat(result).isEqualTo(new Summary(1, 1, 0, 0, 0));
  }

  @Test
  void migrate_positive_realmAlreadyCompliant() {
    when(tenantRepository.findAll()).thenReturn(tenants("tenant1"));
    when(keycloakRealmService.ensureInternationalization("tenant1")).thenReturn(false);

    var result = migration.migrate();

    assertThat(result).isEqualTo(new Summary(1, 0, 1, 0, 0));
  }

  @Test
  void migrate_positive_realmMissing() {
    when(tenantRepository.findAll()).thenReturn(tenants("tenant1"));
    when(keycloakRealmService.ensureInternationalization("tenant1")).thenThrow(NotFoundException.class);

    var result = migration.migrate();

    assertThat(result).isEqualTo(new Summary(1, 0, 0, 1, 0));
  }

  @Test
  void migrate_positive_realmUpdateFailed() {
    when(tenantRepository.findAll()).thenReturn(tenants("tenant1", "tenant2"));
    when(keycloakRealmService.ensureInternationalization("tenant1")).thenThrow(new RuntimeException("error"));
    when(keycloakRealmService.ensureInternationalization("tenant2")).thenReturn(true);

    var result = migration.migrate();

    assertThat(result).isEqualTo(new Summary(2, 1, 0, 0, 1));
  }

  @Test
  void migrate_positive_masterTenantExcluded() {
    when(tenantRepository.findAll()).thenReturn(tenants("master", "tenant1"));
    when(keycloakRealmService.ensureInternationalization("tenant1")).thenReturn(true);

    var result = migration.migrate();

    assertThat(result).isEqualTo(new Summary(1, 1, 0, 0, 0));
    verify(keycloakRealmService, never()).ensureInternationalization("master");
  }

  private static List<TenantEntity> tenants(String... names) {
    return List.of(names).stream().map(name -> {
      var entity = new TenantEntity();
      entity.setName(name);
      return entity;
    }).toList();
  }
}
