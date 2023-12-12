package org.folio.tm.integration.keycloak;

import static java.lang.String.format;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.folio.tm.integration.keycloak.KeycloakUtils.extractResourceId;
import static org.folio.tm.integration.keycloak.model.Strategy.DecisionStrategy.AFFIRMATIVE;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.tm.integration.keycloak.KeycloakTemplate.KeycloakFunction;
import org.folio.tm.integration.keycloak.KeycloakTemplate.KeycloakMethod;
import org.folio.tm.integration.keycloak.exception.KeycloakException;
import org.folio.tm.integration.keycloak.model.Client;
import org.folio.tm.integration.keycloak.model.Strategy;
import org.folio.tm.integration.keycloak.model.User;

@Log4j2
@RequiredArgsConstructor
public class KeycloakClientService {

  private final KeycloakClient keycloakClient;
  private final KeycloakTemplate template;

  public Client createClient(Client client, String realm) {
    return template.call(create(client, realm),
      () -> format("Failed to create client [%s] in realm [%s]", client.getClientId(), realm));
  }

  public void deleteClient(String clientId, String realm) {
    template.call(delete(clientId, realm),
      () -> format("Failed to delete client [%s] in realm [%s]", clientId, realm));
  }

  public User getClientServiceAccountUser(Client client, String realm) {
    if (isBlank(client.getId())) {
      throw new IllegalArgumentException("Client id is empty");
    }

    return template.call(getServiceAccount(client, realm),
      () -> format("Failed to get service account user of client [%s] in realm [%s]", client.getClientId(), realm));
  }

  public Client findClientByClientId(String realm, String clientId) {
    return template.call(getClientById(realm, clientId),
      () -> format("Failed to find client [%s] in realm [%s]", clientId, realm));
  }

  private KeycloakFunction<Client> create(Client client, String realm) {
    return token -> {
      var res = keycloakClient.createClient(realm, client, token);

      client.setId(extractResourceId(res).orElse(null));

      log.info("Keycloak client created with id: {}", client.getId());

      if (client.getClientId().startsWith(realm)) {
        updateClientDecisionStrategy(realm, client.getId(), token);
      }
      return client;
    };
  }

  private void updateClientDecisionStrategy(String realm, String clientId, String token) {
    keycloakClient.updateDecisionStrategy(realm, clientId, Strategy.of(AFFIRMATIVE), token);
  }

  private KeycloakMethod delete(String clientId, String realm) {
    return token -> {
      var client = findClientWithClientId(realm, clientId, token);

      if (client != null) {
        keycloakClient.deleteClient(realm, client.getId(), token);

        log.info("Keycloak client deleted: clientId = {}, realm = {}", clientId, realm);
      } else {
        log.info("Keycloak client is not present in the realm: clientId = {}, realm = {}", clientId,
          realm);
      }
    };
  }

  private KeycloakFunction<User> getServiceAccount(Client client, String realm) {
    return token -> {
      var res = keycloakClient.getServiceAccountUser(realm, client.getId(), token);

      log.info("Keycloak service account of client retrieved: account = {}, clientId = {}, realm = {}",
        res.getUserName(), client.getClientId(), realm);

      return res;
    };
  }

  private Client findClientWithClientId(String realm, String clientId, String token) {
    var found = keycloakClient.getClientsByClientId(realm, clientId, token);

    if (isEmpty(found)) {
      return null;
    }

    if (found.size() != 1) {
      throw new KeycloakException(format("Too many keycloak clients with clientId: %s", clientId));
    }

    return found.get(0);
  }

  private KeycloakFunction<Client> getClientById(String realm, String clientId) {
    return token -> findClientWithClientId(realm, clientId, token);
  }
}
