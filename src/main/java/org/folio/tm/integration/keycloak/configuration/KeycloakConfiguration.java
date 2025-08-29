package org.folio.tm.integration.keycloak.configuration;

import static org.folio.common.utils.tls.FeignClientTlsUtils.buildTargetFeignClient;
import static org.folio.security.integration.keycloak.utils.ClientBuildUtils.buildKeycloakAdminClient;

import feign.Contract;
import feign.codec.Decoder;
import feign.codec.Encoder;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import okhttp3.OkHttpClient;
import org.folio.security.integration.keycloak.configuration.properties.KeycloakProperties;
import org.folio.security.integration.keycloak.utils.KeycloakSecretUtils;
import org.folio.tm.integration.keycloak.ClientSecretService;
import org.folio.tm.integration.keycloak.KeycloakClient;
import org.folio.tm.integration.keycloak.KeycloakRealmService;
import org.folio.tm.integration.keycloak.KeycloakServerInfoService;
import org.folio.tm.integration.keycloak.KeycloakTenantListener;
import org.folio.tm.integration.keycloak.service.clients.ImpersonationClientService;
import org.folio.tm.integration.keycloak.service.clients.KeycloakClientService;
import org.folio.tm.integration.keycloak.service.clients.LoginClientService;
import org.folio.tm.integration.keycloak.service.clients.ModuleClientService;
import org.folio.tm.integration.keycloak.service.clients.PasswordResetClientService;
import org.folio.tm.integration.keycloak.service.roles.KeycloakRealmRoleService;
import org.folio.tm.integration.keycloak.service.roles.PasswordResetRoleService;
import org.folio.tm.integration.keycloak.service.roles.SystemRoleService;
import org.folio.tm.utils.JsonHelper;
import org.folio.tools.store.SecureStore;
import org.folio.tools.store.exception.SecretNotFoundException;
import org.keycloak.admin.client.Keycloak;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.openfeign.FeignClientsConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Log4j2
@Configuration
@RequiredArgsConstructor
@Import(FeignClientsConfiguration.class)
@ConditionalOnProperty("application.keycloak.enabled")
@EnableConfigurationProperties({KeycloakProperties.class, KeycloakRealmSetupProperties.class})
public class KeycloakConfiguration {

  private final SecureStore secureStore;
  private final KeycloakProperties properties;

  @Bean
  @ConditionalOnProperty(name = "application.keycloak.import.enabled", havingValue = "false", matchIfMissing = true)
  public Keycloak keycloak() {
    var clientId = properties.getAdmin().getClientId();
    var clientSecret = getKeycloakClientSecret(clientId);
    return buildKeycloakAdminClient(clientSecret, properties);
  }

  @Bean
  public KeycloakClient keycloakClient(OkHttpClient okHttpClient, Contract contract, Encoder encoder, Decoder decoder) {
    return buildTargetFeignClient(okHttpClient, contract, encoder, decoder, properties.getTls(), properties.getUrl(),
      KeycloakClient.class);
  }

  @Bean
  public KeycloakRealmService keycloakRealmService(Keycloak keycloak, JsonHelper jsonHelper,
    List<KeycloakRealmRoleService> keycloakRealmRoleServices,
    List<KeycloakClientService> keycloakClientServices,
    KeycloakRealmSetupProperties keycloakRealmSetupProperties) {
    return new KeycloakRealmService(keycloak, jsonHelper, keycloakClientServices, keycloakRealmRoleServices,
      keycloakRealmSetupProperties);
  }

  @Bean
  public ClientSecretService clientSecretService(SecureStore secureStore, KeycloakRealmSetupProperties properties) {
    return new ClientSecretService(secureStore, properties);
  }

  @Bean
  public KeycloakTenantListener keycloakTenantListener(KeycloakRealmService keycloakRealmService) {
    return new KeycloakTenantListener(keycloakRealmService);
  }

  @Bean
  public KeycloakServerInfoService keycloakServerInfoService(Keycloak keycloak) {
    return new KeycloakServerInfoService(keycloak);
  }

  @Bean
  public LoginClientService loginClientService(Keycloak keycloak, ClientSecretService clientSecretService,
    KeycloakRealmSetupProperties keycloakRealmSetupProperties, JsonHelper jsonHelper) {
    var service = new LoginClientService(jsonHelper, keycloakRealmSetupProperties);
    service.setKeycloak(keycloak);
    service.setClientSecretService(clientSecretService);
    return service;
  }

  @Bean
  public ModuleClientService moduleClientService(Keycloak keycloak, ClientSecretService clientSecretService,
    KeycloakRealmSetupProperties keycloakRealmSetupProperties) {
    var service = new ModuleClientService(keycloakRealmSetupProperties);
    service.setKeycloak(keycloak);
    service.setClientSecretService(clientSecretService);
    return service;
  }

  @Bean
  public ImpersonationClientService impersonationClientService(Keycloak keycloak, KeycloakClient keycloakClient,
    ClientSecretService clientSecretService, KeycloakRealmSetupProperties keycloakRealmSetupProperties) {
    var service = new ImpersonationClientService(keycloakClient, keycloakRealmSetupProperties);
    service.setKeycloak(keycloak);
    service.setClientSecretService(clientSecretService);
    return service;
  }

  @Bean
  public PasswordResetClientService passwordResetClientService(Keycloak keycloak,
    ClientSecretService clientSecretService, KeycloakServerInfoService keycloakServerInfoService,
    KeycloakRealmSetupProperties keycloakRealmSetupProperties) {
    var service = new PasswordResetClientService(keycloakServerInfoService, keycloakRealmSetupProperties);
    service.setKeycloak(keycloak);
    service.setClientSecretService(clientSecretService);
    return service;
  }

  @Bean
  public SystemRoleService systemRoleService(Keycloak keycloak) {
    var service = new SystemRoleService();
    service.setKeycloak(keycloak);
    return service;
  }

  @Bean
  public PasswordResetRoleService passwordResetRoleService(Keycloak keycloak) {
    var service = new PasswordResetRoleService();
    service.setKeycloak(keycloak);
    return service;
  }

  private String getKeycloakClientSecret(String clientId) {
    try {
      return secureStore.get(KeycloakSecretUtils.globalStoreKey(clientId));
    } catch (SecretNotFoundException e) {
      log.debug("Secret for 'admin' client is not defined in the secret store: clientId = {}", clientId);
      return null;
    }
  }
}
