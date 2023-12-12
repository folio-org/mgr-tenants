package org.folio.tm.integration.keycloak.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuthorizationPolicy {

  private String id;
  private String type;
  private String name;
}
