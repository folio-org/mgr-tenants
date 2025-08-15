package org.folio.tm.integration.keycloak.service.clients;

import static jakarta.ws.rs.core.HttpHeaders.LOCATION;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.folio.tm.support.TestConstants.TENANT_NAME;
import static org.folio.tm.support.TestUtils.assertEqualsUsingRecursiveComparison;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityNotFoundException;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.NotFoundException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.folio.test.types.UnitTest;
import org.folio.tm.integration.keycloak.ClientSecretService;
import org.folio.tm.integration.keycloak.KeycloakServerInfoService;
import org.folio.tm.integration.keycloak.configuration.KeycloakRealmSetupProperties;
import org.folio.tm.integration.keycloak.configuration.PasswordResetClientProperties;
import org.folio.tm.integration.keycloak.exception.KeycloakException;
import org.folio.tm.integration.keycloak.model.ClientAttributes;
import org.folio.tm.integration.keycloak.model.ProtocolMapperConfig;
import org.folio.tm.support.TestUtils;
import org.jboss.resteasy.core.Headers;
import org.jboss.resteasy.core.ServerResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RoleScopeResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.ProtocolMapperTypeRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.info.ServerInfoRepresentation;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class PasswordResetClientServiceTest {

  private static final String CLIENT_ID = "password-reset-client";
  private static final String CLIENT_UUID = UUID.randomUUID().toString();
  private static final String CLIENT_SECRET = UUID.randomUUID().toString();

  private static final String PASSWORD_RESET_ROLE_ID = UUID.randomUUID().toString();
  private static final String SERVICE_ACCOUNT_USER_ID = UUID.randomUUID().toString();

  @InjectMocks private PasswordResetClientService passwordResetClientService;
  @Mock private ClientSecretService clientSecretService;
  @Mock private KeycloakServerInfoService keycloakServerInfoService;
  @Mock private KeycloakRealmSetupProperties keycloakRealmSetupProperties;

  @Mock(answer = RETURNS_DEEP_STUBS) private Keycloak keycloak;
  @Mock(answer = RETURNS_DEEP_STUBS) private RealmResource realmResource;

  @Mock private RoleScopeResource roleScopeResource;
  @Captor private ArgumentCaptor<ClientRepresentation> clientCaptor;
  @Captor private ArgumentCaptor<List<RoleRepresentation>> rolesCaptor;

  @BeforeEach
  void setUp() {
    passwordResetClientService.setKeycloak(keycloak);
    passwordResetClientService.setClientSecretService(clientSecretService);
  }

  @AfterEach
  void tearDown() {
    TestUtils.verifyNoMoreInteractions(this);
  }

  @Test
  void setupClient_positive() {
    var clientResponse = new ServerResponse(null, 201, responseHeaders());

    when(keycloakRealmSetupProperties.getPasswordResetClient()).thenReturn(passwordResetClientProperties());
    when(clientSecretService.getOrCreateClientSecret(TENANT_NAME, CLIENT_ID)).thenReturn(CLIENT_SECRET);
    when(keycloak.realm(TENANT_NAME)).thenReturn(realmResource);
    when(realmResource.clients().create(clientCaptor.capture())).thenReturn(clientResponse);

    when(realmResource.clients().get(CLIENT_UUID).getServiceAccountUser()).thenReturn(serviceAccountUser());
    when(realmResource.roles().get("Password Reset").toRepresentation()).thenReturn(passwordResetRole());
    when(realmResource.users().get(SERVICE_ACCOUNT_USER_ID).roles().realmLevel()).thenReturn(roleScopeResource);
    when(keycloakServerInfoService.getServerInfo()).thenReturn(serverInfo());
    doNothing().when(roleScopeResource).add(rolesCaptor.capture());

    var result = passwordResetClientService.setupClient(TENANT_NAME);

    var passwordResetClient = passwordResetClient();
    var excludedFields = new String[] {"id", "attributes.client.secret.creation.time"};
    assertEqualsUsingRecursiveComparison(result, passwordResetClient, excludedFields);
    assertEqualsUsingRecursiveComparison(clientCaptor.getValue(), passwordResetClient, excludedFields);

    assertThat(rolesCaptor.getValue()).hasSize(1);
    var capturedRole = rolesCaptor.getValue().get(0);
    assertEqualsUsingRecursiveComparison(capturedRole, passwordResetRole());

    verify(realmResource, atLeastOnce()).roles();
    verify(realmResource, atLeastOnce()).users();
    verify(realmResource, atLeastOnce()).clients();
  }

  @Test
  void setupClient_negative_failedToCreateClient() {
    var internalServerError = new ServerResponse(null, 500, new Headers<>());

    when(keycloakServerInfoService.getServerInfo()).thenReturn(serverInfo());
    when(keycloakRealmSetupProperties.getPasswordResetClient()).thenReturn(passwordResetClientProperties());
    when(clientSecretService.getOrCreateClientSecret(TENANT_NAME, CLIENT_ID)).thenReturn(CLIENT_SECRET);
    when(keycloak.realm(TENANT_NAME)).thenReturn(realmResource);
    when(realmResource.clients().create(clientCaptor.capture())).thenReturn(internalServerError);

    assertThatThrownBy(() -> passwordResetClientService.setupClient(TENANT_NAME))
      .isInstanceOf(KeycloakException.class)
      .hasMessage("Failed to create Keycloak client. "
        + "Details: id = null, status = 500, message = Internal Server Error");

    var excludedFields = new String[] {"id", "attributes.client.secret.creation.time"};
    assertEqualsUsingRecursiveComparison(clientCaptor.getValue(), passwordResetClient(), excludedFields);

    verify(realmResource, atLeastOnce()).clients();
  }

  @Test
  void setupClient_negative_failedToCreateClientWithException() {

    when(keycloakServerInfoService.getServerInfo()).thenReturn(serverInfo());
    when(keycloakRealmSetupProperties.getPasswordResetClient()).thenReturn(passwordResetClientProperties());
    when(clientSecretService.getOrCreateClientSecret(TENANT_NAME, CLIENT_ID)).thenReturn(CLIENT_SECRET);
    when(keycloak.realm(TENANT_NAME)).thenReturn(realmResource);
    when(realmResource.clients().create(clientCaptor.capture())).thenThrow(InternalServerErrorException.class);

    assertThatThrownBy(() -> passwordResetClientService.setupClient(TENANT_NAME))
      .isInstanceOf(KeycloakException.class)
      .hasMessage("Failed to create a Keycloak client: %s", CLIENT_ID)
      .hasCauseInstanceOf(InternalServerErrorException.class);

    var excludedFields = new String[] {"id", "attributes.client.secret.creation.time"};
    assertEqualsUsingRecursiveComparison(clientCaptor.getValue(), passwordResetClient(), excludedFields);

    verify(realmResource, atLeastOnce()).clients();
  }

  @Test
  void setupClient_negative_failedToRetrieveServiceAccountUser() {
    var clientResponse = new ServerResponse(null, 201, responseHeaders());

    when(keycloakServerInfoService.getServerInfo()).thenReturn(serverInfo());
    when(keycloakRealmSetupProperties.getPasswordResetClient()).thenReturn(passwordResetClientProperties());
    when(clientSecretService.getOrCreateClientSecret(TENANT_NAME, CLIENT_ID)).thenReturn(CLIENT_SECRET);
    when(keycloak.realm(TENANT_NAME)).thenReturn(realmResource);
    when(realmResource.clients().create(clientCaptor.capture())).thenReturn(clientResponse);
    when(realmResource.clients().get(CLIENT_UUID).getServiceAccountUser()).thenThrow(NotFoundException.class);

    assertThatThrownBy(() -> passwordResetClientService.setupClient(TENANT_NAME))
      .isInstanceOf(KeycloakException.class)
      .hasMessage("Failed to assign a role: '%s' to a client: %s", "Password Reset", "password-reset-client")
      .hasCauseInstanceOf(NotFoundException.class);

    var excludedFields = new String[] {"id", "attributes.client.secret.creation.time"};
    assertEqualsUsingRecursiveComparison(clientCaptor.getValue(), passwordResetClient(), excludedFields);

    verify(realmResource, atLeastOnce()).clients();
  }

  @Test
  void setupClient_negative_failedToFindRoleByName() {
    var clientResponse = new ServerResponse(null, 201, responseHeaders());

    when(keycloakServerInfoService.getServerInfo()).thenReturn(serverInfo());
    when(keycloakRealmSetupProperties.getPasswordResetClient()).thenReturn(passwordResetClientProperties());
    when(clientSecretService.getOrCreateClientSecret(TENANT_NAME, CLIENT_ID)).thenReturn(CLIENT_SECRET);
    when(keycloak.realm(TENANT_NAME)).thenReturn(realmResource);
    when(realmResource.clients().create(clientCaptor.capture())).thenReturn(clientResponse);

    when(realmResource.clients().get(CLIENT_UUID).getServiceAccountUser()).thenReturn(serviceAccountUser());
    when(realmResource.roles().get("Password Reset").toRepresentation()).thenThrow(NotFoundException.class);

    assertThatThrownBy(() -> passwordResetClientService.setupClient(TENANT_NAME))
      .isInstanceOf(KeycloakException.class)
      .hasMessage("Failed to assign a role: '%s' to a client: %s", "Password Reset", "password-reset-client")
      .hasCauseInstanceOf(NotFoundException.class);

    var excludedFields = new String[] {"id", "attributes.client.secret.creation.time"};
    assertEqualsUsingRecursiveComparison(clientCaptor.getValue(), passwordResetClient(), excludedFields);

    verify(realmResource, atLeastOnce()).roles();
    verify(realmResource, atLeastOnce()).clients();
  }

  @Test
  void setupClient_negative_failedToAssignRoleToServiceAccountUser() {
    var clientResponse = new ServerResponse(null, 201, responseHeaders());

    when(keycloakRealmSetupProperties.getPasswordResetClient()).thenReturn(passwordResetClientProperties());
    when(clientSecretService.getOrCreateClientSecret(TENANT_NAME, CLIENT_ID)).thenReturn(CLIENT_SECRET);
    when(keycloak.realm(TENANT_NAME)).thenReturn(realmResource);
    when(keycloakServerInfoService.getServerInfo()).thenReturn(serverInfo());
    when(realmResource.clients().create(clientCaptor.capture())).thenReturn(clientResponse);

    when(realmResource.clients().get(CLIENT_UUID).getServiceAccountUser()).thenReturn(serviceAccountUser());
    when(realmResource.roles().get("System").toRepresentation()).thenReturn(passwordResetRole());
    when(realmResource.users().get(SERVICE_ACCOUNT_USER_ID).roles().realmLevel()).thenReturn(roleScopeResource);
    doThrow(InternalServerErrorException.class).when(roleScopeResource).add(rolesCaptor.capture());

    assertThatThrownBy(() -> passwordResetClientService.setupClient(TENANT_NAME))
      .isInstanceOf(KeycloakException.class)
      .hasMessage("Failed to assign a role: '%s' to a client: %s", "Password Reset", "password-reset-client")
      .hasCauseInstanceOf(InternalServerErrorException.class);

    var excludedFields = new String[] {"id", "attributes.client.secret.creation.time"};
    assertEqualsUsingRecursiveComparison(clientCaptor.getValue(), passwordResetClient(), excludedFields);

    verify(realmResource, atLeastOnce()).roles();
    verify(realmResource, atLeastOnce()).users();
    verify(realmResource, atLeastOnce()).clients();
  }

  @Test
  void setupClient_negative_failedToFindActionMapper() {
    when(clientSecretService.getOrCreateClientSecret(TENANT_NAME, CLIENT_ID)).thenReturn(CLIENT_SECRET);
    when(keycloakRealmSetupProperties.getPasswordResetClient()).thenReturn(passwordResetClientProperties());
    when(keycloakServerInfoService.getServerInfo()).thenReturn(new ServerInfoRepresentation());

    assertThatThrownBy(() -> passwordResetClientService.setupClient(TENANT_NAME))
      .isInstanceOf(EntityNotFoundException.class)
      .hasMessage("Mapper is not found by name: Password reset action mapper");
  }

  @Test
  void setupClient_negative_failedToRetrieveServerInfo() {
    when(clientSecretService.getOrCreateClientSecret(TENANT_NAME, CLIENT_ID)).thenReturn(CLIENT_SECRET);
    when(keycloakRealmSetupProperties.getPasswordResetClient()).thenReturn(passwordResetClientProperties());
    when(keycloakServerInfoService.getServerInfo()).thenThrow(new KeycloakException("Failed to retrieve server info"));

    assertThatThrownBy(() -> passwordResetClientService.setupClient(TENANT_NAME))
      .isInstanceOf(KeycloakException.class)
      .hasMessage("Failed to retrieve server info");
  }

  private static PasswordResetClientProperties passwordResetClientProperties() {
    var keycloakClientProperties = new PasswordResetClientProperties();
    keycloakClientProperties.setClientId(CLIENT_ID);
    keycloakClientProperties.setTokenLifespan(1000L);
    return keycloakClientProperties;
  }

  private static Headers<Object> responseHeaders() {
    var responseHeaders = new Headers<>();
    responseHeaders.add(LOCATION, format("https://keycloak:8443/admin/realms/%s/clients/%s", TENANT_NAME, CLIENT_UUID));
    return responseHeaders;
  }

  private static UserRepresentation serviceAccountUser() {
    var serviceAccountUser = new UserRepresentation();
    serviceAccountUser.setId(SERVICE_ACCOUNT_USER_ID);
    return serviceAccountUser;
  }

  private static RoleRepresentation passwordResetRole() {
    var roleRepresentation = new RoleRepresentation();
    roleRepresentation.setId(PASSWORD_RESET_ROLE_ID);
    roleRepresentation.setName("Password Reset");
    roleRepresentation.setDescription("Role for system user requests");
    return roleRepresentation;
  }

  private static ClientRepresentation passwordResetClient() {
    var keycloakClient = new ClientRepresentation();

    keycloakClient.setName(CLIENT_ID);
    keycloakClient.setClientId(CLIENT_ID);
    keycloakClient.setSecret(CLIENT_SECRET);
    keycloakClient.setDescription("Client for password reset operations");
    keycloakClient.setEnabled(true);
    keycloakClient.setFrontchannelLogout(true);
    keycloakClient.setAuthorizationServicesEnabled(false);
    keycloakClient.setClientAuthenticatorType("client-secret");
    keycloakClient.setAttributes(new ClientAttributes(false, false, 0L, true, false, true, 1000L, false).asMap());
    keycloakClient.setProtocolMappers(List.of(passwordResetProtocolMapper()));
    keycloakClient.setServiceAccountsEnabled(true);
    keycloakClient.setDirectAccessGrantsEnabled(true);
    keycloakClient.setRedirectUris(List.of("/*"));
    keycloakClient.setWebOrigins(List.of("/*"));

    return keycloakClient;
  }

  private static ServerInfoRepresentation serverInfo() {
    var serverInfo = new ServerInfoRepresentation();
    serverInfo.setProtocolMapperTypes(Map.of(
      "openid-connect", List.of(oidcSubMapperType(), passwordResetProtocolMapperType())));
    return serverInfo;
  }

  private static ProtocolMapperTypeRepresentation passwordResetProtocolMapperType() {
    var protocolMapperType = new ProtocolMapperTypeRepresentation();
    protocolMapperType.setId("script-pass-reset-action-mapper.js");
    protocolMapperType.setName("Password reset action mapper");
    protocolMapperType.setCategory("Token mapper");
    protocolMapperType.setHelpText("Password reset action mapper from a JS script");
    return protocolMapperType;
  }

  private static ProtocolMapperRepresentation passwordResetProtocolMapper() {
    var protocolMapper = new ProtocolMapperRepresentation();
    protocolMapper.setName("Password reset action mapper");
    protocolMapper.setProtocol("openid-connect");
    protocolMapper.setProtocolMapper("script-pass-reset-action-mapper.js");

    var protocolMapperConfig = new ProtocolMapperConfig(true, true, true,
      true, null, "passwordResetActionId", "String");
    protocolMapper.setConfig(protocolMapperConfig.asMap());

    return protocolMapper;
  }

  private static ProtocolMapperTypeRepresentation oidcSubMapperType() {
    var protocolMapperType = new ProtocolMapperTypeRepresentation();
    protocolMapperType.setId("oidc-sub-mapper");
    protocolMapperType.setName("Subject (sub)");
    protocolMapperType.setCategory("Token mapper");
    protocolMapperType.setHelpText("Add Subject (sub) claim");
    return protocolMapperType;
  }
}
