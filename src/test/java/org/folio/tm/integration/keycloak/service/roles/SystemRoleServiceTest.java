package org.folio.tm.integration.keycloak.service.roles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.folio.tm.support.TestConstants.TENANT_NAME;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.InternalServerErrorException;
import org.folio.test.types.UnitTest;
import org.folio.tm.integration.keycloak.exception.KeycloakException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RoleResource;
import org.keycloak.admin.client.resource.RolesResource;
import org.keycloak.representations.idm.RoleRepresentation;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class SystemRoleServiceTest {

  @InjectMocks private SystemRoleService systemRoleService;
  @Mock private Keycloak keycloak;
  @Mock private RealmResource realmResource;
  @Mock private RoleResource roleResource;
  @Mock private RolesResource rolesResource;

  @Test
  void getRole_positive() {
    var result = systemRoleService.getRole(TENANT_NAME);
    assertThat(result).isEqualTo(keycloakRole());
  }

  @Test
  void setupRole_positive() {
    var keycloakRole = keycloakRole();

    when(keycloak.realm(TENANT_NAME)).thenReturn(realmResource);
    when(realmResource.roles()).thenReturn(rolesResource);
    when(rolesResource.get("System")).thenReturn(roleResource);
    when(roleResource.toRepresentation()).thenReturn(keycloakRole);

    var result = systemRoleService.setupRole(TENANT_NAME);

    assertThat(result).isEqualTo(keycloakRole);
    verify(rolesResource).create(keycloakRole);
  }

  @Test
  void setupRole_negative_failedToCreateRole() {
    var keycloakRole = keycloakRole();

    when(keycloak.realm(TENANT_NAME)).thenReturn(realmResource);
    when(realmResource.roles()).thenReturn(rolesResource);
    doThrow(InternalServerErrorException.class).when(rolesResource).create(keycloakRole);

    assertThatThrownBy(() -> systemRoleService.setupRole(TENANT_NAME))
      .isInstanceOf(KeycloakException.class)
      .hasMessage("Failed to setup role: System")
      .hasCauseInstanceOf(InternalServerErrorException.class);
  }

  private static RoleRepresentation keycloakRole() {
    var keycloakRole = new RoleRepresentation();
    keycloakRole.setName("System");
    keycloakRole.setDescription("System role for module-to-module communication");
    return keycloakRole;
  }
}
