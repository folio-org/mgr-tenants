package org.folio.tm.integration.okapi;

import static org.folio.tm.integration.okapi.OkapiHeaders.TOKEN;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import org.folio.tm.integration.okapi.model.TenantDescriptor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.service.annotation.DeleteExchange;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;
import org.springframework.web.service.annotation.PutExchange;

@HttpExchange
public interface OkapiClient {

  @GetExchange(url = "/_/proxy/tenants/{tenantId}")
  TenantDescriptor getTenantById(
    @PathVariable("tenantId") String tenantId,
    @RequestHeader(value = TOKEN, required = false) String token);

  @PostExchange(value = "/_/proxy/tenants", contentType = APPLICATION_JSON_VALUE)
  TenantDescriptor createTenant(
    @RequestBody TenantDescriptor tenantDescriptor,
    @RequestHeader(TOKEN) String token);

  @PutExchange(value = "/_/proxy/tenants/{tenantId}", contentType = APPLICATION_JSON_VALUE)
  TenantDescriptor updateTenant(
    @PathVariable("tenantId") String tenantId,
    @RequestBody TenantDescriptor tenantDescriptor,
    @RequestHeader(TOKEN) String token);

  @DeleteExchange(value = "/_/proxy/tenants/{tenantId}", contentType = APPLICATION_JSON_VALUE)
  void deleteTenant(
    @PathVariable("tenantId") String tenantId,
    @RequestHeader(TOKEN) String token);
}
