package org.folio.tm.integration.keycloak;

import static org.folio.tm.integration.keycloak.utils.KeycloakClientUtils.buildClient;
import static org.folio.tm.integration.keycloak.utils.KeycloakClientUtils.folioUserTokenMappers;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.tm.integration.keycloak.configuration.KeycloakRealmSetupProperties;
import org.folio.tm.integration.keycloak.model.AuthorizationClientPolicy;
import org.folio.tm.integration.keycloak.model.AuthorizationPermission;
import org.folio.tm.integration.keycloak.model.Client;

@Log4j2
@RequiredArgsConstructor
public class KeycloakImpersonationService {

  private static final String IMPERSONATE_POLICY = "impersonation-policy";
  private static final String CLIENT_POLICY_TYPE = "client";
  private static final String REALM_MANAGEMENT_CLIENT = "realm-management";
  private static final String ADMIN_IMPERSONATING_PERMISSION = "admin-impersonating.permission.users";

  private final ClientSecretService clientSecretService;
  private final KeycloakPolicyService policyService;
  private final KeycloakClientService clientService;
  private final KeycloakPermissionService permissionService;
  private final KeycloakRealmSetupProperties properties;

  public void setupImpersonationClient(String realm) {
    var userManagementPermission = permissionService.enableUserPermissionsInRealm(realm);
    var impersonationClient = createImpersonationClient(realm);
    var managementClient = findManagementClientId(realm);
    var clientPolicy = createImpersonationPolicy(realm, impersonationClient.getClientId(),
      managementClient.getId());
    updatePermission(realm, managementClient.getId(), userManagementPermission.getScopePermissions().getImpersonate(),
      clientPolicy.getId());
  }

  private Client createImpersonationClient(String realm) {
    var impersonationClient = properties.getImpersonationClient();
    var secret = clientSecretService.getOrCreateClientSecret(realm, impersonationClient);
    var description = "client for impersonating user";

    var mappers = folioUserTokenMappers();
    var client = buildClient(impersonationClient, secret, description, mappers, true, true);

    return clientService.createClient(client, realm);
  }

  private AuthorizationClientPolicy createImpersonationPolicy(String realm, String clientId,
                                                              String managementClientId) {
    var policy = new AuthorizationClientPolicy();
    policy.setName(IMPERSONATE_POLICY);
    policy.setClients(List.of(clientId));
    policy.setType(CLIENT_POLICY_TYPE);
    return policyService.createClientPolicy(policy, realm, managementClientId);
  }

  private Client findManagementClientId(String realm) {
    return clientService.findClientByClientId(realm, REALM_MANAGEMENT_CLIENT);
  }

  private void updatePermission(String realm, String managementClientId, String scopeId, String policyId) {
    var permission = AuthorizationPermission.builder()
      .name(ADMIN_IMPERSONATING_PERMISSION)
      .policies(List.of(policyId))
      .build();
    permissionService.updatePermission(realm, managementClientId, scopeId, permission);
  }
}
