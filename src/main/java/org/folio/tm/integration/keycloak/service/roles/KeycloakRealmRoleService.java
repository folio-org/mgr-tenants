package org.folio.tm.integration.keycloak.service.roles;

import org.keycloak.representations.idm.RoleRepresentation;

public interface KeycloakRealmRoleService {

  /**
   * Provides a role to be created within Keycloak realm.
   */
  RoleRepresentation setupRole(String realm);
}
