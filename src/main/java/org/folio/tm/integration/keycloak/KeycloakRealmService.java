package org.folio.tm.integration.keycloak;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.util.Collections.singletonList;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.folio.tm.domain.dto.Tenant;
import org.folio.tm.integration.keycloak.KeycloakTemplate.KeycloakFunction;
import org.folio.tm.integration.keycloak.KeycloakTemplate.KeycloakMethod;
import org.folio.tm.integration.keycloak.model.Realm;
import org.folio.tm.integration.keycloak.model.ServerInfo;

@Log4j2
@RequiredArgsConstructor
public class KeycloakRealmService {

  private final ObjectMapper objectMapper;
  private final KeycloakClient keycloakClient;
  private final KeycloakTemplate template;
  private final TokenService tokenService;

  /**
   * Creates keycloak realm for given tenant.
   *
   * @param tenant - tenant descriptor as {@link Tenant} object
   */
  public Realm createRealm(Tenant tenant) {
    var realm = toRealm(tenant);

    return template.call(create(realm), () -> "Failed to create realm for tenant: " + tenant.getName());
  }

  /**
   * Updates keycloak realm for given tenant.
   *
   * @param tenant - tenant descriptor as {@link Tenant} object
   */
  public Realm updateRealm(Tenant tenant) {
    var realm = toRealm(tenant);

    return template.call(update(realm), () -> "Failed to update realm for tenant: " + tenant.getName());
  }

  /**
   * Deletes keycloak realm by tenant name.
   *
   * @param name - tenant name as {@link String} object
   */
  public void deleteRealm(String name) {
    template.call(delete(name), () -> "Failed to delete realm for tenant: " + name);
  }

  public boolean isRealmExist(String name) {
    return template.call(isExist(name), () -> "Failed to test realm existence: " + name);
  }

  public ServerInfo getServerInfo() {
    return template.call(retrieveServerInfo(), () -> "Failed to get server info");
  }

  private KeycloakFunction<Realm> create(Realm realm) {
    return token -> {
      if (!isRealmExist(realm.getName())) {
        log.info("Creating realm [realm: {}]", realm);

        var res = keycloakClient.createRealm(realm, token);

        realm.setId(KeycloakUtils.extractResourceId(res).orElse(null));

        log.info("Realm created [id: {}]", realm.getId());

        tokenService.renewToken();
        log.info("Token renewed");
      }

      return realm;
    };
  }

  private KeycloakFunction<ServerInfo> retrieveServerInfo() {
    return keycloakClient::getServerInfo;
  }

  private KeycloakFunction<Realm> update(Realm realm) {
    return token -> {
      log.info("Updating realm [realm: {}]", realm);
      keycloakClient.updateRealm(realm.getName(), realm, token);

      tokenService.renewToken();
      log.info("Token renewed");

      return realm;
    };
  }

  private KeycloakMethod delete(String name) {
    return token -> {
      if (isRealmExist(name)) {
        log.info("Deleting realm [name: {}]", name);

        keycloakClient.deleteRealm(name, token);

        tokenService.renewToken();
        log.info("Token renewed");
      } else {
        log.info("Realm already deleted [name: {}]", name);
      }
    };
  }

  private KeycloakFunction<Boolean> isExist(String name) {
    return token -> {
      try {
        log.info("Check existence of realm [name: {}]", name);
        var realm = keycloakClient.getRealm(name, token);
        log.info("Realm exists in Keycloak [name: {}]", name);
        return Objects.nonNull(realm);
      } catch (FeignException.NotFound cause) {
        log.info("Realm was not found [name: {}]", name);
        return false;
      }
    };
  }

  private Realm toRealm(Tenant tenant) {
    var realm = new Realm();

    assert tenant.getId() != null;
    realm.setId(tenant.getId().toString());

    realm.setName(tenant.getName());
    realm.setEnabled(true);
    realm.setDuplicateEmailsAllowed(TRUE);
    realm.setLoginWithEmailAllowed(FALSE);
    realm.setRequiredActions(getAuthenticationRequiredActions());
    realm.setComponents(Map.of(
      "org.keycloak.userprofile.UserProfileProvider", List.of(Map.of(
        "providerId", "declarative-user-profile",
        "config", Map.of("kc.user.profile.config", singletonList(getDeclarativeUserProfileConfiguration()))
      ))
    ));

    return realm;
  }

  @SneakyThrows
  private List<Map<String, Object>> getAuthenticationRequiredActions() {
    var userProfileFileLocation = "json/realms/authentication-required-actions.json";
    try (var inStream = getResourceFileInputStream(userProfileFileLocation)) {
      return objectMapper.readValue(inStream, new TypeReference<>() {});
    }
  }

  @SneakyThrows
  private String getDeclarativeUserProfileConfiguration() {
    var userProfileFileLocation = "json/realms/user-profile-configuration.json";
    try (var inStream = getResourceFileInputStream(userProfileFileLocation)) {
      return objectMapper.readTree(inStream).toString();
    }
  }

  private static InputStream getResourceFileInputStream(String userProfileFileLocation) {
    return KeycloakRealmService.class.getClassLoader().getResourceAsStream(userProfileFileLocation);
  }
}
