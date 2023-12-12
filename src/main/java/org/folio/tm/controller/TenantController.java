package org.folio.tm.controller;

import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NO_CONTENT;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.folio.tm.domain.dto.Tenant;
import org.folio.tm.domain.dto.Tenants;
import org.folio.tm.rest.resource.TenantsApi;
import org.folio.tm.service.TenantService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class TenantController implements TenantsApi {

  private final TenantService tenantService;

  @Override
  public ResponseEntity<Tenant> createTenant(Tenant tenant) {
    return ResponseEntity.status(CREATED).body(tenantService.createTenant(tenant));
  }

  @Override
  public ResponseEntity<Tenant> updateTenantById(UUID id, Tenant tenant) {
    return ResponseEntity.ok(tenantService.updateTenantById(id, tenant));
  }

  @Override
  public ResponseEntity<Tenants> getTenantsByQuery(String query, Integer offset, Integer limit) {
    return ResponseEntity.ok(tenantService.getTenantsByQuery(query, offset, limit));
  }

  @Override
  public ResponseEntity<Tenant> getTenantById(UUID id) {
    return ResponseEntity.ok(tenantService.getTenantById(id));
  }

  @Override
  public ResponseEntity<Void> deleteTenantById(UUID id) {
    tenantService.deleteTenantById(id);
    return ResponseEntity.status(NO_CONTENT).build();
  }
}
