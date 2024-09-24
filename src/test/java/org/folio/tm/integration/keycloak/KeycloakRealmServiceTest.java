package org.folio.tm.integration.keycloak;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.folio.tm.support.TestConstants.TENANT_ID;
import static org.folio.tm.support.TestConstants.TENANT_NAME;
import static org.folio.tm.support.TestUtils.OBJECT_MAPPER;
import static org.folio.tm.support.TestUtils.readString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import java.io.InputStream;
import java.util.List;
import lombok.SneakyThrows;
import org.apache.commons.lang3.SerializationException;
import org.folio.test.types.UnitTest;
import org.folio.tm.domain.dto.Tenant;
import org.folio.tm.integration.keycloak.exception.KeycloakException;
import org.folio.tm.integration.keycloak.service.clients.KeycloakClientService;
import org.folio.tm.integration.keycloak.service.roles.KeycloakRealmRoleService;
import org.folio.tm.support.TestUtils;
import org.folio.tm.utils.JsonHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RealmsResource;
import org.keycloak.admin.client.token.TokenManager;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.idm.ComponentExportRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RequiredActionProviderRepresentation;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class KeycloakRealmServiceTest {

  private KeycloakRealmService keycloakRealmService;

  @Mock private Keycloak keycloak;
  @Mock private KeycloakClientService keycloakClientService;
  @Mock private KeycloakRealmRoleService keycloakRealmRoleService;
  @Spy private JsonHelper jsonHelper = new JsonHelper(OBJECT_MAPPER);

  @Mock private TokenManager tokenManager;
  @Mock private RealmResource realmResource;
  @Mock private RealmsResource realmsResource;
  @Mock private AccessTokenResponse accessTokenResponse;

  @BeforeEach
  void setUp() {
    keycloakRealmService = new KeycloakRealmService(
      keycloak, jsonHelper, List.of(keycloakClientService), List.of(keycloakRealmRoleService));
  }

  @AfterEach
  void tearDown() {
    TestUtils.verifyNoMoreInteractions(this);
  }

  private static RealmRepresentation keycloakRealm() {
    var realmRepresentation = new RealmRepresentation();
    realmRepresentation.setRealm(TENANT_NAME);
    realmRepresentation.setDuplicateEmailsAllowed(true);
    realmRepresentation.setLoginWithEmailAllowed(false);
    realmRepresentation.setEditUsernameAllowed(true);
    realmRepresentation.setEnabled(true);
    realmRepresentation.setRequiredActions(requiredActions());
    realmRepresentation.setComponents(realmComponents());
    return realmRepresentation;
  }

  @SneakyThrows
  private static List<RequiredActionProviderRepresentation> requiredActions() {
    var configuration = readString("json/realms/authentication-required-actions.json");
    return OBJECT_MAPPER.readValue(configuration, new TypeReference<>() {});
  }

  private static MultivaluedHashMap<String, ComponentExportRepresentation> realmComponents() {
    var componentExportRepresentation = new ComponentExportRepresentation();
    componentExportRepresentation.setProviderId("declarative-user-profile");
    var config = new MultivaluedHashMap<String, String>();
    config.add("kc.user.profile.config", getDeclarativeUserProfileConfiguration());
    componentExportRepresentation.setConfig(config);

    var resultMap = new MultivaluedHashMap<String, ComponentExportRepresentation>();
    resultMap.add("org.keycloak.userprofile.UserProfileProvider", componentExportRepresentation);
    return resultMap;
  }

  @SneakyThrows
  private static String getDeclarativeUserProfileConfiguration() {
    var configuration = readString("json/realms/user-profile-configuration.json");
    return OBJECT_MAPPER.readTree(configuration).toString();
  }

  private static Tenant tenant() {
    return new Tenant().id(TENANT_ID).name(TENANT_NAME);
  }

  @Nested
  @DisplayName("createRealm")
  class CreateRealm {

    @Test
    void positive() {
      var keycloakRealm = keycloakRealm();

      when(keycloak.realm(TENANT_NAME)).thenThrow(NotFoundException.class);
      when(keycloak.realms()).thenReturn(realmsResource);
      when(keycloak.tokenManager()).thenReturn(tokenManager);
      when(tokenManager.grantToken()).thenReturn(accessTokenResponse);

      var result = keycloakRealmService.createRealm(tenant());

      assertThat(result)
        .satisfies(realm -> assertThat(realm.getId()).isNotNull())
        .usingRecursiveComparison()
        .ignoringFields("id")
        .isEqualTo(keycloakRealm);

      verify(realmsResource).create(any(RealmRepresentation.class));
      verify(keycloakClientService).setupClient(TENANT_NAME);
      verify(keycloakRealmRoleService).setupRole(TENANT_NAME);

      //noinspection unchecked
      verify(jsonHelper).parse(any(InputStream.class), any(TypeReference.class));
      verify(jsonHelper).parse(any(InputStream.class));
    }

    @Test
    void positive_realmExists() {
      var keycloakRealm = keycloakRealm();

      when(keycloak.realm(TENANT_NAME)).thenReturn(realmResource);
      when(realmResource.toRepresentation()).thenReturn(keycloakRealm);

      var result = keycloakRealmService.createRealm(tenant());

      assertThat(result).isEqualTo(keycloakRealm);
      verify(keycloak, never()).tokenManager();
      verifyNoInteractions(keycloakClientService);
      verifyNoInteractions(keycloakRealmRoleService);
    }

    @Test
    void negative_failedToCreateRealm() {
      when(keycloak.realm(TENANT_NAME)).thenThrow(NotFoundException.class);
      when(keycloak.realms()).thenReturn(realmsResource);
      doThrow(WebApplicationException.class).when(realmsResource).create(any(RealmRepresentation.class));

      var tenant = tenant();
      assertThatThrownBy(() -> keycloakRealmService.createRealm(tenant))
        .isInstanceOf(KeycloakException.class)
        .hasMessage("Failed to create realm for tenant: " + TENANT_NAME)
        .hasCauseInstanceOf(WebApplicationException.class);

      //noinspection unchecked
      verify(jsonHelper).parse(any(InputStream.class), any(TypeReference.class));
      verify(jsonHelper).parse(any(InputStream.class));
    }

    @Test
    void negative_failedToCreateClient() {
      when(keycloak.realm(TENANT_NAME)).thenReturn(realmResource);
      when(realmResource.toRepresentation()).thenThrow(NotFoundException.class).thenReturn(keycloakRealm());
      when(keycloak.realms()).thenReturn(realmsResource);
      when(keycloak.tokenManager()).thenReturn(tokenManager);
      when(tokenManager.grantToken()).thenReturn(accessTokenResponse);
      when(keycloakClientService.setupClient(TENANT_NAME)).thenThrow(new KeycloakException("Failed to create client"));

      var tenant = tenant();
      assertThatThrownBy(() -> keycloakRealmService.createRealm(tenant))
        .isInstanceOf(KeycloakException.class)
        .hasMessage("Failed to create client");

      verify(realmsResource).create(any(RealmRepresentation.class));
      verify(keycloakRealmRoleService).setupRole(TENANT_NAME);
      verify(realmResource).remove();

      //noinspection unchecked
      verify(jsonHelper).parse(any(InputStream.class), any(TypeReference.class));
      verify(jsonHelper).parse(any(InputStream.class));
    }

    @Test
    void negative_failedToCreateClientRuntimeException() {
      when(keycloak.realm(TENANT_NAME)).thenReturn(realmResource);
      when(realmResource.toRepresentation()).thenThrow(NotFoundException.class).thenReturn(keycloakRealm());
      when(keycloak.realms()).thenReturn(realmsResource);
      when(keycloak.tokenManager()).thenReturn(tokenManager);
      when(tokenManager.grantToken()).thenReturn(accessTokenResponse);
      when(keycloakClientService.setupClient(TENANT_NAME)).thenThrow(new RuntimeException("Failed to create client"));

      var tenant = tenant();
      assertThatThrownBy(() -> keycloakRealmService.createRealm(tenant))
        .isInstanceOf(KeycloakException.class)
        .hasMessage("Failed to create realm for tenant: %s", TENANT_NAME)
        .hasCauseInstanceOf(RuntimeException.class);

      verify(realmsResource).create(any(RealmRepresentation.class));
      verify(keycloakRealmRoleService).setupRole(TENANT_NAME);
      verify(realmResource).remove();

      //noinspection unchecked
      verify(jsonHelper).parse(any(InputStream.class), any(TypeReference.class));
      verify(jsonHelper).parse(any(InputStream.class));
    }

    @Test
    void negative_failedToReadAuthenticationRequiredActions() {
      when(keycloak.realm(TENANT_NAME)).thenThrow(NotFoundException.class);
      when(keycloak.realms()).thenReturn(realmsResource);
      when(jsonHelper.parse(any(InputStream.class), any(TypeReference.class))).thenCallRealMethod();
      when(jsonHelper.parse(any(InputStream.class))).thenThrow(new SerializationException("Failed to read input"));

      var tenant = tenant();
      assertThatThrownBy(() -> keycloakRealmService.createRealm(tenant))
        .isInstanceOf(KeycloakException.class)
        .hasMessage("Failed to create realm for tenant: %s", TENANT_NAME)
        .hasCauseInstanceOf(IllegalStateException.class);

      //noinspection unchecked
      verify(jsonHelper).parse(any(InputStream.class), any(TypeReference.class));
      verify(jsonHelper).parse(any(InputStream.class));
    }

    @Test
    void negative_failedToReadUserProfileConfiguration() {
      when(keycloak.realm(TENANT_NAME)).thenThrow(NotFoundException.class);
      when(keycloak.realms()).thenReturn(realmsResource);

      //noinspection unchecked
      when(jsonHelper.parse(any(InputStream.class), any(TypeReference.class)))
        .thenThrow(new SerializationException("Failed to read input"));

      var tenant = tenant();
      assertThatThrownBy(() -> keycloakRealmService.createRealm(tenant))
        .isInstanceOf(KeycloakException.class)
        .hasMessage("Failed to create realm for tenant: %s", TENANT_NAME)
        .hasCauseInstanceOf(IllegalStateException.class);
    }
  }

  @Nested
  @DisplayName("updateRealm")
  class UpdateRealm {

    @Test
    void positive() {
      var keycloakRealm = keycloakRealm();

      when(keycloak.realm(TENANT_NAME)).thenReturn(realmResource);
      when(keycloak.tokenManager()).thenReturn(tokenManager);
      when(tokenManager.grantToken()).thenReturn(accessTokenResponse);

      var result = keycloakRealmService.updateRealm(tenant());

      assertThat(result)
        .satisfies(realm -> assertThat(realm.getId()).isNotNull())
        .usingRecursiveComparison()
        .ignoringFields("id")
        .isEqualTo(keycloakRealm);

      verify(realmResource).update(any(RealmRepresentation.class));

      //noinspection unchecked
      verify(jsonHelper).parse(any(InputStream.class), any(TypeReference.class));
      verify(jsonHelper).parse(any(InputStream.class));
    }

    @Test
    void negative() {
      when(keycloak.realm(TENANT_NAME)).thenThrow(InternalServerErrorException.class);
      when(keycloak.tokenManager()).thenReturn(tokenManager);
      when(tokenManager.grantToken()).thenReturn(accessTokenResponse);

      var tenant = tenant();
      assertThatThrownBy(() -> keycloakRealmService.updateRealm(tenant))
        .isInstanceOf(KeycloakException.class)
        .hasMessage("Failed to update realm for tenant: " + TENANT_NAME)
        .hasCauseInstanceOf(InternalServerErrorException.class);

      //noinspection unchecked
      verify(jsonHelper).parse(any(InputStream.class), any(TypeReference.class));
      verify(jsonHelper).parse(any(InputStream.class));
    }
  }

  @Nested
  @DisplayName("deleteRealm")
  class DeleteRealm {

    @Test
    void positive() {
      when(keycloak.tokenManager()).thenReturn(tokenManager);
      when(tokenManager.grantToken()).thenReturn(accessTokenResponse);
      when(keycloak.realm(TENANT_NAME)).thenReturn(realmResource);
      when(realmResource.toRepresentation()).thenReturn(keycloakRealm());

      keycloakRealmService.deleteRealm(TENANT_NAME);
      verify(realmResource).remove();
    }

    @Test
    void positive_realmNotFound() {
      when(keycloak.realm(TENANT_NAME)).thenReturn(realmResource);
      when(realmResource.toRepresentation()).thenThrow(NotFoundException.class);

      keycloakRealmService.deleteRealm(TENANT_NAME);
      verify(realmResource, never()).remove();
    }

    @Test
    void negative() {
      when(keycloak.tokenManager()).thenReturn(tokenManager);
      when(tokenManager.grantToken()).thenReturn(accessTokenResponse);
      when(keycloak.realm(TENANT_NAME)).thenReturn(realmResource);
      when(realmResource.toRepresentation()).thenReturn(keycloakRealm());
      doThrow(InternalServerErrorException.class).when(realmResource).remove();

      assertThatThrownBy(() -> keycloakRealmService.deleteRealm(TENANT_NAME))
        .isInstanceOf(KeycloakException.class)
        .hasMessage("Failed to delete realm for tenant: " + TENANT_NAME)
        .hasCauseInstanceOf(InternalServerErrorException.class);
    }
  }

  @Nested
  @DisplayName("findRealmByName")
  class FindRealmByName {

    @Test
    void positive() {
      var keycloakRealm = keycloakRealm();
      when(keycloak.realm(TENANT_NAME)).thenReturn(realmResource);
      when(realmResource.toRepresentation()).thenReturn(keycloakRealm);

      var result = keycloakRealmService.findRealmByName(TENANT_NAME);

      assertThat(result).isPresent();
      assertThat(result.get())
        .usingRecursiveComparison()
        .isEqualTo(keycloakRealm);
    }

    @Test
    void positive_notFoundByName() {
      when(keycloak.realm(TENANT_NAME)).thenReturn(realmResource);
      when(realmResource.toRepresentation()).thenThrow(NotFoundException.class);

      var result = keycloakRealmService.findRealmByName(TENANT_NAME);

      assertThat(result).isEmpty();
    }

    @Test
    void negative_internalServerErrorException() {
      when(keycloak.realm(TENANT_NAME)).thenReturn(realmResource);
      when(realmResource.toRepresentation()).thenThrow(InternalServerErrorException.class);

      assertThatThrownBy(() -> keycloakRealmService.findRealmByName(TENANT_NAME))
        .isInstanceOf(KeycloakException.class)
        .hasMessage("Failed to find Keycloak realm by name: " + TENANT_NAME)
        .hasCauseInstanceOf(InternalServerErrorException.class);
    }
  }
}
