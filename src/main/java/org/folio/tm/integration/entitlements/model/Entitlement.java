package org.folio.tm.integration.entitlements.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entitlement model for mgr-tenant-entitlements integration.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Entitlement {

  @JsonProperty("applicationId")
  private String applicationId;

  @JsonProperty("tenantId")
  private String tenantId;

  @JsonProperty("modules")
  private List<String> modules;
}
