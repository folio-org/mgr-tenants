package org.folio.tm.integration.keycloak.configuration;

import static org.folio.common.utils.FeignClientTlsUtils.buildTargetFeignClient;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Contract;
import feign.codec.Decoder;
import feign.codec.Encoder;
import lombok.extern.log4j.Log4j2;
import okhttp3.OkHttpClient;
import org.folio.security.integration.keycloak.configuration.properties.KeycloakProperties;
import org.folio.tm.integration.keycloak.ClientSecretService;
import org.folio.tm.integration.keycloak.KeycloakAuthScopeService;
import org.folio.tm.integration.keycloak.KeycloakClient;
import org.folio.tm.integration.keycloak.KeycloakClientService;
import org.folio.tm.integration.keycloak.KeycloakImpersonationService;
import org.folio.tm.integration.keycloak.KeycloakPermissionService;
import org.folio.tm.integration.keycloak.KeycloakPolicyService;
import org.folio.tm.integration.keycloak.KeycloakRealmManagementService;
import org.folio.tm.integration.keycloak.KeycloakRealmService;
import org.folio.tm.integration.keycloak.KeycloakRoleService;
import org.folio.tm.integration.keycloak.KeycloakTemplate;
import org.folio.tm.integration.keycloak.TokenService;
import org.folio.tm.service.listeners.TenantServiceListener;
import org.folio.tools.store.SecureStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.openfeign.FeignClientsConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Log4j2
@Configuration
@Import(FeignClientsConfiguration.class)
@EnableConfigurationProperties({KeycloakProperties.class, KeycloakRealmSetupProperties.class})
@ConditionalOnProperty("application.keycloak.enabled")
public class KeycloakConfiguration {

  @Bean
  public KeycloakClient keycloakClient(OkHttpClient okHttpClient, KeycloakProperties properties, Contract contract,
    Encoder encoder, Decoder decoder) {
    return buildTargetFeignClient(okHttpClient, contract, encoder, decoder, properties.getTls(), properties.getUrl(),
      KeycloakClient.class);
  }

  @Bean
  public TokenService tokenService(KeycloakClient keycloakClient, KeycloakProperties properties,
    SecureStore secureStore) {
    return new TokenService(keycloakClient, properties.getAdmin(), secureStore);
  }

  @Bean
  public KeycloakTemplate keycloakTemplate(TokenService tokenService) {
    return new KeycloakTemplate(tokenService);
  }

  @Bean
  public KeycloakRealmService keycloakRealmService(KeycloakClient keycloakClient, KeycloakTemplate template,
    TokenService tokenService, ObjectMapper objectMapper) {
    return new KeycloakRealmService(objectMapper, keycloakClient, template, tokenService);
  }

  @Bean
  public KeycloakClientService keycloakClientService(KeycloakClient keycloakClient, KeycloakTemplate template) {
    return new KeycloakClientService(keycloakClient, template);
  }

  @Bean
  public KeycloakRoleService keycloakRoleService(KeycloakClient keycloakClient, KeycloakTemplate template) {
    return new KeycloakRoleService(keycloakClient, template);
  }

  @Bean
  public KeycloakPolicyService keycloakPolicyService(KeycloakClient keycloakClient, KeycloakTemplate template) {
    return new KeycloakPolicyService(keycloakClient, template);
  }

  @Bean
  public KeycloakPermissionService keycloakPermissionService(KeycloakClient keycloakClient, KeycloakTemplate template) {
    return new KeycloakPermissionService(keycloakClient, template);
  }

  @Bean
  public KeycloakAuthScopeService keycloakAuthScopeService(KeycloakClient keycloakClient, KeycloakTemplate template) {
    return new KeycloakAuthScopeService(keycloakClient, template);
  }

  @Bean
  public ClientSecretService clientSecretService(SecureStore secureStore,
                                                 KeycloakRealmSetupProperties setupProperties) {
    return new ClientSecretService(secureStore, setupProperties);
  }

  @Bean
  public KeycloakImpersonationService keycloakImpersonationService(ClientSecretService secretService,
                                                                   KeycloakPolicyService policyService,
                                                                   KeycloakClientService clientService,
                                                                   KeycloakPermissionService permissionService,
                                                                   KeycloakRealmSetupProperties properties) {
    return new KeycloakImpersonationService(secretService, policyService, clientService, permissionService, properties);
  }

  @Bean
  public TenantServiceListener keycloakTenantListener(KeycloakRealmService realmService,
                                                      KeycloakClientService clientService,
                                                      KeycloakRoleService roleService,
                                                      KeycloakPolicyService policyService,
                                                      KeycloakPermissionService permissionService,
                                                      KeycloakAuthScopeService authScopeService,
                                                      ClientSecretService clientSecretService,
                                                      KeycloakRealmSetupProperties setupProperties,
                                                      KeycloakImpersonationService keycloakImpersonationService) {
    return new KeycloakRealmManagementService(realmService, clientService, roleService,
      policyService, permissionService, authScopeService, clientSecretService,
      setupProperties, keycloakImpersonationService)
      .tenantServiceListener();
  }
}
