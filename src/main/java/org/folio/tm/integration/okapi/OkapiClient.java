package org.folio.tm.integration.okapi;

import static org.folio.tm.integration.okapi.OkapiHeaders.TOKEN;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import org.folio.tm.integration.okapi.model.TenantDescriptor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

public interface OkapiClient {

  @GetMapping(value = "/_/proxy/tenants/{tenantId}", consumes = APPLICATION_JSON_VALUE)
  TenantDescriptor getTenantById(
    @PathVariable("tenantId") String tenantId,
    @RequestHeader(TOKEN) String token);

  @PostMapping(value = "/_/proxy/tenants", consumes = APPLICATION_JSON_VALUE)
  TenantDescriptor createTenant(
    @RequestBody TenantDescriptor tenantDescriptor,
    @RequestHeader(TOKEN) String token);

  @PutMapping(value = "/_/proxy/tenants/{tenantId}", consumes = APPLICATION_JSON_VALUE)
  TenantDescriptor updateTenant(
    @PathVariable("tenantId") String tenantId,
    @RequestBody TenantDescriptor tenantDescriptor,
    @RequestHeader(TOKEN) String token);

  @DeleteMapping(value = "/_/proxy/tenants/{tenantId}", consumes = APPLICATION_JSON_VALUE)
  void deleteTenant(
    @PathVariable("tenantId") String tenantId,
    @RequestHeader(TOKEN) String token);
}
