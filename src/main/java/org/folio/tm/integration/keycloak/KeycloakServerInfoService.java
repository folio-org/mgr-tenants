package org.folio.tm.integration.keycloak;

import jakarta.ws.rs.WebApplicationException;
import lombok.RequiredArgsConstructor;
import org.folio.tm.integration.keycloak.exception.KeycloakException;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.info.ServerInfoRepresentation;

@RequiredArgsConstructor
public class KeycloakServerInfoService {

  private final Keycloak keycloak;

  /**
   * Retrieves Keycloak server information.
   *
   * @return {@link ServerInfoRepresentation} object
   */
  public ServerInfoRepresentation getServerInfo() {
    try {
      return keycloak.serverInfo().getInfo();
    } catch (WebApplicationException exception) {
      throw new KeycloakException("Failed to get server info");
    }
  }
}
