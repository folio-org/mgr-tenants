package org.folio.tm.integration.entitlements.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response model for entitlements list from mgr-tenant-entitlements.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EntitlementsResponse {

  @JsonProperty("entitlements")
  private List<Entitlement> entitlements;

  @JsonProperty("totalRecords")
  private Integer totalRecords;
}
