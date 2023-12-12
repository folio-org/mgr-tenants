package org.folio.tm.integration.keycloak;

import static java.lang.String.format;

import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.folio.tm.integration.keycloak.KeycloakTemplate.KeycloakFunction;
import org.folio.tm.integration.keycloak.model.AuthorizationScope;

@RequiredArgsConstructor
public class KeycloakAuthScopeService {

  private final KeycloakClient keycloakClient;
  private final KeycloakTemplate template;

  public List<AuthorizationScope> createAuthScopes(String realm, String clientId, List<String> scopes) {
    return scopes.stream().map(s -> {
      var authScope = AuthorizationScope.builder()
        .name(s)
        .displayName(s)
        .iconUri(s)
        .build();

      return createAuthScope(realm, clientId, authScope);
    }).collect(Collectors.toList());
  }

  public AuthorizationScope createAuthScope(String realm, String clientId,
                                            AuthorizationScope authScope) {
    return template.call(createScope(realm, clientId, authScope),
      () -> format("Failed to create authorization scope [%s]", authScope.getName()));
  }

  private KeycloakFunction<AuthorizationScope> createScope(String realm, String clientId,
                                                           AuthorizationScope authScope) {
    return token -> keycloakClient.createAuthorizationScope(realm, clientId, authScope, token);
  }
}
