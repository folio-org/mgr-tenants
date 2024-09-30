package org.folio.tm.integration.keycloak.service.clients;

import org.keycloak.representations.idm.ClientRepresentation;

public interface KeycloakClientService {

  /**
   * Retrieves a Keycloak client for given realm name.
   *
   * @param realm - Keycloak realm name
   * @return {@link ClientRepresentation} object
   */
  ClientRepresentation setupClient(String realm);
}
