package org.folio.tm.integration.entitlements;

import static org.folio.tm.integration.okapi.OkapiHeaders.TOKEN;

import org.folio.tm.integration.entitlements.model.EntitlementsResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

public interface TenantEntitlementsClient {

  /**
   * Retrieves list of entitlements by CQL query.
   *
   * @param limit - maximum number of results to return
   * @param query - CQL query string (e.g., "tenantId==uuid")
   * @param token - X-Okapi-Token header for authorization
   * @return {@link EntitlementsResponse} containing list of entitlements
   */
  @GetMapping("/entitlements")
  EntitlementsResponse getEntitlements(
    @RequestParam(value = "limit", defaultValue = "1") Integer limit,
    @RequestParam(value = "query") String query,
    @RequestHeader(value = TOKEN, required = false) String token
  );
}
