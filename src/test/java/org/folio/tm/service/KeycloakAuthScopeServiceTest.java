package org.folio.tm.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.folio.tm.support.TestConstants.AUTH_TOKEN;
import static org.folio.tm.support.TestConstants.CLIENT_ID;
import static org.folio.tm.support.TestConstants.REALM_NAME;
import static org.folio.tm.support.TestConstants.SCOPE_GET;
import static org.folio.tm.support.TestConstants.authorizationScope;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import feign.FeignException;
import java.util.List;
import org.folio.test.types.UnitTest;
import org.folio.tm.integration.keycloak.KeycloakAuthScopeService;
import org.folio.tm.integration.keycloak.KeycloakClient;
import org.folio.tm.integration.keycloak.KeycloakTemplate;
import org.folio.tm.integration.keycloak.TokenService;
import org.folio.tm.integration.keycloak.exception.KeycloakException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class KeycloakAuthScopeServiceTest {

  @Mock private KeycloakClient keycloakClient;
  @Mock private TokenService tokenService;
  @InjectMocks private KeycloakTemplate keycloakTemplate;
  private KeycloakAuthScopeService keycloakAuthScopeService;

  @BeforeEach
  void setUp() {
    keycloakAuthScopeService = new KeycloakAuthScopeService(keycloakClient, keycloakTemplate);
  }

  @Test
  void createScope_positive() {
    var scope = authorizationScope();

    when(tokenService.issueToken()).thenReturn(AUTH_TOKEN);
    when(keycloakClient.createAuthorizationScope(REALM_NAME, CLIENT_ID, scope, AUTH_TOKEN))
      .thenReturn(scope);

    var result = keycloakAuthScopeService.createAuthScope(REALM_NAME, CLIENT_ID, scope);
    assertThat(result).isEqualTo(scope);
  }

  @Test
  void createScope_negative() {
    var scope = authorizationScope();

    when(tokenService.issueToken()).thenReturn(AUTH_TOKEN);
    when(keycloakClient.createAuthorizationScope(REALM_NAME, CLIENT_ID, scope, AUTH_TOKEN))
      .thenThrow(FeignException.InternalServerError.class);

    assertThatThrownBy(() -> keycloakAuthScopeService.createAuthScope(REALM_NAME, CLIENT_ID, scope))
      .isInstanceOf(KeycloakException.class)
      .hasMessage("Failed to create authorization scope [%s]", SCOPE_GET)
      .hasCauseInstanceOf(FeignException.InternalServerError.class);
  }

  @Test
  void createAuthScopes_positive() {
    var scope = authorizationScope();

    when(tokenService.issueToken()).thenReturn(AUTH_TOKEN);
    when(keycloakClient.createAuthorizationScope(any(), any(), any(), any()))
      .thenReturn(scope);

    var result = keycloakAuthScopeService.createAuthScopes(REALM_NAME, CLIENT_ID, List.of(SCOPE_GET));
    assertThat(result).isEqualTo(List.of(scope));
  }

  @Test
  void createAuthScopes_negative() {
    when(tokenService.issueToken()).thenReturn(AUTH_TOKEN);
    when(keycloakClient.createAuthorizationScope(any(), any(), any(), any()))
      .thenThrow(FeignException.InternalServerError.class);

    assertThatThrownBy(() -> keycloakAuthScopeService.createAuthScopes(REALM_NAME, CLIENT_ID, List.of(SCOPE_GET)))
      .isInstanceOf(KeycloakException.class)
      .hasMessage("Failed to create authorization scope [%s]", SCOPE_GET)
      .hasCauseInstanceOf(FeignException.InternalServerError.class);
  }
}
