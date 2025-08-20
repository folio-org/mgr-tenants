package org.folio.tm.integration.keycloak.service.clients;

import static org.folio.tm.integration.keycloak.model.Client.OPENID_CONNECT_PROTOCOL;
import static org.folio.tm.integration.keycloak.service.roles.PasswordResetRoleService.PASSWORD_RESET_ROLE_NAME;
import static org.folio.tm.integration.keycloak.utils.KeycloakClientUtils.getSubjectProtocolMapper;

import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.tm.integration.keycloak.KeycloakServerInfoService;
import org.folio.tm.integration.keycloak.configuration.KeycloakRealmSetupProperties;
import org.folio.tm.integration.keycloak.model.ClientAttributes;
import org.folio.tm.integration.keycloak.model.ProtocolMapperConfig;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.ProtocolMapperTypeRepresentation;

@Log4j2
@RequiredArgsConstructor
public class PasswordResetClientService extends AbstractKeycloakClientService {

  private static final String PASSWORD_RESET_ACTION_MAPPER = "Password reset action mapper";
  private static final String PASSWORD_RESET_ACTION_ID_CLAIM = "passwordResetActionId";

  private final KeycloakServerInfoService keycloakServerInfoService;
  private final KeycloakRealmSetupProperties keycloakRealmSetupProperties;

  @Override
  public ClientRepresentation setupClient(String realm) {
    var clientRepresentation = setupKeycloakClient(realm);
    assignRoleToServiceAccount(realm, clientRepresentation, PASSWORD_RESET_ROLE_NAME);
    return clientRepresentation;
  }

  @Override
  protected String getClientId(String realm) {
    var passwordResetClientConfiguration = keycloakRealmSetupProperties.getPasswordResetClient();
    return passwordResetClientConfiguration.getClientId();
  }

  @Override
  protected String getClientDescription() {
    return "Client for password reset operations";
  }

  @Override
  protected Boolean isServiceAccountEnabled() {
    return true;
  }

  @Override
  protected Boolean isAuthorizationServicesEnabled() {
    return false;
  }

  @Override
  protected Map<String, String> getAttributes() {
    var clientProperties = keycloakRealmSetupProperties.getPasswordResetClient();
    return ClientAttributes.of(clientProperties.getTokenLifespan(), false).asMap();
  }

  @Override
  protected List<ProtocolMapperRepresentation> getProtocolMappers() {
    return List.of(getPasswordResetProtocolMapper(), getSubjectProtocolMapper());
  }

  private ProtocolMapperRepresentation getPasswordResetProtocolMapper() {
    var serverInfo = keycloakServerInfoService.getServerInfo();
    if (serverInfo == null || serverInfo.getProtocolMapperTypes() == null) {
      throw new EntityNotFoundException("Mapper is not found by name: " + PASSWORD_RESET_ACTION_MAPPER);
    }

    return serverInfo.getProtocolMapperTypes().get(OPENID_CONNECT_PROTOCOL).stream()
      .filter(mapper -> mapper.getName().equals(PASSWORD_RESET_ACTION_MAPPER))
      .findFirst()
      .map(typeRepr -> toProtocolMapper(typeRepr, null, PASSWORD_RESET_ACTION_ID_CLAIM))
      .orElseThrow(() -> new EntityNotFoundException("Mapper is not found by name: " + PASSWORD_RESET_ACTION_MAPPER));
  }

  public static ProtocolMapperRepresentation toProtocolMapper(ProtocolMapperTypeRepresentation mapperType,
    String userAttr, String claimName) {
    var protocolMapperRepresentation = new ProtocolMapperRepresentation();

    protocolMapperRepresentation.setName(mapperType.getName());
    protocolMapperRepresentation.setProtocol(OPENID_CONNECT_PROTOCOL);
    protocolMapperRepresentation.setProtocolMapper(mapperType.getId());
    protocolMapperRepresentation.setConfig(ProtocolMapperConfig.forUserAttribute(userAttr, claimName).asMap());

    return protocolMapperRepresentation;
  }
}
