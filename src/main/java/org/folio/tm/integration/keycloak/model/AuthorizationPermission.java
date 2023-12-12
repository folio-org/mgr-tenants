package org.folio.tm.integration.keycloak.model;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthorizationPermission {

  private String id;
  private String type;
  private String name;
  private List<String> scopes;
  private List<String> policies;
}
