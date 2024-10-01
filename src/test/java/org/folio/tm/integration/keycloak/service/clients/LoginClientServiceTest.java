package org.folio.tm.integration.keycloak.service.clients;

import static jakarta.ws.rs.HttpMethod.DELETE;
import static jakarta.ws.rs.HttpMethod.GET;
import static jakarta.ws.rs.HttpMethod.OPTIONS;
import static jakarta.ws.rs.HttpMethod.PATCH;
import static jakarta.ws.rs.HttpMethod.POST;
import static jakarta.ws.rs.HttpMethod.PUT;
import static jakarta.ws.rs.core.HttpHeaders.LOCATION;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.folio.common.utils.CollectionUtils.mapItems;
import static org.folio.tm.support.TestConstants.TENANT_NAME;
import static org.folio.tm.support.TestConstants.userIdProtocolMapper;
import static org.folio.tm.support.TestConstants.usernameProtocolMapper;
import static org.folio.tm.support.TestUtils.OBJECT_MAPPER;
import static org.folio.tm.support.TestUtils.assertEqualsUsingRecursiveComparison;
import static org.keycloak.representations.idm.authorization.DecisionStrategy.AFFIRMATIVE;
import static org.keycloak.representations.idm.authorization.DecisionStrategy.UNANIMOUS;
import static org.keycloak.representations.idm.authorization.Logic.POSITIVE;
import static org.keycloak.representations.idm.authorization.PolicyEnforcementMode.ENFORCING;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.InternalServerErrorException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.folio.security.integration.keycloak.configuration.properties.KeycloakClientProperties;
import org.folio.test.types.UnitTest;
import org.folio.tm.integration.keycloak.ClientSecretService;
import org.folio.tm.integration.keycloak.configuration.KeycloakRealmSetupProperties;
import org.folio.tm.integration.keycloak.exception.KeycloakException;
import org.folio.tm.integration.keycloak.model.ClientAttributes;
import org.folio.tm.support.TestUtils;
import org.folio.tm.utils.JsonHelper;
import org.jboss.resteasy.core.Headers;
import org.jboss.resteasy.core.ServerResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.authorization.PolicyRepresentation;
import org.keycloak.representations.idm.authorization.ResourceServerRepresentation;
import org.keycloak.representations.idm.authorization.ScopeRepresentation;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class LoginClientServiceTest {

  private static final String CLIENT_ID = TENANT_NAME + "-test-app";
  private static final String CLIENT_UUID = UUID.randomUUID().toString();
  private static final String CLIENT_SECRET = UUID.randomUUID().toString();
  private static final List<String> EXPECTED_SCOPES = List.of(GET, POST, PUT, DELETE, PATCH, OPTIONS);

  @InjectMocks private LoginClientService loginClientService;
  @Mock private ClientSecretService clientSecretService;
  @Mock private KeycloakRealmSetupProperties keycloakRealmSetupProperties;
  @Spy private final JsonHelper jsonHelper = new JsonHelper(OBJECT_MAPPER);

  @Mock(answer = RETURNS_DEEP_STUBS) private Keycloak keycloak;
  @Mock(answer = RETURNS_DEEP_STUBS) private RealmResource realmResource;
  @Captor private ArgumentCaptor<ClientRepresentation> clientCaptor;

  @BeforeEach
  void setUp() {
    loginClientService.setKeycloak(keycloak);
    loginClientService.setClientSecretService(clientSecretService);
  }

  @AfterEach
  void tearDown() {
    TestUtils.verifyNoMoreInteractions(this);
  }

  @Test
  void setupClient_positive() {
    var clientResponse = new ServerResponse(null, 201, responseHeaders());

    when(keycloakRealmSetupProperties.getLoginClient()).thenReturn(loginClientProperties());
    when(clientSecretService.getOrCreateClientSecret(TENANT_NAME, CLIENT_ID)).thenReturn(CLIENT_SECRET);
    when(keycloak.realm(TENANT_NAME)).thenReturn(realmResource);
    when(realmResource.clients().create(clientCaptor.capture())).thenReturn(clientResponse);

    var result = loginClientService.setupClient(TENANT_NAME);

    var expectedLoginClient = loginClient();
    var excludedFields = new String[] {"id", "attributes.client.secret.creation.time"};
    assertEqualsUsingRecursiveComparison(result, expectedLoginClient, excludedFields);
    assertEqualsUsingRecursiveComparison(clientCaptor.getValue(), expectedLoginClient, excludedFields);

    verify(realmResource, atLeastOnce()).clients();
    verify(jsonHelper, times(4)).asJsonString(any());
  }

  @Test
  void setupClient_negative_failedToCreateClient() {
    var internalServerError = new ServerResponse(null, 500, new Headers<>());

    when(keycloakRealmSetupProperties.getLoginClient()).thenReturn(loginClientProperties());
    when(clientSecretService.getOrCreateClientSecret(TENANT_NAME, CLIENT_ID)).thenReturn(CLIENT_SECRET);
    when(keycloak.realm(TENANT_NAME)).thenReturn(realmResource);
    when(realmResource.clients().create(clientCaptor.capture())).thenReturn(internalServerError);

    assertThatThrownBy(() -> loginClientService.setupClient(TENANT_NAME))
      .isInstanceOf(KeycloakException.class)
      .hasMessage("Failed to create Keycloak client. "
        + "Details: id = null, status = 500, message = Internal Server Error");

    var excludedFields = new String[] {"id", "attributes.client.secret.creation.time"};
    assertEqualsUsingRecursiveComparison(clientCaptor.getValue(), loginClient(), excludedFields);
    verify(realmResource, atLeastOnce()).clients();
    verify(jsonHelper, times(4)).asJsonString(any());
  }

  @Test
  void setupClient_negative_failedToCreateClientWithException() {
    when(keycloakRealmSetupProperties.getLoginClient()).thenReturn(loginClientProperties());
    when(clientSecretService.getOrCreateClientSecret(TENANT_NAME, CLIENT_ID)).thenReturn(CLIENT_SECRET);
    when(keycloak.realm(TENANT_NAME)).thenReturn(realmResource);
    when(realmResource.clients().create(clientCaptor.capture())).thenThrow(InternalServerErrorException.class);

    assertThatThrownBy(() -> loginClientService.setupClient(TENANT_NAME))
      .isInstanceOf(KeycloakException.class)
      .hasMessage("Failed to create a Keycloak client: %s", CLIENT_ID)
      .hasCauseInstanceOf(InternalServerErrorException.class);

    var excludedFields = new String[] {"id", "attributes.client.secret.creation.time"};
    assertEqualsUsingRecursiveComparison(clientCaptor.getValue(), loginClient(), excludedFields);

    verify(realmResource, atLeastOnce()).clients();
    verify(jsonHelper, times(4)).asJsonString(any());
  }

  private static KeycloakClientProperties loginClientProperties() {
    var keycloakClientProperties = new KeycloakClientProperties();
    keycloakClientProperties.setClientId("-test-app");
    return keycloakClientProperties;
  }

  private static Headers<Object> responseHeaders() {
    var responseHeaders = new Headers<>();
    responseHeaders.add(LOCATION, format("https://keycloak:8443/admin/realms/%s/clients/%s", TENANT_NAME, CLIENT_UUID));
    return responseHeaders;
  }

  private static ClientRepresentation loginClient() {
    var keycloakClient = new ClientRepresentation();

    keycloakClient.setName(CLIENT_ID);
    keycloakClient.setClientId(CLIENT_ID);
    keycloakClient.setSecret(CLIENT_SECRET);
    keycloakClient.setDescription("Client for login operations");
    keycloakClient.setEnabled(true);
    keycloakClient.setFrontchannelLogout(true);
    keycloakClient.setAuthorizationServicesEnabled(true);
    keycloakClient.setAuthorizationSettings(authorizationSettings());
    keycloakClient.setClientAuthenticatorType("client-secret");
    keycloakClient.setAttributes(new ClientAttributes(false, false, 0L, true, false, null, null).asMap());
    keycloakClient.setProtocolMappers(List.of(usernameProtocolMapper(), userIdProtocolMapper()));
    keycloakClient.setServiceAccountsEnabled(true);
    keycloakClient.setDirectAccessGrantsEnabled(true);
    keycloakClient.setRedirectUris(List.of("/*"));
    keycloakClient.setWebOrigins(List.of("/*"));

    return keycloakClient;
  }

  private static ResourceServerRepresentation authorizationSettings() {
    var resourceServer = new ResourceServerRepresentation();
    resourceServer.setDecisionStrategy(AFFIRMATIVE);
    resourceServer.setPolicyEnforcementMode(ENFORCING);
    resourceServer.setAllowRemoteResourceManagement(true);
    resourceServer.setScopes(mapItems(EXPECTED_SCOPES, LoginClientServiceTest::scope));
    resourceServer.setPolicies(List.of(systemRolePolicy(), passwordResetPolicy(), systemUserPermission()));

    return resourceServer;
  }

  private static PolicyRepresentation systemRolePolicy() {
    var policyRepresentation = new PolicyRepresentation();
    policyRepresentation.setType("role");
    policyRepresentation.setName("System role policy");
    policyRepresentation.setLogic(POSITIVE);
    policyRepresentation.setDecisionStrategy(UNANIMOUS);
    policyRepresentation.setConfig(Map.of("roles", "[{\"id\":\"System\",\"required\":false}]"));

    return policyRepresentation;
  }

  private static PolicyRepresentation passwordResetPolicy() {
    var policyRepresentation = new PolicyRepresentation();
    policyRepresentation.setType("role");
    policyRepresentation.setName("Password Reset policy");
    policyRepresentation.setLogic(POSITIVE);
    policyRepresentation.setDecisionStrategy(UNANIMOUS);
    policyRepresentation.setConfig(Map.of("roles", "[{\"id\":\"Password Reset\",\"required\":false}]"));

    return policyRepresentation;
  }

  private static PolicyRepresentation systemUserPermission() {
    var policyRepresentation = new PolicyRepresentation();
    policyRepresentation.setType("scope");
    policyRepresentation.setName("System role permission");
    policyRepresentation.setLogic(POSITIVE);
    policyRepresentation.setDecisionStrategy(UNANIMOUS);
    policyRepresentation.setConfig(Map.of(
      "scopes", "[\"GET\",\"POST\",\"PUT\",\"DELETE\",\"PATCH\",\"OPTIONS\"]",
      "applyPolicies", "[\"System role policy\"]"));
    return policyRepresentation;
  }

  private static ScopeRepresentation scope(String name) {
    var scopeRepresentation = new ScopeRepresentation();
    scopeRepresentation.setName(name);
    scopeRepresentation.setIconUri(name);
    scopeRepresentation.setDisplayName(name);
    return scopeRepresentation;
  }
}
