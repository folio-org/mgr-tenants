package org.folio.tm.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.folio.tm.support.TestConstants.ADMIN_IMPERSONATING_PERMISSION;
import static org.folio.tm.support.TestConstants.AUTH_TOKEN;
import static org.folio.tm.support.TestConstants.CLIENT_ID;
import static org.folio.tm.support.TestConstants.IMPERSONATE_SCOPE_ID;
import static org.folio.tm.support.TestConstants.PERMISSION_NAME;
import static org.folio.tm.support.TestConstants.REALM_MANAGEMENT_CLIENT_ID;
import static org.folio.tm.support.TestConstants.REALM_NAME;
import static org.folio.tm.support.TestConstants.authorizationImpersonationPermission;
import static org.folio.tm.support.TestConstants.authorizationPermission;
import static org.folio.tm.support.TestConstants.scopePermission;
import static org.folio.tm.support.TestConstants.userManagementPermission;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import feign.FeignException.InternalServerError;
import org.folio.test.types.UnitTest;
import org.folio.tm.integration.keycloak.KeycloakClient;
import org.folio.tm.integration.keycloak.KeycloakPermissionService;
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
class KeycloakPermissionServiceTest {

  @Mock private KeycloakClient keycloakClient;
  @Mock private TokenService tokenService;
  @InjectMocks private KeycloakTemplate keycloakTemplate;
  private KeycloakPermissionService keycloakPermissionService;

  @BeforeEach
  void setUp() {
    keycloakPermissionService = new KeycloakPermissionService(keycloakClient, keycloakTemplate);
  }

  @Test
  void createPermission_positive() {
    var permission = authorizationPermission();

    when(tokenService.issueToken()).thenReturn(AUTH_TOKEN);
    when(keycloakClient.createAuthPermission(REALM_NAME, CLIENT_ID, permission, AUTH_TOKEN)).thenReturn(permission);

    var result = keycloakPermissionService.createRolePermission(permission, REALM_NAME, CLIENT_ID);
    assertThat(result).isEqualTo(permission);
  }

  @Test
  void createPermission_negative() {
    var permission = authorizationPermission();

    when(tokenService.issueToken()).thenReturn(AUTH_TOKEN);
    when(keycloakClient.createAuthPermission(REALM_NAME, CLIENT_ID, permission, AUTH_TOKEN))
      .thenThrow(InternalServerError.class);

    assertThatThrownBy(() -> keycloakPermissionService.createRolePermission(permission, REALM_NAME, CLIENT_ID))
      .isInstanceOf(KeycloakException.class)
      .hasMessage("Failed to create authorization permission [%s] in client [%s]", PERMISSION_NAME, CLIENT_ID)
      .hasCauseInstanceOf(InternalServerError.class);
  }

  @Test
  void enableUsersPermission_positive() {
    var permissionRequest = userManagementPermission();
    var permissionResponse = userManagementPermission(scopePermission());

    when(tokenService.issueToken()).thenReturn(AUTH_TOKEN);
    when(keycloakClient.enablePermissionForRealmClient(REALM_NAME, permissionRequest, AUTH_TOKEN))
      .thenReturn(permissionResponse);

    var result = keycloakPermissionService.enableUserPermissionsInRealm(REALM_NAME);

    assertThat(result).isEqualTo(permissionResponse);
  }

  @Test
  void enableUsersPermission_negative() {
    var permissionRequest = userManagementPermission();

    when(tokenService.issueToken()).thenReturn(AUTH_TOKEN);
    when(keycloakClient.enablePermissionForRealmClient(REALM_NAME, permissionRequest, AUTH_TOKEN))
      .thenThrow(InternalServerError.class);

    assertThatThrownBy(() -> keycloakPermissionService.enableUserPermissionsInRealm(REALM_NAME))
      .isInstanceOf(KeycloakException.class)
      .hasMessage("Failed to enable users impersonation for realm [%s]", REALM_NAME)
      .hasCauseInstanceOf(InternalServerError.class);
  }

  @Test
  void updatePermission_positive() {
    var permissionRequest = authorizationImpersonationPermission();

    when(tokenService.issueToken()).thenReturn(AUTH_TOKEN);
    doNothing().when(keycloakClient).updatePermission(REALM_NAME, REALM_MANAGEMENT_CLIENT_ID, IMPERSONATE_SCOPE_ID,
      permissionRequest, AUTH_TOKEN);

    keycloakPermissionService.updatePermission(REALM_NAME, REALM_MANAGEMENT_CLIENT_ID,
      IMPERSONATE_SCOPE_ID, permissionRequest);

    verify(keycloakClient).updatePermission(REALM_NAME, REALM_MANAGEMENT_CLIENT_ID, IMPERSONATE_SCOPE_ID,
      permissionRequest, AUTH_TOKEN);
  }

  @Test
  void updatePermission_negative() {
    var permissionRequest = authorizationImpersonationPermission();

    when(tokenService.issueToken()).thenReturn(AUTH_TOKEN);
    doThrow(InternalServerError.class).when(keycloakClient)
      .updatePermission(REALM_NAME, REALM_MANAGEMENT_CLIENT_ID, IMPERSONATE_SCOPE_ID,
      permissionRequest, AUTH_TOKEN);

    assertThatThrownBy(() -> keycloakPermissionService
      .updatePermission(REALM_NAME, REALM_MANAGEMENT_CLIENT_ID, IMPERSONATE_SCOPE_ID, permissionRequest))
      .isInstanceOf(KeycloakException.class)
      .hasMessage("Failed to update [%s] permission for client [%s]", ADMIN_IMPERSONATING_PERMISSION, REALM_NAME)
      .hasCauseInstanceOf(InternalServerError.class);
  }
}
