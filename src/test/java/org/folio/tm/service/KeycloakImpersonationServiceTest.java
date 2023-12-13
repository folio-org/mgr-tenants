package org.folio.tm.service;

import static org.folio.tm.integration.keycloak.model.ProtocolMapper.USER_ATTRIBUTE_MAPPER_TYPE;
import static org.folio.tm.integration.keycloak.model.ProtocolMapper.USER_PROPERTY_MAPPER_TYPE;
import static org.folio.tm.support.TestConstants.IMPERSONATE_CLIENT;
import static org.folio.tm.support.TestConstants.IMPERSONATE_POLICY_ID;
import static org.folio.tm.support.TestConstants.IMPERSONATE_SCOPE_ID;
import static org.folio.tm.support.TestConstants.REALM_MANAGEMENT_CLIENT;
import static org.folio.tm.support.TestConstants.REALM_MANAGEMENT_CLIENT_ID;
import static org.folio.tm.support.TestConstants.REALM_NAME;
import static org.folio.tm.support.TestConstants.SECRET;
import static org.folio.tm.support.TestConstants.authorizationClientPolicy;
import static org.folio.tm.support.TestConstants.authorizationImpersonationPermission;
import static org.folio.tm.support.TestConstants.clientDescriptor;
import static org.folio.tm.support.TestConstants.protocolMapper;
import static org.folio.tm.support.TestConstants.scopePermission;
import static org.folio.tm.support.TestConstants.userManagementPermission;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.folio.test.types.UnitTest;
import org.folio.tm.integration.keycloak.ClientSecretService;
import org.folio.tm.integration.keycloak.KeycloakClientService;
import org.folio.tm.integration.keycloak.KeycloakImpersonationService;
import org.folio.tm.integration.keycloak.KeycloakPermissionService;
import org.folio.tm.integration.keycloak.KeycloakPolicyService;
import org.folio.tm.integration.keycloak.configuration.KeycloakRealmSetupProperties;
import org.folio.tm.integration.keycloak.model.Client;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class KeycloakImpersonationServiceTest {

  @Mock private ClientSecretService clientSecretService;
  @Mock private KeycloakPolicyService policyService;
  @Mock private KeycloakClientService clientService;
  @Mock private KeycloakPermissionService permissionService;
  @Mock private KeycloakRealmSetupProperties properties;

  @InjectMocks private KeycloakImpersonationService impersonationService;

  @BeforeEach
  public void setup() {
    impersonationService =
      new KeycloakImpersonationService(clientSecretService, policyService, clientService, permissionService,
        properties);
  }

  @Test
  void setupImpersonationClient_positive() {
    var managementClient = client(REALM_MANAGEMENT_CLIENT, "realm-management client");
    managementClient.setId(REALM_MANAGEMENT_CLIENT_ID);
    var policy = authorizationClientPolicy();
    policy.setId(IMPERSONATE_POLICY_ID);

    var impersonationClient = client(IMPERSONATE_CLIENT, "client for impersonating user");
    impersonationClient.setProtocolMappers(List.of(
      protocolMapper(USER_PROPERTY_MAPPER_TYPE, "username", "username", "sub"),
      protocolMapper(USER_ATTRIBUTE_MAPPER_TYPE, "user_id mapper", "user_id", "user_id")
    ));

    when(properties.getImpersonationClient()).thenReturn(IMPERSONATE_CLIENT);
    when(permissionService.enableUserPermissionsInRealm(REALM_NAME))
      .thenReturn(userManagementPermission(scopePermission()));
    when(clientSecretService.getOrCreateClientSecret(REALM_NAME, IMPERSONATE_CLIENT)).thenReturn(SECRET);
    when(clientService.createClient(impersonationClient, REALM_NAME)).thenReturn(impersonationClient);
    when(clientService.findClientByClientId(REALM_NAME, REALM_MANAGEMENT_CLIENT)).thenReturn(managementClient);
    when(policyService.createClientPolicy(authorizationClientPolicy(), REALM_NAME, REALM_MANAGEMENT_CLIENT_ID))
      .thenReturn(policy);
    doNothing().when(permissionService).updatePermission(REALM_NAME, REALM_MANAGEMENT_CLIENT_ID, IMPERSONATE_SCOPE_ID,
      authorizationImpersonationPermission());

    impersonationService.setupImpersonationClient(REALM_NAME);

    verify(permissionService).enableUserPermissionsInRealm(REALM_NAME);
    verify(clientSecretService).getOrCreateClientSecret(REALM_NAME, IMPERSONATE_CLIENT);
    verify(clientService).createClient(impersonationClient, REALM_NAME);
    verify(clientService).findClientByClientId(REALM_NAME, REALM_MANAGEMENT_CLIENT);
  }

  private static Client client(String clientId, String desc) {
    return clientDescriptor(clientId, SECRET, desc);
  }
}
