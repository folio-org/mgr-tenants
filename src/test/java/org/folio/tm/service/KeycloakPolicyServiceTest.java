package org.folio.tm.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.folio.tm.support.TestConstants.AUTH_TOKEN;
import static org.folio.tm.support.TestConstants.CLIENT_ID;
import static org.folio.tm.support.TestConstants.CLIENT_POLICY_TYPE;
import static org.folio.tm.support.TestConstants.IMPERSONATE_POLICY;
import static org.folio.tm.support.TestConstants.POLICY_NAME;
import static org.folio.tm.support.TestConstants.REALM_MANAGEMENT_CLIENT;
import static org.folio.tm.support.TestConstants.REALM_NAME;
import static org.folio.tm.support.TestConstants.ROLE_POLICY_TYPE;
import static org.folio.tm.support.TestConstants.authorizationClientPolicy;
import static org.folio.tm.support.TestConstants.authorizationPolicy;
import static org.mockito.Mockito.when;

import feign.FeignException.InternalServerError;
import org.folio.test.types.UnitTest;
import org.folio.tm.integration.keycloak.KeycloakClient;
import org.folio.tm.integration.keycloak.KeycloakPolicyService;
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
class KeycloakPolicyServiceTest {

  @Mock private KeycloakClient keycloakClient;
  @Mock private TokenService tokenService;
  @InjectMocks private KeycloakTemplate keycloakTemplate;
  private KeycloakPolicyService keycloakPolicyService;

  @BeforeEach
  void setUp() {
    keycloakPolicyService = new KeycloakPolicyService(keycloakClient, keycloakTemplate);
  }

  @Test
  void createPolicy_positive() {
    var policy = authorizationPolicy();

    when(tokenService.issueToken()).thenReturn(AUTH_TOKEN);
    when(keycloakClient.createRolePolicy(REALM_NAME, CLIENT_ID, policy, AUTH_TOKEN)).thenReturn(policy);
    var result = keycloakPolicyService.createRolePolicy(policy, REALM_NAME, CLIENT_ID);

    assertThat(result).isEqualTo(policy);
  }

  @Test
  void createPolicy_negative() {
    var policy = authorizationPolicy();

    when(tokenService.issueToken()).thenReturn(AUTH_TOKEN);
    when(keycloakClient.createRolePolicy(REALM_NAME, CLIENT_ID, policy, AUTH_TOKEN))
      .thenThrow(InternalServerError.class);

    assertThatThrownBy(() -> keycloakPolicyService.createRolePolicy(policy, REALM_NAME, CLIENT_ID))
      .isInstanceOf(KeycloakException.class)
      .hasMessage("Failed to create [%s] - based policy [%s] in client [%s]", ROLE_POLICY_TYPE, POLICY_NAME, CLIENT_ID)
      .hasCauseInstanceOf(InternalServerError.class);
  }

  @Test
  void createClientPolicy_positive() {
    var policy = authorizationClientPolicy();

    when(tokenService.issueToken()).thenReturn(AUTH_TOKEN);
    when(keycloakClient.createClientPolicy(REALM_NAME, CLIENT_ID, policy, AUTH_TOKEN)).thenReturn(policy);
    var result = keycloakPolicyService.createClientPolicy(policy, REALM_NAME, CLIENT_ID);

    assertThat(result).isEqualTo(policy);
  }

  @Test
  void createClientPolicy_negative() {
    var policy = authorizationClientPolicy();

    when(tokenService.issueToken()).thenReturn(AUTH_TOKEN);
    when(keycloakClient.createClientPolicy(REALM_NAME, REALM_MANAGEMENT_CLIENT, policy, AUTH_TOKEN))
      .thenThrow(InternalServerError.class);

    assertThatThrownBy(() -> keycloakPolicyService.createClientPolicy(policy, REALM_NAME, REALM_MANAGEMENT_CLIENT))
      .isInstanceOf(KeycloakException.class)
      .hasMessage("Failed to create [%s] - based policy [%s] in client [%s]", CLIENT_POLICY_TYPE,
        IMPERSONATE_POLICY, REALM_MANAGEMENT_CLIENT)
      .hasCauseInstanceOf(InternalServerError.class);
  }
}
