package org.folio.tm.integration.keycloak.service.clients;

import static org.folio.tm.integration.keycloak.service.roles.SystemRoleService.SYSTEM_ROLE_NAME;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.tm.integration.keycloak.configuration.KeycloakRealmSetupProperties;
import org.folio.tm.integration.keycloak.model.ClientAttributes;
import org.keycloak.representations.idm.ClientRepresentation;

@Log4j2
@RequiredArgsConstructor
public class ModuleClientService extends AbstractKeycloakClientService {

  private final KeycloakRealmSetupProperties keycloakRealmSetupProperties;

  @Override
  public ClientRepresentation setupClient(String realm) {
    var clientRepresentation = setupKeycloakClient(realm);
    assignRoleToServiceAccount(realm, clientRepresentation, SYSTEM_ROLE_NAME);
    return clientRepresentation;
  }

  @Override
  protected Map<String, String> getAttributes() {
    return ClientAttributes.defaultValue().asMap();
  }

  @Override
  protected Boolean isServiceAccountEnabled() {
    return true;
  }

  @Override
  protected Boolean isAuthorizationServicesEnabled() {
    return true;
  }

  @Override
  protected String getClientId(String realm) {
    var moduleClientConfiguration = keycloakRealmSetupProperties.getM2mClient();
    return moduleClientConfiguration.getClientId();
  }

  @Override
  protected String getClientDescription() {
    return "Client for module-to-module communication";
  }
}
