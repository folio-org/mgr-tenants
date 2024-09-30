package org.folio.tm.integration.keycloak;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.tm.service.listeners.TenantServiceListener;

@Log4j2
@RequiredArgsConstructor
public class KeycloakTenantListener implements TenantServiceListener {

  private final KeycloakRealmService keycloakRealmService;

  @Override
  public void onTenantCreate(org.folio.tm.domain.dto.Tenant tenant) {
    log.debug("Running Keycloak event 'onTenantCreate' for tenant {}", tenant.getName());
    keycloakRealmService.createRealm(tenant);
  }

  @Override
  public void onTenantUpdate(org.folio.tm.domain.dto.Tenant tenant) {
    log.debug("Running Keycloak event 'onTenantUpdate' for tenant {}", tenant.getName());
    keycloakRealmService.updateRealm(tenant);
  }

  @Override
  public void onTenantDelete(String tenantName) {
    log.debug("Running Keycloak event 'onTenantDelete' for tenant {}", tenantName);
    keycloakRealmService.deleteRealm(tenantName);
  }
}
