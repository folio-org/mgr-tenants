package org.folio.tm.integration.keycloak;

import static java.lang.String.format;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.tm.integration.keycloak.KeycloakTemplate.KeycloakFunction;
import org.folio.tm.integration.keycloak.KeycloakTemplate.KeycloakMethod;
import org.folio.tm.integration.keycloak.model.Role;
import org.folio.tm.integration.keycloak.model.User;

@Log4j2
@RequiredArgsConstructor
public class KeycloakRoleService {

  private final KeycloakClient keycloakClient;
  private final KeycloakTemplate template;

  public Role createRole(Role role, String realm) {
    return template.call(create(role, realm),
      () -> format("Failed to create role [%s] in realm [%s]", role.getName(), realm));
  }

  public Role createRole(String name, String desc, String realm) {
    var role = Role.builder()
      .name(name)
      .description(desc)
      .build();

    return createRole(role, realm);
  }

  public void assignRole(Role role, User user, String realm) {
    template.call(assign(role, user, realm),
      () -> format("Failed to assign role [%s] to user [%s] in realm [%s]", role.getName(), user.getUserName(), realm));
  }

  private KeycloakFunction<Role> create(Role role, String realm) {
    return token -> {
      keycloakClient.createRole(realm, role, token);

      // POST /{realm}/roles returns Location header with name but not id
      // additional request to obtain new role with id
      var res = keycloakClient.getRoleByName(realm, role.getName(), token);

      log.info("Keycloak role created with id: {}", res.getId());

      return res;
    };
  }

  public Role getRoleByName(String realm, String roleName) {
    return template.call(getRole(realm, roleName),
      () -> format("Failed to get role [%s]", roleName));
  }

  private KeycloakFunction<Role> getRole(String realm, String roleName) {
    return token -> keycloakClient.getRoleByName(realm, roleName, token);
  }

  private KeycloakMethod assign(Role role, User user, String realm) {
    return token -> {
      keycloakClient.assignRolesToUser(realm, user.getId(), List.of(role), token);

      log.info("Keycloak role assigned to user: role = {}, user = {}, realm = {}", role.getName(), user.getUserName(),
        realm);
    };
  }
}
