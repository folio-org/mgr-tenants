package org.folio.tm.integration.keycloak;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.util.Optional.ofNullable;
import static org.springframework.util.CollectionUtils.isEmpty;

import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.tm.domain.dto.Tenant;
import org.folio.tm.integration.keycloak.configuration.KeycloakRealmSetupProperties;
import org.folio.tm.integration.keycloak.exception.KeycloakException;
import org.folio.tm.integration.keycloak.service.clients.KeycloakClientService;
import org.folio.tm.integration.keycloak.service.roles.KeycloakRealmRoleService;
import org.folio.tm.utils.JsonHelper;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.representations.idm.ComponentExportRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RequiredActionProviderRepresentation;
import org.springframework.util.Assert;

@Log4j2
@RequiredArgsConstructor
public class KeycloakRealmService {

  private final Keycloak keycloak;
  private final JsonHelper jsonHelper;
  private final List<KeycloakClientService> keycloakClientServices;
  private final List<KeycloakRealmRoleService> keycloakRoleServices;
  private final KeycloakRealmSetupProperties keycloakRealmSetupProperties;

  /**
   * Creates keycloak realm for given tenant.
   *
   * @param tenant - tenant descriptor as {@link Tenant} object
   */
  public RealmRepresentation createRealm(Tenant tenant) {
    var realmName = tenant.getName();
    try {
      var realmByName = findRealmByName(realmName);
      if (realmByName.isPresent()) {
        return realmByName.get();
      }

      var realmResource = keycloak.realms();
      var realm = toRealmRepresentation(tenant);
      realmResource.create(realm);
      keycloak.tokenManager().grantToken();

      keycloakRoleServices.forEach(roleService -> roleService.setupRole(realmName));
      keycloakClientServices.forEach(clientService -> clientService.setupClient(realmName));
      return realm;
    } catch (Exception exception) {
      deleteRealm(realmName);
      if (exception instanceof KeycloakException) {
        throw exception;
      }

      throw new KeycloakException("Failed to create realm for tenant: " + tenant.getName(), exception);
    }
  }

  /**
   * Updates keycloak realm for given tenant.
   *
   * @param tenant - tenant descriptor as {@link Tenant} object
   */
  public RealmRepresentation updateRealm(Tenant tenant) {
    var realmRepresentation = toRealmRepresentation(tenant);
    try {
      keycloak.tokenManager().grantToken();
      var realmResource = keycloak.realm(tenant.getName());
      realmResource.update(realmRepresentation);
      return realmRepresentation;
    } catch (WebApplicationException exception) {
      throw new KeycloakException("Failed to update realm for tenant: " + tenant.getName(), exception);
    }
  }

  /**
   * Deletes keycloak realm by tenant name.
   *
   * @param name - tenant name as {@link String} object
   */
  public void deleteRealm(String name) {
    try {
      var realmByName = findRealmByName(name);
      if (realmByName.isEmpty()) {
        log.debug("Realm is not found by name");
        return;
      }

      keycloak.tokenManager().grantToken();
      keycloak.realm(name).remove();
    } catch (WebApplicationException exception) {
      throw new KeycloakException("Failed to delete realm for tenant: " + name, exception);
    }
  }

  /**
   * Retrieves realm by name.
   *
   * @param name - realm name
   * @return {@link Optional} with found {@link RealmRepresentation} object, empty if realm is not found.
   */
  public Optional<RealmRepresentation> findRealmByName(String name) {
    try {
      log.debug("Check existence of realm [name: {}]", name);
      var realmRepresentation = keycloak.realm(name).toRepresentation();
      log.debug("Realm exists in Keycloak [name: {}]", name);
      return ofNullable(realmRepresentation);
    } catch (NotFoundException cause) {
      log.debug("Realm was not found [name: {}]", name);
      return Optional.empty();
    } catch (WebApplicationException exception) {
      throw new KeycloakException("Failed to find Keycloak realm by name: " + name, exception);
    }
  }

  private RealmRepresentation toRealmRepresentation(Tenant tenant) {
    Assert.notNull(tenant.getId(), "Tenant identifier must not be null");
    var realmName = tenant.getName();

    var realm = new RealmRepresentation();
    realm.setId(tenant.getId().toString());
    realm.setRealm(realmName);
    realm.setEnabled(true);
    realm.setDuplicateEmailsAllowed(TRUE);
    realm.setLoginWithEmailAllowed(FALSE);
    realm.setEditUsernameAllowed(TRUE);
    realm.setRequiredActions(getAuthenticationRequiredActions());
    realm.setComponents(getRealmComponentsConfiguration());
    realm.setAccessCodeLifespan(keycloakRealmSetupProperties.getAccessCodeLifespan());
    ofNullable(keycloakRealmSetupProperties.getParRequestUriLifespan())
      .ifPresent(parRequestUriLifespan -> {
        if (isEmpty(realm.getAttributes())) {
          realm.setAttributes(new HashMap<>());
        }
        realm.getAttributes().put("parRequestUriLifespan", parRequestUriLifespan.toString());
      });

    realm.setAccessTokenLifespan(keycloakRealmSetupProperties.getAccessTokenLifespan());
    realm.setSsoSessionIdleTimeout(keycloakRealmSetupProperties.getSsoSessionIdleTimeout());
    realm.setSsoSessionMaxLifespan(keycloakRealmSetupProperties.getSsoSessionMaxLifespan());
    realm.setClientSessionIdleTimeout(keycloakRealmSetupProperties.getClientSessionIdleTimeout());
    realm.setClientSessionMaxLifespan(keycloakRealmSetupProperties.getClientSessionMaxLifespan());

    return realm;
  }

  private MultivaluedHashMap<String, ComponentExportRepresentation> getRealmComponentsConfiguration() {
    var componentExportRepresentation = new ComponentExportRepresentation();
    componentExportRepresentation.setProviderId("declarative-user-profile");
    componentExportRepresentation.setConfig(getDeclarativeUserProfileConfiguration());

    var componentExportRepr = new MultivaluedHashMap<String, ComponentExportRepresentation>();
    componentExportRepr.add("org.keycloak.userprofile.UserProfileProvider", componentExportRepresentation);
    return componentExportRepr;
  }

  private List<RequiredActionProviderRepresentation> getAuthenticationRequiredActions() {
    var userProfileFileLocation = "json/realms/authentication-required-actions.json";
    try (var inStream = getResourceFileInputStream(userProfileFileLocation)) {
      return jsonHelper.parse(inStream, new TypeReference<>() {});
    } catch (Exception exception) {
      throw new IllegalStateException("Failed to read authentication request actions", exception);
    }
  }

  private MultivaluedHashMap<String, String> getDeclarativeUserProfileConfiguration() {
    var userProfileFileLocation = "json/realms/user-profile-configuration.json";
    try (var inStream = getResourceFileInputStream(userProfileFileLocation)) {
      var userProfileConfigurationString = jsonHelper.parse(inStream).toString();
      var userProfileConfigurationMap = new MultivaluedHashMap<String, String>();
      userProfileConfigurationMap.add("kc.user.profile.config", userProfileConfigurationString);
      return userProfileConfigurationMap;
    } catch (Exception exception) {
      throw new IllegalStateException("Failed to read user profile configuration", exception);
    }
  }

  private static InputStream getResourceFileInputStream(String userProfileFileLocation) {
    return KeycloakRealmService.class.getClassLoader().getResourceAsStream(userProfileFileLocation);
  }
}
