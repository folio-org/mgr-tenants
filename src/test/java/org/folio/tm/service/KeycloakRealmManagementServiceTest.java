package org.folio.tm.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.tm.integration.keycloak.KeycloakUtils.SCOPES;
import static org.folio.tm.integration.keycloak.model.Client.OPENID_CONNECT_PROTOCOL;
import static org.folio.tm.integration.keycloak.model.ProtocolMapper.USER_ATTRIBUTE_MAPPER_TYPE;
import static org.folio.tm.integration.keycloak.model.ProtocolMapper.USER_PROPERTY_MAPPER_TYPE;
import static org.folio.tm.support.TestConstants.LOGIN_DESC;
import static org.folio.tm.support.TestConstants.M2M_DESC;
import static org.folio.tm.support.TestConstants.PASSWORD_RESET_ACTION_MAPPER;
import static org.folio.tm.support.TestConstants.PASSWORD_RESET_CLIENT;
import static org.folio.tm.support.TestConstants.POLICY_ID;
import static org.folio.tm.support.TestConstants.REALM_NAME;
import static org.folio.tm.support.TestConstants.ROLE_ID;
import static org.folio.tm.support.TestConstants.authorizationPermission;
import static org.folio.tm.support.TestConstants.authorizationPolicy;
import static org.folio.tm.support.TestConstants.authorizationScope;
import static org.folio.tm.support.TestConstants.protocolMapper;
import static org.folio.tm.support.TestConstants.realmDescriptor;
import static org.folio.tm.support.TestConstants.roleDescriptor;
import static org.folio.tm.support.TestConstants.serverInfo;
import static org.folio.tm.support.TestConstants.tenant;
import static org.folio.tm.support.TestConstants.userDescriptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.folio.security.integration.keycloak.configuration.properties.KeycloakClientProperties;
import org.folio.test.types.UnitTest;
import org.folio.tm.integration.keycloak.ClientSecretService;
import org.folio.tm.integration.keycloak.KeycloakAuthScopeService;
import org.folio.tm.integration.keycloak.KeycloakClientService;
import org.folio.tm.integration.keycloak.KeycloakImpersonationService;
import org.folio.tm.integration.keycloak.KeycloakPermissionService;
import org.folio.tm.integration.keycloak.KeycloakPolicyService;
import org.folio.tm.integration.keycloak.KeycloakRealmManagementService;
import org.folio.tm.integration.keycloak.KeycloakRealmService;
import org.folio.tm.integration.keycloak.KeycloakRoleService;
import org.folio.tm.integration.keycloak.configuration.KeycloakRealmSetupProperties;
import org.folio.tm.integration.keycloak.configuration.PasswordResetClientProperties;
import org.folio.tm.integration.keycloak.model.AuthorizationScope;
import org.folio.tm.integration.keycloak.model.Client;
import org.folio.tm.integration.keycloak.model.ProtocolMapper;
import org.folio.tm.integration.keycloak.model.ProtocolMapper.Config;
import org.folio.tm.support.TestConstants;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class KeycloakRealmManagementServiceTest {

  @Mock private KeycloakRealmService keycloakRealmService;
  @Mock private KeycloakClientService keycloakClientService;
  @Mock private KeycloakRoleService keycloakRoleService;
  @Mock private KeycloakPolicyService policyService;
  @Mock private KeycloakPermissionService permissionService;
  @Mock private KeycloakAuthScopeService authScopeService;
  @Mock private KeycloakRealmSetupProperties keycloakRealmSetupProperties;
  @Mock private ClientSecretService clientSecretService;
  @Mock private KeycloakImpersonationService impersonationService;

  @Captor private ArgumentCaptor<Client> clientCaptor;

  @InjectMocks private KeycloakRealmManagementService keycloakRealmManagementService;

  @Test
  void setupModuleClient_positive() {
    var m2mProperties = keycloakClientProperties("m2m-client");
    var clientId = m2mProperties.getClientId();
    var clientSecret = "m2m-secret";

    var m2mClient = client(clientId, clientId, clientSecret, M2M_DESC);

    var role = roleDescriptor();
    var user = userDescriptor();

    when(keycloakRealmSetupProperties.getM2mClient()).thenReturn(m2mProperties);
    when(keycloakClientService.createClient(any(), eq(REALM_NAME))).thenReturn(m2mClient);
    when(keycloakClientService.getClientServiceAccountUser(m2mClient, REALM_NAME)).thenReturn(user);
    doNothing().when(keycloakRoleService).assignRole(role, user, REALM_NAME);
    when(clientSecretService.getOrCreateClientSecret(REALM_NAME, clientId)).thenReturn(clientSecret);

    keycloakRealmManagementService.setupModuleClient(REALM_NAME, role);

    verify(keycloakClientService).createClient(any(), eq(REALM_NAME));
    verify(keycloakClientService).getClientServiceAccountUser(m2mClient, REALM_NAME);
    verify(keycloakRoleService).assignRole(role, user, REALM_NAME);
  }

  @Test
  void setupLoginClient_positive() {
    var loginProperties = keycloakClientProperties("-application");
    var clientId = REALM_NAME + loginProperties.getClientId();
    var clientSecret = "login-secret";

    var loginClient = client(REALM_NAME + "-application", clientId, clientSecret, LOGIN_DESC);
    loginClient.setProtocolMappers(List.of(
      protocolMapper(USER_PROPERTY_MAPPER_TYPE, "username", "username", "sub"),
      protocolMapper(USER_ATTRIBUTE_MAPPER_TYPE, "user_id mapper", "user_id", "user_id")
    ));

    when(keycloakRealmSetupProperties.getLoginClient()).thenReturn(loginProperties);
    when(keycloakClientService.createClient(any(), eq(REALM_NAME))).thenReturn(loginClient);
    when(clientSecretService.getOrCreateClientSecret(REALM_NAME, clientId)).thenReturn(clientSecret);

    var policy = authorizationPolicy();
    when(policyService.createRolePolicy(policy, REALM_NAME, loginClient.getId()))
      .thenReturn(authorizationPolicy(POLICY_ID));

    var scopes = SCOPES.stream().map(s -> authorizationScope(s)).collect(Collectors.toList());
    var scopeIds = scopes.stream().map(AuthorizationScope::getId).collect(Collectors.toList());

    when(authScopeService.createAuthScopes(eq(REALM_NAME), eq(loginClient.getId()), any()))
      .thenReturn(scopes);

    var permission = authorizationPermission();
    permission.setScopes(scopeIds);

    when(permissionService.createRolePermission(permission, REALM_NAME, loginClient.getId()))
      .thenReturn(permission);

    var sysRole = roleDescriptor(ROLE_ID);
    var passwordResetRole = roleDescriptor(UUID.randomUUID().toString());

    keycloakRealmManagementService.setupLoginClient(REALM_NAME, sysRole, passwordResetRole);

    verify(keycloakClientService).createClient(loginClient, REALM_NAME);
    verify(policyService).createRolePolicy(policy, REALM_NAME, loginClient.getId());
    verify(authScopeService).createAuthScopes(eq(REALM_NAME), eq(loginClient.getId()), eq(SCOPES));
    verify(permissionService).createRolePermission(permission, REALM_NAME, loginClient.getId());
  }

  @Test
  void setupPasswordResetClient_positive() {
    var clientProperties = passwordResetClientProperties(PASSWORD_RESET_CLIENT);
    var clientId = clientProperties.getClientId();
    var clientSecret = "superSecret";

    var role = roleDescriptor();
    var user = userDescriptor();

    var serverInfo = serverInfo();

    when(keycloakRealmService.getServerInfo()).thenReturn(serverInfo);
    when(keycloakRealmSetupProperties.getPasswordResetClient()).thenReturn(clientProperties);
    when(keycloakClientService.getClientServiceAccountUser(any(Client.class), eq(REALM_NAME))).thenReturn(user);
    doNothing().when(keycloakRoleService).assignRole(role, user, REALM_NAME);
    when(clientSecretService.getOrCreateClientSecret(REALM_NAME, clientId)).thenReturn(clientSecret);
    when(keycloakClientService.createClient(any(Client.class), eq(REALM_NAME))).thenAnswer(i -> i.getArguments()[0]);

    keycloakRealmManagementService.setupPasswordResetClient(REALM_NAME, role);

    verify(keycloakClientService).createClient(clientCaptor.capture(), eq(REALM_NAME));
    verify(keycloakClientService).getClientServiceAccountUser(any(Client.class), eq(REALM_NAME));
    verify(keycloakRoleService).assignRole(role, user, REALM_NAME);

    var expectedClient = resetClient(clientId, clientSecret, clientProperties.getTokenLifespan());

    var actualClient = clientCaptor.getValue();
    assertThat(actualClient).usingRecursiveComparison().ignoringFields("attributes.clientSecretCreationTime")
      .isEqualTo(expectedClient);
  }

  @Test
  void setupImpersonationClient_positive() {
    doNothing().when(impersonationService).setupImpersonationClient(REALM_NAME);
    keycloakRealmManagementService.setupImpersonationClient(REALM_NAME);
    verify(impersonationService).setupImpersonationClient(REALM_NAME);
  }

  @Test
  void createRealm_positive() {
    var tenant = tenant();
    var realm = realmDescriptor();
    when(keycloakRealmService.createRealm(tenant)).thenReturn(realm);
    var result = keycloakRealmManagementService.createRealm(tenant);
    assertThat(result).isEqualTo(realm);
  }

  @Test
  void updateRealm_positive() {
    var tenant = tenant();
    var realm = realmDescriptor();
    when(keycloakRealmService.updateRealm(tenant)).thenReturn(realm);
    var result = keycloakRealmManagementService.updateRealm(tenant);
    assertThat(result).isEqualTo(realm);
  }

  @Test
  void destroyRealm_positive() {
    doNothing().when(keycloakRealmService).deleteRealm(REALM_NAME);
    keycloakRealmManagementService.destroyRealm(REALM_NAME);
    verify(keycloakRealmService).deleteRealm(REALM_NAME);
  }

  private KeycloakClientProperties keycloakClientProperties(String clientId) {
    var loginProperties = new KeycloakClientProperties();
    loginProperties.setClientId(clientId);
    return loginProperties;
  }

  private Client resetClient(String clientId, String clientSecret, Long tokenLifespan) {
    var expectedClient = client(clientId, clientId, clientSecret, "Client for password reset operations");
    expectedClient.setDirectAccessGrantsEnabled(true);
    expectedClient.setAuthorizationServicesEnabled(false);
    expectedClient.getAttributes().setAccessTokenLifeSpan(tokenLifespan);
    expectedClient.getAttributes().setUseRefreshTokens(false);
    expectedClient.setProtocolMappers(List.of(resetActionIdMapper()));
    return expectedClient;
  }

  private Client client(String name, String clientId, String secret, String desc) {
    return TestConstants.clientDescriptor(name, clientId, secret, desc);
  }

  private static PasswordResetClientProperties passwordResetClientProperties(String clientId) {
    var properties = new PasswordResetClientProperties();
    properties.setClientId(clientId);
    properties.setTokenLifespan(60L);
    return properties;
  }

  private static ProtocolMapper resetActionIdMapper() {
    var mapper = new ProtocolMapper();
    mapper.setName(PASSWORD_RESET_ACTION_MAPPER);
    mapper.setMapper(PASSWORD_RESET_ACTION_MAPPER);
    mapper.setProtocol(OPENID_CONNECT_PROTOCOL);
    mapper.setConfig(Config.of(true, true, true, null, "passwordResetActionId", "String"));
    return mapper;
  }
}
