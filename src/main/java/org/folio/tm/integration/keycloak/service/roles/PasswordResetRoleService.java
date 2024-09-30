package org.folio.tm.integration.keycloak.service.roles;

import org.keycloak.representations.idm.RoleRepresentation;

public class PasswordResetRoleService extends AbstractKeycloakRoleService {

  public static final String PASSWORD_RESET_ROLE_NAME = "Password Reset";

  @Override
  public RoleRepresentation getRole(String realmName) {
    var roleRepresentation = new RoleRepresentation();
    roleRepresentation.setName(PASSWORD_RESET_ROLE_NAME);
    roleRepresentation.setDescription("A role with access to password reset endpoints");
    return roleRepresentation;
  }
}
