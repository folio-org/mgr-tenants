package org.folio.tm.integration.keycloak.service.clients;

import static jakarta.ws.rs.core.Response.Status.Family.SUCCESSFUL;
import static java.lang.String.format;
import static org.folio.tm.integration.keycloak.utils.KeycloakClientUtils.applyIfNotNull;

import jakarta.annotation.Nullable;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.folio.tm.integration.keycloak.ClientSecretService;
import org.folio.tm.integration.keycloak.exception.KeycloakException;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.authorization.ResourceServerRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;

@Log4j2
public abstract class AbstractKeycloakClientService implements KeycloakClientService {

  protected static final String CLIENT_SECRET_AUTH_TYPE = "client-secret";
  protected static final List<String> KEYCLOAK_CLIENT_DEFAULT_URIS = List.of("/*");

  protected Keycloak keycloak;
  protected ClientSecretService clientSecretService;

  /**
   * Builds keycloak client using given realm and data from implementations.
   *
   * @param realm - Keycloak realm name
   */
  protected ClientRepresentation setupKeycloakClient(String realm) {
    var clientId = getClientId(realm);
    Assert.notNull(clientId, "client id must not be null");
    log.info("Generating client representation: clientId = {}, realm = {}", clientId, realm);

    var clientRepresentation = new ClientRepresentation();
    clientRepresentation.setName(getName(realm));
    clientRepresentation.setClientId(clientId);
    clientRepresentation.setEnabled(true);
    clientRepresentation.setDirectAccessGrantsEnabled(true);
    clientRepresentation.setFrontchannelLogout(true);

    applyIfNotNull(getClientDescription(), clientRepresentation::setDescription);
    applyIfNotNull(getClientSecret(realm, clientId), clientRepresentation::setSecret);
    applyIfNotNull(getClientAuthenticatorType(), clientRepresentation::setClientAuthenticatorType);
    applyIfNotNull(isServiceAccountEnabled(), clientRepresentation::setServiceAccountsEnabled);
    applyIfNotNull(isAuthorizationServicesEnabled(), clientRepresentation::setAuthorizationServicesEnabled);
    applyIfNotNull(getWebOrigins(), clientRepresentation::setWebOrigins);
    applyIfNotNull(getRedirectUris(), clientRepresentation::setRedirectUris);
    applyIfNotNull(getAttributes(), clientRepresentation::setAttributes);
    applyIfNotNull(getProtocolMappers(), clientRepresentation::setProtocolMappers);
    applyIfNotNull(getAuthorizationSettings(), clientRepresentation::setAuthorizationSettings);

    var realmResource = keycloak.realm(realm);
    try (var response = realmResource.clients().create(clientRepresentation)) {
      processKeycloakResponse(clientRepresentation, response);
      clientRepresentation.setId(getKeycloakClientId(realm, response, clientId));
    } catch (WebApplicationException exception) {
      throw new KeycloakException("Failed to create a Keycloak client: " + clientId, exception);
    }

    return clientRepresentation;
  }

  private static String getKeycloakClientId(String realm, Response response, String clientId) {
    return Optional.ofNullable(response.getHeaders())
      .map(headers -> headers.get(HttpHeaders.LOCATION))
      .flatMap(locationHeaderValue -> locationHeaderValue.stream().findFirst())
      .filter(String.class::isInstance)
      .map(String.class::cast)
      .map(string -> StringUtils.substringAfterLast(string, "/"))
      .orElseThrow(() -> new KeycloakException(String.format(
        "Failed to find client id in keycloak response: realm = %s, clientId = %s", realm, clientId)));
  }

  @Autowired
  public void setClientSecretService(ClientSecretService clientSecretService) {
    this.clientSecretService = clientSecretService;
  }

  @Autowired
  public void setKeycloak(Keycloak keycloak) {
    this.keycloak = keycloak;
  }

  /**
   * Defines client identifier.
   *
   * @param realm - keycloak realm name.
   */
  protected abstract String getClientId(String realm);

  /**
   * Defines Keycloak client description.
   */
  protected abstract String getClientDescription();

  /**
   * Returns client secret from secret store.
   *
   * @param realm - realm name
   * @param clientId - client identifier
   * @return client secret as {@link String} object
   */
  protected String getClientSecret(String realm, String clientId) {
    return clientSecretService.getOrCreateClientSecret(realm, clientId);
  }

  /**
   * Defines Keycloak client name.
   *
   * @param realm - Keycloak realm name.
   */
  protected String getName(String realm) {
    return getClientId(realm);
  }

  /**
   * Defines Keycloak client authenticator type.
   */
  protected String getClientAuthenticatorType() {
    return CLIENT_SECRET_AUTH_TYPE;
  }

  /**
   * Defines if Keycloak client service account enabled.
   */
  protected abstract Boolean isServiceAccountEnabled();

  /**
   * Defines if Keycloak client authorization service enabled.
   */
  @Nullable
  protected abstract Boolean isAuthorizationServicesEnabled();

  /**
   * Defines authorization settings for Keycloak client.
   */
  protected ResourceServerRepresentation getAuthorizationSettings() {
    return null;
  }

  /**
   * Defines a list of valid redirect URIs.
   */
  protected List<String> getRedirectUris() {
    return KEYCLOAK_CLIENT_DEFAULT_URIS;
  }

  /**
   * Defines a list of valid web origins.
   */
  protected List<String> getWebOrigins() {
    return KEYCLOAK_CLIENT_DEFAULT_URIS;
  }

  /**
   * Defines a map with Keycloak client attributes.
   */
  @Nullable
  protected abstract Map<String, String> getAttributes();

  /**
   * Defines a list with Keycloak client protocol mappers.
   */
  @Nullable
  @SuppressWarnings("java:S1168")
  protected List<ProtocolMapperRepresentation> getProtocolMappers() {
    return null;
  }

  /**
   * Assigns a role to a Keycloak client user.
   *
   * @param realm - realm name
   * @param client - created client in Keycloak
   * @param role - role name to assign
   */
  protected void assignRoleToServiceAccount(String realm, ClientRepresentation client, String role) {
    var realmResource = keycloak.realm(realm);

    try {
      var serviceAccountUser = realmResource.clients().get(client.getId()).getServiceAccountUser();
      var roleByName = realmResource.roles().get(role).toRepresentation();
      realmResource.users().get(serviceAccountUser.getId()).roles().realmLevel().add(List.of(roleByName));
    } catch (WebApplicationException exception) {
      var clientId = client.getClientId();
      throw new KeycloakException(format("Failed to assign a role: '%s' to a client: %s", role, clientId), exception);
    }
  }

  private static void processKeycloakResponse(ClientRepresentation client, Response response) {
    var statusInfo = response.getStatusInfo();
    if (statusInfo.getFamily() == SUCCESSFUL) {
      log.debug("Keycloak client created: id = {}, name = {}", client.getId(), client.getName());
      return;
    }

    throw new KeycloakException(format(
      "Failed to create Keycloak client. Details: id = %s, status = %s, message = %s", client.getId(),
      statusInfo.getStatusCode(), statusInfo.getReasonPhrase()));
  }
}
