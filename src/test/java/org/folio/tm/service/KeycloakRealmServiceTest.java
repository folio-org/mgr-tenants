package org.folio.tm.service;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.folio.tm.support.TestConstants.TENANT_ID;
import static org.folio.tm.support.TestConstants.TENANT_NAME;
import static org.folio.tm.support.TestConstants.getAccessToken;
import static org.folio.tm.support.TestConstants.realmDescriptor;
import static org.folio.tm.support.TestConstants.serverInfo;
import static org.folio.tm.support.TestConstants.tenant;
import static org.folio.tm.support.TestUtils.OBJECT_MAPPER;
import static org.folio.tm.support.TestUtils.readString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import feign.FeignException.InternalServerError;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.SneakyThrows;
import org.folio.test.types.UnitTest;
import org.folio.tm.domain.dto.Tenant;
import org.folio.tm.integration.keycloak.KeycloakClient;
import org.folio.tm.integration.keycloak.KeycloakRealmService;
import org.folio.tm.integration.keycloak.KeycloakTemplate;
import org.folio.tm.integration.keycloak.KeycloakUtils;
import org.folio.tm.integration.keycloak.TokenService;
import org.folio.tm.integration.keycloak.exception.KeycloakException;
import org.folio.tm.integration.keycloak.model.Realm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

@UnitTest
@ExtendWith(MockitoExtension.class)
class KeycloakRealmServiceTest {

  private KeycloakRealmService keycloakService;

  @InjectMocks private KeycloakTemplate template;
  @Mock private KeycloakClient keycloakClient;
  @Mock private TokenService tokenService;
  @Spy private final ObjectMapper objectMapper = OBJECT_MAPPER;

  @BeforeEach
  void setUp() {
    keycloakService = new KeycloakRealmService(objectMapper, keycloakClient, template, tokenService);
  }

  @Test
  void create_positive() {
    var expected = realmDescriptor()
      .components(realmComponents())
      .requiredActions(requiredActions());
    var token = getAccessToken();
    when(tokenService.issueToken()).thenReturn(token);
    when(tokenService.renewToken()).thenReturn(token);
    when(keycloakClient.getRealm(expected.getName(), token)).thenThrow(FeignException.NotFound.class);
    when(keycloakClient.createRealm(expected, token)).thenReturn(ResponseEntity.ok(null));

    try (var mocked = mockStatic(KeycloakUtils.class)) {
      mocked.when(() -> KeycloakUtils.extractResourceId(any())).thenReturn(Optional.of(expected.getId()));

      var result = keycloakService.createRealm(new Tenant().id(TENANT_ID).name(TENANT_NAME));

      assertThat(result).isEqualTo(expected);
    }
  }

  @Test
  void create_negative() {
    var tenant = tenant();
    var expected = realmDescriptor()
      .components(realmComponents())
      .requiredActions(requiredActions());
    var token = getAccessToken();

    when(tokenService.issueToken()).thenReturn(token);
    doThrow(InternalServerError.class).when(keycloakClient).createRealm(expected, token);

    assertThatThrownBy(() -> keycloakService.createRealm(tenant))
      .isInstanceOf(KeycloakException.class)
      .hasMessage("Failed to create realm for tenant: " + TENANT_NAME)
      .hasCauseInstanceOf(InternalServerError.class);
  }

  @Test
  void update_positive() {
    var expected = realmDescriptor()
      .components(realmComponents())
      .requiredActions(requiredActions());
    var token = getAccessToken();
    when(tokenService.issueToken()).thenReturn(token);
    when(tokenService.renewToken()).thenReturn(token);
    doNothing().when(keycloakClient).updateRealm(TENANT_NAME, expected, token);

    var result = keycloakService.updateRealm(tenant());

    assertThat(result).isEqualTo(expected);
  }

  @Test
  void update_negative() {
    var expected = realmDescriptor()
      .components(realmComponents())
      .requiredActions(requiredActions());
    var token = getAccessToken();
    var tenant = tenant();

    when(tokenService.issueToken()).thenReturn(token);
    doThrow(InternalServerError.class).when(keycloakClient).updateRealm(TENANT_NAME, expected, token);

    assertThatThrownBy(() -> keycloakService.updateRealm(tenant))
      .isInstanceOf(KeycloakException.class)
      .hasMessage("Failed to update realm for tenant: " + TENANT_NAME)
      .hasCauseInstanceOf(InternalServerError.class);
  }

  @Test
  void delete_positive() {
    var token = getAccessToken();
    when(keycloakClient.getRealm(TENANT_NAME, token)).thenReturn(new Realm());
    when(tokenService.issueToken()).thenReturn(token);
    when(tokenService.renewToken()).thenReturn(token);
    keycloakService.deleteRealm(TENANT_NAME);
    verify(keycloakClient).deleteRealm(TENANT_NAME, token);
  }

  @Test
  void delete_negative() {
    var token = getAccessToken();
    when(tokenService.issueToken()).thenReturn(token);
    when(keycloakClient.getRealm(TENANT_NAME, token)).thenReturn(new Realm());
    doThrow(InternalServerError.class).when(keycloakClient).deleteRealm(TENANT_NAME, token);

    assertThatThrownBy(() -> keycloakService.deleteRealm(TENANT_NAME))
      .isInstanceOf(KeycloakException.class)
      .hasMessage("Failed to delete realm for tenant: " + TENANT_NAME)
      .hasCauseInstanceOf(InternalServerError.class);

    verify(keycloakClient).deleteRealm(TENANT_NAME, token);
  }

  @Test
  void getServerInfo_positive() {
    var token = getAccessToken();
    when(tokenService.issueToken()).thenReturn(token);
    var expected = serverInfo();
    when(keycloakClient.getServerInfo(token)).thenReturn(expected);

    var result = keycloakService.getServerInfo();
    assertThat(result).isEqualTo(expected);
  }

  @SneakyThrows
  private static List<Map<String, Object>> requiredActions() {
    var configuration = readString("json/realms/authentication-required-actions.json");
    return OBJECT_MAPPER.readValue(configuration, new TypeReference<>() {});
  }

  private static Map<String, List<Map<String, Object>>> realmComponents() {
    return Map.of(
      "org.keycloak.userprofile.UserProfileProvider", List.of(Map.of(
        "providerId", "declarative-user-profile",
        "config", Map.of("kc.user.profile.config", singletonList(getDeclarativeUserProfileConfiguration()))
      ))
    );
  }

  @SneakyThrows
  private static String getDeclarativeUserProfileConfiguration() {
    var configuration = readString("json/realms/user-profile-configuration.json");
    return OBJECT_MAPPER.readTree(configuration).toString();
  }
}
