package org.folio.tm.controller;

import static org.springframework.http.HttpStatus.CREATED;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.folio.tm.domain.dto.TenantAttribute;
import org.folio.tm.domain.dto.TenantAttributes;
import org.folio.tm.rest.resource.TenantAttributesApi;
import org.folio.tm.service.TenantAttributeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class TenantAttributeController implements TenantAttributesApi {

  private final TenantAttributeService service;

  @Override
  public ResponseEntity<TenantAttributes> getTenantAttributes(UUID tenantId, String query, Integer offset,
    Integer limit) {
    return ResponseEntity.ok(service.getAll(tenantId, query, offset, limit));
  }

  @Override
  public ResponseEntity<TenantAttributes> createTenantAttributes(UUID tenantId, TenantAttributes tenantAttributes) {
    var created = service.upsertAll(tenantId, tenantAttributes);
    return ResponseEntity.status(CREATED).body(created);
  }

  @Override
  public ResponseEntity<TenantAttribute> getTenantAttribute(UUID tenantId, UUID id) {
    return ResponseEntity.ok(service.get(tenantId, id));
  }

  @Override
  public ResponseEntity<TenantAttribute> updateTenantAttribute(UUID tenantId, UUID id,
    TenantAttribute tenantAttribute) {
    return ResponseEntity.ok(service.update(tenantId, id, tenantAttribute));
  }

  @Override
  public ResponseEntity<Void> deleteTenantAttribute(UUID tenantId, UUID id) {
    service.delete(tenantId, id);
    return ResponseEntity.noContent().build();
  }
}
