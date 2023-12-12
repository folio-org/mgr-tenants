package org.folio.tm.integration.keycloak.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserManagementPermission {

  private boolean enabled;

  private String resource;
  private ScopePermission scopePermissions;

  public UserManagementPermission(boolean enabled) {
    this.enabled = enabled;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class ScopePermission {

    private String impersonate;
    private String manage;
    private String manageGroupMembership;
    private String mapRoles;
    private String userImpersonated;
    private String view;
  }
}
