package org.folio.tm.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.folio.tm.support.TestConstants.AUTH_TOKEN;
import static org.folio.tm.support.TestConstants.REALM_NAME;
import static org.folio.tm.support.TestConstants.ROLE_NAME;
import static org.folio.tm.support.TestConstants.USERNAME;
import static org.folio.tm.support.TestConstants.USER_ID;
import static org.folio.tm.support.TestConstants.userDescriptor;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import feign.FeignException.InternalServerError;
import java.util.List;
import org.folio.test.types.UnitTest;
import org.folio.tm.integration.keycloak.KeycloakClient;
import org.folio.tm.integration.keycloak.KeycloakRoleService;
import org.folio.tm.integration.keycloak.KeycloakTemplate;
import org.folio.tm.integration.keycloak.TokenService;
import org.folio.tm.integration.keycloak.exception.KeycloakException;
import org.folio.tm.support.TestConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class KeycloakRoleServiceTest {

  @Mock private KeycloakClient keycloakClient;
  @Mock private TokenService tokenService;
  @InjectMocks private KeycloakTemplate keycloakTemplate;
  private KeycloakRoleService keycloakRoleService;

  @BeforeEach
  void setUp() {
    keycloakRoleService = new KeycloakRoleService(keycloakClient, keycloakTemplate);
  }

  @Test
  void create_positive() {
    var expected = TestConstants.roleDescriptor();
    when(tokenService.issueToken()).thenReturn(AUTH_TOKEN);
    doNothing().when(keycloakClient).createRole(REALM_NAME, expected, AUTH_TOKEN);
    when(keycloakClient.getRoleByName(REALM_NAME, ROLE_NAME, AUTH_TOKEN)).thenReturn(expected);

    var result = keycloakRoleService.createRole(expected, REALM_NAME);
    assertThat(result).isEqualTo(expected);
  }

  @Test
  void create_negative() {
    var expected = TestConstants.roleDescriptor();
    when(tokenService.issueToken()).thenReturn(AUTH_TOKEN);
    doThrow(InternalServerError.class).when(keycloakClient).createRole(REALM_NAME, expected, AUTH_TOKEN);

    assertThatThrownBy(() -> keycloakRoleService.createRole(expected, REALM_NAME))
      .isInstanceOf(KeycloakException.class)
      .hasMessage("Failed to create role [%s] in realm [%s]", ROLE_NAME, REALM_NAME)
      .hasCauseInstanceOf(InternalServerError.class);
  }

  @Test
  void assignRole_positive() {
    var role = TestConstants.roleDescriptor();
    var user = userDescriptor();
    when(tokenService.issueToken()).thenReturn(AUTH_TOKEN);
    keycloakRoleService.assignRole(role, user, REALM_NAME);
    verify(keycloakClient).assignRolesToUser(REALM_NAME, USER_ID, List.of(role), AUTH_TOKEN);
  }

  @Test
  void assignRole_negative() {
    var role = TestConstants.roleDescriptor();
    var user = userDescriptor();

    when(tokenService.issueToken()).thenReturn(AUTH_TOKEN);
    doThrow(InternalServerError.class).when(keycloakClient)
      .assignRolesToUser(REALM_NAME, USER_ID, List.of(role), AUTH_TOKEN);

    assertThatThrownBy(() -> keycloakRoleService.assignRole(role, user, REALM_NAME))
      .isInstanceOf(KeycloakException.class)
      .hasMessage("Failed to assign role [%s] to user [%s] in realm [%s]", ROLE_NAME, USERNAME, REALM_NAME)
      .hasCauseInstanceOf(InternalServerError.class);

    verify(keycloakClient).assignRolesToUser(REALM_NAME, USER_ID, List.of(role), AUTH_TOKEN);
  }
}
