package org.folio.tm.integration.keycloak.service.roles;

import org.keycloak.representations.idm.RoleRepresentation;

public class SystemRoleService extends AbstractKeycloakRoleService {

  public static final String SYSTEM_ROLE_NAME = "System";

  @Override
  public RoleRepresentation getRole(String realm) {
    var roleRepresentation = new RoleRepresentation();
    roleRepresentation.setName(SYSTEM_ROLE_NAME);
    roleRepresentation.setDescription("System role for module-to-module communication");
    return roleRepresentation;
  }
}
