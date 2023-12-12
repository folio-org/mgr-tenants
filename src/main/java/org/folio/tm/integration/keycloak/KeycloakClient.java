package org.folio.tm.integration.keycloak;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED_VALUE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.util.List;
import java.util.Map;
import org.folio.security.integration.keycloak.model.TokenResponse;
import org.folio.tm.integration.keycloak.model.AuthorizationClientPolicy;
import org.folio.tm.integration.keycloak.model.AuthorizationPermission;
import org.folio.tm.integration.keycloak.model.AuthorizationRolePolicy;
import org.folio.tm.integration.keycloak.model.AuthorizationScope;
import org.folio.tm.integration.keycloak.model.Client;
import org.folio.tm.integration.keycloak.model.Realm;
import org.folio.tm.integration.keycloak.model.Role;
import org.folio.tm.integration.keycloak.model.ServerInfo;
import org.folio.tm.integration.keycloak.model.Strategy;
import org.folio.tm.integration.keycloak.model.User;
import org.folio.tm.integration.keycloak.model.UserManagementPermission;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

public interface KeycloakClient {

  @PostMapping(value = "/realms/master/protocol/openid-connect/token", consumes = APPLICATION_FORM_URLENCODED_VALUE)
  TokenResponse login(@RequestBody Map<String, ?> loginRequest);

  @PostMapping(value = "/admin/realms", consumes = APPLICATION_JSON_VALUE)
  ResponseEntity<Void> createRealm(@RequestBody Realm realm,
                                   @RequestHeader(AUTHORIZATION) String token);

  @GetMapping(value = "/admin/realms/{realm}", consumes = APPLICATION_JSON_VALUE)
  Realm getRealm(@PathVariable("realm") String realmName,
                 @RequestHeader(AUTHORIZATION) String token);

  @DeleteMapping(value = "/admin/realms/{realmId}", consumes = APPLICATION_JSON_VALUE)
  void deleteRealm(@PathVariable("realmId") String realmId,
                   @RequestHeader(AUTHORIZATION) String token);

  @PutMapping(value = "/admin/realms/{realmId}", consumes = APPLICATION_JSON_VALUE)
  void updateRealm(@PathVariable("realmId") String realmId,
                   @RequestBody Realm realm,
                   @RequestHeader(AUTHORIZATION) String token);

  @GetMapping(value = "/admin/serverinfo", produces = APPLICATION_JSON_VALUE)
  ServerInfo getServerInfo(@RequestHeader(AUTHORIZATION) String token);

  @PostMapping(value = "/admin/realms/{realmId}/clients", consumes = APPLICATION_JSON_VALUE)
  ResponseEntity<Void> createClient(@PathVariable("realmId") String realmId,
                                    @RequestBody Client client,
                                    @RequestHeader(AUTHORIZATION) String token);

  @GetMapping(value = "/admin/realms/{realmId}/clients?clientId={clientId}", produces = APPLICATION_JSON_VALUE)
  List<Client> getClientsByClientId(@PathVariable("realmId") String realmId,
                                    @PathVariable("clientId") String clientId,
                                    @RequestHeader(AUTHORIZATION) String token);

  @DeleteMapping(value = "/admin/realms/{realmId}/clients/{id}")
  void deleteClient(@PathVariable("realmId") String realmId,
                    @PathVariable("id") String id,
                    @RequestHeader(AUTHORIZATION) String token);

  @PostMapping(value = "/admin/realms/{realmId}/roles", consumes = APPLICATION_JSON_VALUE)
  void createRole(@PathVariable("realmId") String realmId,
                  @RequestBody Role role,
                  @RequestHeader(AUTHORIZATION) String token);

  @GetMapping(value = "/admin/realms/{realmId}/roles/{name}", produces = APPLICATION_JSON_VALUE)
  Role getRoleByName(@PathVariable("realmId") String realmId,
                     @PathVariable("name") String roleName,
                     @RequestHeader(AUTHORIZATION) String token);

  @PostMapping(value = "/admin/realms/{realmId}/users/{userId}/role-mappings/realm",
               consumes = APPLICATION_JSON_VALUE)
  void assignRolesToUser(@PathVariable("realmId") String realmId,
                         @PathVariable("userId") String userId,
                         @RequestBody List<Role> roles,
                         @RequestHeader(AUTHORIZATION) String token);

  @GetMapping(value = "/admin/realms/{realmId}/clients/{id}/service-account-user", produces = APPLICATION_JSON_VALUE)
  User getServiceAccountUser(@PathVariable("realmId") String realmId,
                             @PathVariable("id") String id,
                             @RequestHeader(AUTHORIZATION) String token);

  @PostMapping(value = "/admin/realms/{realmId}/clients/{clientId}/authz/resource-server/scope",
               consumes = APPLICATION_JSON_VALUE)
  AuthorizationScope createAuthorizationScope(@PathVariable("realmId") String realmId,
                                              @PathVariable("clientId") String clientId,
                                              @RequestBody AuthorizationScope authorizationScope,
                                              @RequestHeader(AUTHORIZATION) String token);

  @PostMapping(value = "/admin/realms/{realmId}/clients/{clientId}/authz/resource-server/policy/role",
               consumes = APPLICATION_JSON_VALUE)
  AuthorizationRolePolicy createRolePolicy(@PathVariable("realmId") String realmId,
                                           @PathVariable("clientId") String clientId,
                                           @RequestBody AuthorizationRolePolicy authorizationRolePolicy,
                                           @RequestHeader(AUTHORIZATION) String token);

  @PostMapping(value = "/admin/realms/{realmId}/clients/{clientId}/authz/resource-server/policy/client",
               consumes = APPLICATION_JSON_VALUE)
  AuthorizationClientPolicy createClientPolicy(@PathVariable("realmId") String realmId,
                                               @PathVariable("clientId") String clientId,
                                               @RequestBody AuthorizationClientPolicy authorizationPolicy,
                                               @RequestHeader(AUTHORIZATION) String token);

  @PostMapping(value = "/admin/realms/{realmId}/clients/{clientId}/authz/resource-server/permission/scope",
               consumes = APPLICATION_JSON_VALUE)
  AuthorizationPermission createAuthPermission(@PathVariable("realmId") String realmId,
                                               @PathVariable("clientId") String clientId,
                                               @RequestBody AuthorizationPermission authorizationPermission,
                                               @RequestHeader(AUTHORIZATION) String token);

  @PutMapping(value = "/admin/realms/{realmId}/clients/{clientId}/authz/resource-server",
              consumes = APPLICATION_JSON_VALUE)
  void updateDecisionStrategy(@PathVariable("realmId") String realmId,
                              @PathVariable("clientId") String clientId,
                              @RequestBody Strategy strategy,
                              @RequestHeader(AUTHORIZATION) String token);

  @PutMapping(value = "/admin/realms/{realm}/users-management-permissions", consumes = APPLICATION_JSON_VALUE)
  UserManagementPermission enablePermissionForRealmClient(@PathVariable("realm") String realm,
                                                          @RequestBody UserManagementPermission userPermission,
                                                          @RequestHeader(AUTHORIZATION) String token);

  @PutMapping(value = "/admin/realms/{realm}/clients/{clientId}/authz/resource-server"
    + "/permission/scope/{scopeId}",
              consumes = APPLICATION_JSON_VALUE)
  void updatePermission(@PathVariable("realm") String realm,
                        @PathVariable("clientId") String clientId,
                        @PathVariable("scopeId") String scopeId,
                        @RequestBody AuthorizationPermission authorizationPermission,
                        @RequestHeader(AUTHORIZATION) String token);
}
