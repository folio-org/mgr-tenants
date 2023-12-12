package org.folio.tm.service.listeners;

import org.folio.tm.domain.dto.Tenant;
import org.folio.tm.service.TenantService;

/**
 * Interface contains methods that should react on events in {@link TenantService}.
 */
public interface TenantServiceListener {

  /**
   * Handle tenant create event.
   *
   * @param tenant - object with a tenant's data
   */
  default void onTenantCreate(Tenant tenant) {}

  /**
   * Handle tenant update event.
   *
   * @param tenant - object with a tenant's data
   */
  default void onTenantUpdate(Tenant tenant) {}

  /**
   * Handle tenant delete event.
   *
   * @param tenantName - tenant name to delete
   */
  default void onTenantDelete(String tenantName) {}
}
