package org.folio.tm.integration.keycloak;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import org.folio.tm.integration.keycloak.model.UserManagementPermission;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PutExchange;

@HttpExchange
public interface KeycloakClient {

  @PutExchange(value = "/admin/realms/{realm}/users-management-permissions", contentType = APPLICATION_JSON_VALUE)
  UserManagementPermission updateRealmUserManagementPermission(
    @PathVariable("realm") String realm,
    @RequestBody UserManagementPermission userPermission,
    @RequestHeader(AUTHORIZATION) String token);
}
