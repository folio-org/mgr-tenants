package org.folio.tm.integration.keycloak.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.NotFoundException;
import java.util.List;
import org.folio.test.types.UnitTest;
import org.folio.tm.domain.entity.TenantEntity;
import org.folio.tm.integration.keycloak.KeycloakRealmService;
import org.folio.tm.integration.keycloak.migration.UnmanagedAttributePolicyMigration.Summary;
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
class UnmanagedAttributePolicyMigrationTest {

  @InjectMocks private UnmanagedAttributePolicyMigration migration;

  @Mock private TenantRepository tenantRepository;
  @Mock private KeycloakRealmService keycloakRealmService;

  @AfterEach
  void tearDown() {
    TestUtils.verifyNoMoreInteractions(this);
  }

  @Test
  void migrate_positive() {
    when(tenantRepository.findAll()).thenReturn(tenants("tenant1", "tenant2", "tenant3", "tenant4"));
    when(keycloakRealmService.ensureUnmanagedAttributePolicy("tenant1")).thenReturn(true);
    when(keycloakRealmService.ensureUnmanagedAttributePolicy("tenant2")).thenThrow(new RuntimeException("error"));
    when(keycloakRealmService.ensureUnmanagedAttributePolicy("tenant3")).thenThrow(NotFoundException.class);
    when(keycloakRealmService.ensureUnmanagedAttributePolicy("tenant4")).thenReturn(false);

    var result = migration.migrate();

    assertThat(result).isEqualTo(new Summary(4, 1, 1, 1, 1));
    var order = inOrder(keycloakRealmService);
    order.verify(keycloakRealmService).ensureUnmanagedAttributePolicy("tenant1");
    order.verify(keycloakRealmService).ensureUnmanagedAttributePolicy("tenant2");
    order.verify(keycloakRealmService).ensureUnmanagedAttributePolicy("tenant3");
    order.verify(keycloakRealmService).ensureUnmanagedAttributePolicy("tenant4");
  }

  @Test
  void migrate_positive_allCompliant() {
    when(tenantRepository.findAll()).thenReturn(tenants("tenant1", "tenant2"));
    when(keycloakRealmService.ensureUnmanagedAttributePolicy("tenant1")).thenReturn(false);
    when(keycloakRealmService.ensureUnmanagedAttributePolicy("tenant2")).thenReturn(false);

    var result = migration.migrate();

    assertThat(result).isEqualTo(new Summary(2, 0, 2, 0, 0));
  }

  @Test
  void migrate_positive_masterTenantExcluded() {
    when(tenantRepository.findAll()).thenReturn(tenants("master", "tenant1"));
    when(keycloakRealmService.ensureUnmanagedAttributePolicy("tenant1")).thenReturn(true);

    var result = migration.migrate();

    assertThat(result).isEqualTo(new Summary(1, 1, 0, 0, 0));
    verify(keycloakRealmService, never()).ensureUnmanagedAttributePolicy("master");
  }

  @Test
  void migrate_positive_noTenants() {
    when(tenantRepository.findAll()).thenReturn(List.of());

    var result = migration.migrate();

    assertThat(result).isEqualTo(new Summary(0, 0, 0, 0, 0));
    verifyNoInteractions(keycloakRealmService);
  }

  @Test
  void run_positive() {
    when(tenantRepository.findAll()).thenReturn(tenants("tenant1"));
    when(keycloakRealmService.ensureUnmanagedAttributePolicy("tenant1")).thenReturn(true);

    assertThatNoException().isThrownBy(() -> migration.run(null));

    verify(keycloakRealmService).ensureUnmanagedAttributePolicy("tenant1");
  }

  @Test
  void run_positive_totalFailureSwallowed() {
    when(tenantRepository.findAll()).thenThrow(new RuntimeException("database is unavailable"));

    assertThatNoException().isThrownBy(() -> migration.run(null));

    verify(tenantRepository).findAll();
    verifyNoInteractions(keycloakRealmService);
  }

  private static List<TenantEntity> tenants(String... names) {
    return List.of(names).stream().map(name -> {
      var entity = new TenantEntity();
      entity.setName(name);
      return entity;
    }).toList();
  }
}
