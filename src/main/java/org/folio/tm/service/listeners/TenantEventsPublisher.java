package org.folio.tm.service.listeners;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.tm.domain.dto.Tenant;
import org.springframework.stereotype.Service;

@Log4j2
@Service
@RequiredArgsConstructor
public class TenantEventsPublisher {

  private final List<TenantServiceListener> listeners;

  public void onTenantCreate(Tenant tenant) {
    log.info("Executing 'onTenantCreate' handlers for tenant {}", tenant.getName());
    listeners.forEach(listener -> listener.onTenantCreate(tenant));
  }

  public void onTenantUpdate(Tenant tenant) {
    log.info("Executing 'onTenantUpdate' handlers for tenant {}", tenant.getName());
    listeners.forEach(listener -> listener.onTenantUpdate(tenant));
  }

  public void onTenantDelete(String tenantName) {
    log.info("Executing 'onTenantDelete' handlers for tenant {}", tenantName);
    listeners.forEach(listener -> listener.onTenantDelete(tenantName));
  }
}
