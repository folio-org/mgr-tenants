package org.folio.tm.integration.keycloak;

import static java.lang.String.format;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.tm.integration.keycloak.KeycloakTemplate.KeycloakFunction;
import org.folio.tm.integration.keycloak.KeycloakTemplate.KeycloakMethod;
import org.folio.tm.integration.keycloak.model.AuthorizationPermission;
import org.folio.tm.integration.keycloak.model.UserManagementPermission;

@Log4j2
@RequiredArgsConstructor
public class KeycloakPermissionService {

  private final KeycloakClient keycloakClient;
  private final KeycloakTemplate template;

  public AuthorizationPermission createRolePermission(AuthorizationPermission permission, String realm,
                                                      String clientId) {
    return template.call(createPermission(permission, realm, clientId),
      () -> format("Failed to create authorization permission [%s] in client [%s]", permission.getName(), clientId));
  }

  private KeycloakFunction<AuthorizationPermission> createPermission(AuthorizationPermission permission, String realm,
                                                                                      String clientId) {
    return token -> {
      var res = keycloakClient.createAuthPermission(realm, clientId, permission, token);
      log.info("Keycloak System role permission created with id: {}", res.getId());

      return res;
    };
  }

  public UserManagementPermission enableUserPermissionsInRealm(String realm) {
    var userPermission = new UserManagementPermission(true);
    return template.call(enablePermissions(userPermission, realm),
      () -> format("Failed to enable users impersonation for realm [%s]", realm));
  }

  public void updatePermission(String realm, String clientId, String scopeId,
                               AuthorizationPermission permission) {
    template.call(updatePerms(realm, clientId, scopeId, permission),
      () -> format("Failed to update [%s] permission for client [%s]", permission.getName(), realm));
  }

  private KeycloakFunction<UserManagementPermission> enablePermissions(UserManagementPermission userPermission,
                                                                       String realm) {
    return token -> {
      var res = keycloakClient.enablePermissionForRealmClient(realm, userPermission, token);
      log.info("User impersonation feature has been enabled for realm: {}", realm);
      return res;
    };
  }

  private KeycloakMethod updatePerms(String realm, String clientId, String scopeId,
                                     AuthorizationPermission permission) {
    return token -> {
      keycloakClient.updatePermission(realm, clientId, scopeId, permission, token);
      log.info("{} permission was successfully updated for a client {}", permission.getName(), clientId);
    };
  }
}
