package org.folio.tm.integration.keycloak.model;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AuthorizationRolePolicy extends AuthorizationPolicy {

  private List<RolePolicy> roles;

  @Data
  @AllArgsConstructor(staticName = "of")
  public static class RolePolicy {

    private String id;
    private Boolean required;
  }
}
