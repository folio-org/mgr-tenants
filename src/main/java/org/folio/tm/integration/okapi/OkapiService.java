package org.folio.tm.integration.okapi;

import feign.FeignException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.tm.domain.dto.Tenant;
import org.folio.tm.integration.okapi.exception.OkapiRequestException;
import org.folio.tm.integration.okapi.model.TenantDescriptor;
import org.folio.tm.service.listeners.TenantServiceListener;

@Log4j2
@RequiredArgsConstructor
public class OkapiService implements TenantServiceListener {

  private final OkapiClient okapiClient;
  private final HttpServletRequest request;

  @Override
  public void onTenantCreate(Tenant tenant) {
    log.info("Running Okapi event 'onTenantCreate' for tenant {}", tenant.getName());
    createTenant(tenant);
  }

  @Override
  public void onTenantUpdate(Tenant tenant) {
    log.info("Running Okapi event 'onTenantUpdate' for tenant {}", tenant.getName());
    updateTenantById(tenant);
  }

  @Override
  public void onTenantDelete(String tenantName) {
    log.info("Running Okapi event 'onTenantDelete' for tenant {}", tenantName);
    deleteTenantById(tenantName);
  }

  /**
   * Creates tenant in Okapi using given {@link Tenant} descriptor.
   */
  public TenantDescriptor createTenant(Tenant tenant) {
    try {
      if (isTenantExist(tenant.getName())) {
        return toTenantDescriptor(tenant);
      }
      log.debug("Creating tenant descriptor [id: {}]", tenant.getName());
      return okapiClient.createTenant(toTenantDescriptor(tenant), getOkapiToken());
    } catch (Exception e) {
      throw new OkapiRequestException("Failed to create tenant", e);
    }
  }

  public TenantDescriptor updateTenantById(Tenant tenant) {
    var okapiTenantId = tenant.getName();
    try {
      log.debug("Updating tenant descriptor [id: {}]", okapiTenantId);
      return okapiClient.updateTenant(okapiTenantId, toTenantDescriptor(tenant), getOkapiToken());
    } catch (Exception e) {
      throw new OkapiRequestException("Failed to update tenant: " + okapiTenantId, e);
    }
  }

  public void deleteTenantById(String name) {
    try {
      if (isTenantExist(name)) {
        log.debug("Deleting tenant descriptor [id: {}]", name);
        okapiClient.deleteTenant(name, getOkapiToken());
      }
      log.debug("Tenant already deleted in okapi [id: {}]", name);
    } catch (Exception e) {
      throw new OkapiRequestException("Failed to delete tenant by id: " + name, e);
    }
  }

  public TenantDescriptor getTenant(String id) {
    try {
      return okapiClient.getTenantById(id, getOkapiToken());
    } catch (Exception e) {
      throw new OkapiRequestException("Failed to get tenant by id: " + id, e);
    }
  }

  private static TenantDescriptor toTenantDescriptor(Tenant tenant) {
    return new TenantDescriptor(tenant.getName(), tenant.getName(), tenant.getDescription());
  }

  private String getOkapiToken() {
    return request.getHeader(OkapiHeaders.TOKEN);
  }

  private boolean isTenantExist(String tenantId) {
    try {
      log.debug("Check existence of tenant [id: {}]", tenantId);
      var getResponse = okapiClient.getTenantById(tenantId, getOkapiToken());
      log.debug("Tenant exists in Okapi [id: {}]", tenantId);
      return Objects.nonNull(getResponse);
    } catch (FeignException.NotFound e) {
      log.warn("Tenant was not found in Okapi [id: {}]", tenantId);
      return false;
    }
  }
}
