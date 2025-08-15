package org.folio.tm.integration.keycloak.service.clients;

import static jakarta.ws.rs.core.HttpHeaders.LOCATION;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.folio.tm.support.TestConstants.AUTH_TOKEN;
import static org.folio.tm.support.TestConstants.TENANT_NAME;
import static org.folio.tm.support.TestConstants.userIdProtocolMapper;
import static org.folio.tm.support.TestConstants.usernameProtocolMapper;
import static org.folio.tm.support.TestUtils.assertEqualsUsingRecursiveComparison;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import feign.FeignException.InternalServerError;
import jakarta.ws.rs.InternalServerErrorException;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.folio.test.types.UnitTest;
import org.folio.tm.integration.keycloak.ClientSecretService;
import org.folio.tm.integration.keycloak.KeycloakClient;
import org.folio.tm.integration.keycloak.configuration.KeycloakRealmSetupProperties;
import org.folio.tm.integration.keycloak.exception.KeycloakException;
import org.folio.tm.integration.keycloak.model.ClientAttributes;
import org.folio.tm.integration.keycloak.model.UserManagementPermission;
import org.folio.tm.integration.keycloak.model.UserManagementPermission.ScopePermission;
import org.folio.tm.support.TestUtils;
import org.jboss.resteasy.core.Headers;
import org.jboss.resteasy.core.ServerResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.AuthorizationResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.ScopePermissionResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.authorization.ClientPolicyRepresentation;
import org.keycloak.representations.idm.authorization.ScopePermissionRepresentation;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class ImpersonationClientServiceTest {

  private static final String IMPERSONATE_PERMISSION_ID = UUID.randomUUID().toString();
  private static final String CLIENT_ID = UUID.randomUUID().toString();
  private static final String REALM_MGMT_CLIENT_ID = UUID.randomUUID().toString();
  private static final String CLIENT_SECRET = UUID.randomUUID().toString();

  @InjectMocks private ImpersonationClientService impersonationClientService;

  @Mock(answer = RETURNS_DEEP_STUBS) private Keycloak keycloak;
  @Mock(answer = RETURNS_DEEP_STUBS) private RealmResource realmResource;
  @Mock(answer = RETURNS_DEEP_STUBS) private AuthorizationResource authResource;

  @Mock private KeycloakClient keycloakClient;
  @Mock private ClientSecretService clientSecretService;
  @Mock private KeycloakRealmSetupProperties keycloakRealmSetupProperties;
  @Mock private ScopePermissionResource scopePermissionResource;

  @Captor private ArgumentCaptor<ClientRepresentation> clientCaptor;
  @Captor private ArgumentCaptor<ClientPolicyRepresentation> policyCaptor;
  @Captor private ArgumentCaptor<ScopePermissionRepresentation> scopePermissionCaptor;

  @BeforeEach
  void setUp() {
    impersonationClientService.setKeycloak(keycloak);
    impersonationClientService.setClientSecretService(clientSecretService);
  }

  @AfterEach
  void tearDown() {
    TestUtils.verifyNoMoreInteractions(this);
  }

  @Test
  void setupClient_positive() {
    var userMgmtPermission = new UserManagementPermission(true);
    var clientResponse = new ServerResponse(null, 201, responseHeaders());

    when(keycloak.realm(TENANT_NAME)).thenReturn(realmResource);
    when(keycloakRealmSetupProperties.getImpersonationClient()).thenReturn("impersonation-client");
    when(clientSecretService.getOrCreateClientSecret(TENANT_NAME, "impersonation-client")).thenReturn(CLIENT_SECRET);
    when(keycloak.tokenManager().getAccessTokenString()).thenReturn(AUTH_TOKEN);
    when(realmResource.clients().create(clientCaptor.capture())).thenReturn(clientResponse);
    when(realmResource.clients().findByClientId("realm-management")).thenReturn(List.of(realmManagementClient()));
    when(realmResource.clients().get(REALM_MGMT_CLIENT_ID).authorization()).thenReturn(authResource);
    when(authResource.policies().client().create(policyCaptor.capture())).thenReturn(clientResponse);
    when(authResource.permissions().scope().findById(IMPERSONATE_PERMISSION_ID)).thenReturn(scopePermissionResource);
    doNothing().when(scopePermissionResource).update(scopePermissionCaptor.capture());
    when(keycloakClient.updateRealmUserManagementPermission(TENANT_NAME, userMgmtPermission, "Bearer " + AUTH_TOKEN))
      .thenReturn(userManagementPermission());

    var result = impersonationClientService.setupClient(TENANT_NAME);

    var impersonationClient = impersonationClient();
    var excludedFields = new String[] {"id", "attributes.client.secret.creation.time"};
    assertEqualsUsingRecursiveComparison(result, impersonationClient, excludedFields);
    assertEqualsUsingRecursiveComparison(clientCaptor.getValue(), impersonationClient, excludedFields);
    assertEqualsUsingRecursiveComparison(scopePermissionCaptor.getValue(), impersonationPermission());

    verify(keycloak, atLeastOnce()).realm(TENANT_NAME);
    verify(keycloak, atLeastOnce()).tokenManager();
    verify(realmResource, atLeastOnce()).clients();
    verify(authResource, atLeastOnce()).policies();
    verify(authResource, atLeastOnce()).permissions();
  }

  @Test
  void setupClient_negative_failedToUpdateRealmUserManagementPermission() {
    var userMgmtPermission = new UserManagementPermission(true);

    when(keycloak.tokenManager().getAccessTokenString()).thenReturn(AUTH_TOKEN);
    when(keycloakClient.updateRealmUserManagementPermission(TENANT_NAME, userMgmtPermission, "Bearer " + AUTH_TOKEN))
      .thenThrow(InternalServerError.class);

    assertThatThrownBy(() -> impersonationClientService.setupClient(TENANT_NAME))
      .isInstanceOf(KeycloakException.class)
      .hasMessage("Failed to enable user management in Keycloak for realm: %s", TENANT_NAME)
      .hasCauseInstanceOf(InternalServerError.class);

    verify(keycloak, atLeastOnce()).tokenManager();
  }

  @Test
  void setupClient_negative_failedToCreateClient() {

    var userMgmtPermission = new UserManagementPermission(true);
    var internalServerErrorResponse = new ServerResponse(null, 500, new Headers<>());

    when(keycloakRealmSetupProperties.getImpersonationClient()).thenReturn("impersonation-client");
    when(keycloak.realm(TENANT_NAME)).thenReturn(realmResource);
    when(keycloak.tokenManager().getAccessTokenString()).thenReturn(AUTH_TOKEN);
    when(realmResource.clients().create(clientCaptor.capture())).thenReturn(internalServerErrorResponse);
    when(clientSecretService.getOrCreateClientSecret(TENANT_NAME, "impersonation-client")).thenReturn(CLIENT_SECRET);
    when(keycloakClient.updateRealmUserManagementPermission(TENANT_NAME, userMgmtPermission, "Bearer " + AUTH_TOKEN))
      .thenReturn(userManagementPermission());

    assertThatThrownBy(() -> impersonationClientService.setupClient(TENANT_NAME))
      .isInstanceOf(KeycloakException.class)
      .hasMessage("Failed to create Keycloak client. "
        + "Details: id = null, status = 500, message = Internal Server Error");

    var excludedFields = new String[] {"id", "attributes.client.secret.creation.time"};
    assertEqualsUsingRecursiveComparison(clientCaptor.getValue(), impersonationClient(), excludedFields);

    verify(keycloak, atLeastOnce()).realm(TENANT_NAME);
    verify(keycloak, atLeastOnce()).tokenManager();
    verify(realmResource, atLeastOnce()).clients();
  }

  @Test
  void setupClient_negative_failedToCreateClientWithException() {
    var userMgmtPermission = new UserManagementPermission(true);

    when(keycloakRealmSetupProperties.getImpersonationClient()).thenReturn("impersonation-client");
    when(keycloak.realm(TENANT_NAME)).thenReturn(realmResource);
    when(keycloak.tokenManager().getAccessTokenString()).thenReturn(AUTH_TOKEN);
    when(realmResource.clients().create(clientCaptor.capture())).thenThrow(InternalServerErrorException.class);
    when(clientSecretService.getOrCreateClientSecret(TENANT_NAME, "impersonation-client")).thenReturn(CLIENT_SECRET);
    when(keycloakClient.updateRealmUserManagementPermission(TENANT_NAME, userMgmtPermission, "Bearer " + AUTH_TOKEN))
      .thenReturn(userManagementPermission());

    assertThatThrownBy(() -> impersonationClientService.setupClient(TENANT_NAME))
      .isInstanceOf(KeycloakException.class)
      .hasMessage("Failed to create a Keycloak client: impersonation-client")
      .isInstanceOf(RuntimeException.class);

    var excludedFields = new String[] {"id", "attributes.client.secret.creation.time"};
    assertEqualsUsingRecursiveComparison(clientCaptor.getValue(), impersonationClient(), excludedFields);

    verify(keycloak, atLeastOnce()).realm(TENANT_NAME);
    verify(keycloak, atLeastOnce()).tokenManager();
    verify(realmResource, atLeastOnce()).clients();
  }

  @Test
  void setupClient_negative_failedToFindRealmManagementClient() {
    var userMgmtPermission = new UserManagementPermission(true);
    var clientResponse = new ServerResponse(null, 201, responseHeaders());

    when(keycloak.realm(TENANT_NAME)).thenReturn(realmResource);
    when(keycloakRealmSetupProperties.getImpersonationClient()).thenReturn("impersonation-client");
    when(clientSecretService.getOrCreateClientSecret(TENANT_NAME, "impersonation-client")).thenReturn(CLIENT_SECRET);
    when(keycloak.tokenManager().getAccessTokenString()).thenReturn(AUTH_TOKEN);
    when(realmResource.clients().create(clientCaptor.capture())).thenReturn(clientResponse);
    when(realmResource.clients().findByClientId("realm-management")).thenReturn(Collections.emptyList());
    when(keycloakClient.updateRealmUserManagementPermission(TENANT_NAME, userMgmtPermission, "Bearer " + AUTH_TOKEN))
      .thenReturn(userManagementPermission());

    assertThatThrownBy(() -> impersonationClientService.setupClient(TENANT_NAME))
      .isInstanceOf(KeycloakException.class)
      .hasMessage("Failed to find realm management client for realm: %s", TENANT_NAME);

    var excludedFields = new String[] {"id", "attributes.client.secret.creation.time"};
    assertEqualsUsingRecursiveComparison(clientCaptor.getValue(), impersonationClient(), excludedFields);

    verify(keycloak, atLeastOnce()).realm(TENANT_NAME);
    verify(keycloak, atLeastOnce()).tokenManager();
    verify(realmResource, atLeastOnce()).clients();
  }

  @Test
  void setupClient_negative_failedToCreateImpersonationPolicy() {

    var userMgmtPermission = new UserManagementPermission(true);
    var clientResponse = new ServerResponse(null, 201, responseHeaders());
    var internalServerErrorResponse = new ServerResponse(null, 500, new Headers<>());

    when(keycloakRealmSetupProperties.getImpersonationClient()).thenReturn("impersonation-client");
    when(keycloak.realm(TENANT_NAME)).thenReturn(realmResource);
    when(clientSecretService.getOrCreateClientSecret(TENANT_NAME, "impersonation-client")).thenReturn(CLIENT_SECRET);
    when(keycloak.tokenManager().getAccessTokenString()).thenReturn(AUTH_TOKEN);
    when(realmResource.clients().create(clientCaptor.capture())).thenReturn(clientResponse);
    when(realmResource.clients().findByClientId("realm-management")).thenReturn(List.of(realmManagementClient()));
    when(realmResource.clients().get(REALM_MGMT_CLIENT_ID).authorization()).thenReturn(authResource);
    when(authResource.policies().client().create(policyCaptor.capture())).thenReturn(internalServerErrorResponse);
    when(keycloakClient.updateRealmUserManagementPermission(TENANT_NAME, userMgmtPermission, "Bearer " + AUTH_TOKEN))
      .thenReturn(userManagementPermission());

    assertThatThrownBy(() -> impersonationClientService.setupClient(TENANT_NAME))
      .isInstanceOf(KeycloakException.class)
      .hasMessage("Failed to create impersonation policy. Details: clientId = realm-management, "
        + "name = impersonation-policy, status = 500, message = Internal Server Error");

    var excludedFields = new String[] {"id", "attributes.client.secret.creation.time"};
    assertEqualsUsingRecursiveComparison(clientCaptor.getValue(), impersonationClient(), excludedFields);

    verify(keycloak, atLeastOnce()).realm(TENANT_NAME);
    verify(keycloak, atLeastOnce()).tokenManager();
    verify(realmResource, atLeastOnce()).clients();
    verify(authResource, atLeastOnce()).policies();
  }

  @Test
  void setupClient_negative_failedToCreateImpersonationPolicyWithException() {
    var userMgmtPermission = new UserManagementPermission(true);
    var clientResponse = new ServerResponse(null, 201, responseHeaders());

    when(keycloakRealmSetupProperties.getImpersonationClient()).thenReturn("impersonation-client");
    when(keycloak.realm(TENANT_NAME)).thenReturn(realmResource);
    when(clientSecretService.getOrCreateClientSecret(TENANT_NAME, "impersonation-client")).thenReturn(CLIENT_SECRET);
    when(keycloak.tokenManager().getAccessTokenString()).thenReturn(AUTH_TOKEN);
    when(realmResource.clients().create(clientCaptor.capture())).thenReturn(clientResponse);
    when(realmResource.clients().findByClientId("realm-management")).thenReturn(List.of(realmManagementClient()));
    when(realmResource.clients().get(REALM_MGMT_CLIENT_ID).authorization()).thenReturn(authResource);
    when(authResource.policies().client().create(policyCaptor.capture())).thenThrow(InternalServerErrorException.class);
    when(keycloakClient.updateRealmUserManagementPermission(TENANT_NAME, userMgmtPermission, "Bearer " + AUTH_TOKEN))
      .thenReturn(userManagementPermission());

    assertThatThrownBy(() -> impersonationClientService.setupClient(TENANT_NAME))
      .isInstanceOf(KeycloakException.class)
      .hasMessage("Failed to create impersonation policy for client: " + REALM_MGMT_CLIENT_ID)
      .isInstanceOf(RuntimeException.class);

    var excludedFields = new String[] {"id", "attributes.client.secret.creation.time"};
    assertEqualsUsingRecursiveComparison(clientCaptor.getValue(), impersonationClient(), excludedFields);

    verify(keycloak, atLeastOnce()).realm(TENANT_NAME);
    verify(keycloak, atLeastOnce()).tokenManager();
    verify(realmResource, atLeastOnce()).clients();
    verify(authResource, atLeastOnce()).policies();
  }

  @Test
  void setupClient_negative_failedToUpdateImpersonationPermission() {
    var userMgmtPermission = new UserManagementPermission(true);
    var clientResponse = new ServerResponse(null, 201, responseHeaders());

    when(keycloak.realm(TENANT_NAME)).thenReturn(realmResource);
    when(keycloakRealmSetupProperties.getImpersonationClient()).thenReturn("impersonation-client");
    when(clientSecretService.getOrCreateClientSecret(TENANT_NAME, "impersonation-client")).thenReturn(CLIENT_SECRET);
    when(keycloak.tokenManager().getAccessTokenString()).thenReturn(AUTH_TOKEN);
    when(realmResource.clients().create(clientCaptor.capture())).thenReturn(clientResponse);
    when(realmResource.clients().findByClientId("realm-management")).thenReturn(List.of(realmManagementClient()));
    when(realmResource.clients().get(REALM_MGMT_CLIENT_ID).authorization()).thenReturn(authResource);
    when(authResource.policies().client().create(policyCaptor.capture())).thenReturn(clientResponse);
    when(authResource.permissions().scope().findById(IMPERSONATE_PERMISSION_ID)).thenReturn(scopePermissionResource);
    doThrow(InternalServerErrorException.class).when(scopePermissionResource).update(scopePermissionCaptor.capture());
    when(keycloakClient.updateRealmUserManagementPermission(TENANT_NAME, userMgmtPermission, "Bearer " + AUTH_TOKEN))
      .thenReturn(userManagementPermission());

    assertThatThrownBy(() -> impersonationClientService.setupClient(TENANT_NAME))
      .isInstanceOf(KeycloakException.class)
      .hasMessage("Failed to update impersonation permission: %s", IMPERSONATE_PERMISSION_ID);

    var excludedFields = new String[] {"id", "attributes.client.secret.creation.time"};
    assertEqualsUsingRecursiveComparison(clientCaptor.getValue(), impersonationClient(), excludedFields);
    assertEqualsUsingRecursiveComparison(scopePermissionCaptor.getValue(), impersonationPermission(), excludedFields);

    verify(keycloak, atLeastOnce()).realm(TENANT_NAME);
    verify(keycloak, atLeastOnce()).tokenManager();
    verify(realmResource, atLeastOnce()).clients();
    verify(authResource, atLeastOnce()).policies();
    verify(authResource, atLeastOnce()).permissions();
  }

  private static ClientRepresentation realmManagementClient() {
    var clientRepresentation = new ClientRepresentation();
    clientRepresentation.setId(REALM_MGMT_CLIENT_ID);
    clientRepresentation.setClientId("realm-management");
    clientRepresentation.setName("realm-management");
    return clientRepresentation;
  }

  private static Headers<Object> responseHeaders() {
    var responseHeaders = new Headers<>();
    responseHeaders.add(LOCATION, format("https://keycloak:8443/admin/realms/%s/clients/%s", TENANT_NAME, CLIENT_ID));
    return responseHeaders;
  }

  private static ScopePermissionRepresentation impersonationPermission() {
    var scopePermission = new ScopePermissionRepresentation();
    scopePermission.setName("admin-impersonating.permission.users");
    scopePermission.setPolicies(Set.of("impersonation-policy"));
    return scopePermission;
  }

  private static ClientRepresentation impersonationClient() {
    var keycloakClient = new ClientRepresentation();

    keycloakClient.setName("impersonation-client");
    keycloakClient.setClientId("impersonation-client");
    keycloakClient.setSecret(CLIENT_SECRET);
    keycloakClient.setDescription("Client for impersonating user (used for scheduling jobs and in consortia)");
    keycloakClient.setEnabled(true);
    keycloakClient.setFrontchannelLogout(true);
    keycloakClient.setAuthorizationServicesEnabled(true);
    keycloakClient.setClientAuthenticatorType("client-secret");
    keycloakClient.setAttributes(new ClientAttributes(false, false, 0L, true, false, true, null, null).asMap());
    keycloakClient.setProtocolMappers(List.of(usernameProtocolMapper(), userIdProtocolMapper()));
    keycloakClient.setServiceAccountsEnabled(true);
    keycloakClient.setDirectAccessGrantsEnabled(true);
    keycloakClient.setRedirectUris(List.of("/*"));
    keycloakClient.setWebOrigins(List.of("/*"));

    return keycloakClient;
  }

  private static UserManagementPermission userManagementPermission() {
    var scopePermissions = new ScopePermission();
    scopePermissions.setImpersonate(IMPERSONATE_PERMISSION_ID);

    var userManagementPermission = new UserManagementPermission();
    userManagementPermission.setEnabled(true);
    userManagementPermission.setScopePermissions(scopePermissions);
    return userManagementPermission;
  }
}
