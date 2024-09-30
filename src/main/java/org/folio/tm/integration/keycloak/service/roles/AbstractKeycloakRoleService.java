package org.folio.tm.integration.keycloak.service.roles;

import jakarta.ws.rs.WebApplicationException;
import lombok.extern.log4j.Log4j2;
import org.folio.tm.integration.keycloak.exception.KeycloakException;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.RoleRepresentation;
import org.springframework.beans.factory.annotation.Autowired;

@Log4j2
public abstract class AbstractKeycloakRoleService implements KeycloakRealmRoleService {

  private Keycloak keycloak;

  @Override
  public RoleRepresentation setupRole(String realm) {
    var roleRepresentation = getRole(realm);
    var roleName = roleRepresentation.getName();
    try {
      log.debug("Creating realm-level role in Keycloak: name: {}", roleName);
      var realmResource = keycloak.realm(realm);
      realmResource.roles().create(roleRepresentation);
      var createdRoleRepresentation = realmResource.roles().get(roleName).toRepresentation();
      log.info("Keycloak role created: name: {},  id: {}", roleName, createdRoleRepresentation.getId());
      return roleRepresentation;
    } catch (WebApplicationException exception) {
      throw new KeycloakException("Failed to setup role: " + roleName, exception);
    }
  }

  protected abstract RoleRepresentation getRole(String realm);

  @Autowired
  public void setKeycloak(Keycloak keycloak) {
    this.keycloak = keycloak;
  }
}
