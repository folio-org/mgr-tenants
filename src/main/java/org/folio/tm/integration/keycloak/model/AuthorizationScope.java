package org.folio.tm.integration.keycloak.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AuthorizationScope {

  private String id;
  private String name;
  private String displayName;
  private String iconUri;
}
