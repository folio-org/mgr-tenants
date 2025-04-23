package org.folio.tm.integration.keycloak.configuration;

import lombok.Data;
import org.folio.security.integration.keycloak.configuration.properties.KeycloakClientProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@ConfigurationProperties(prefix = "application.keycloak.realm-setup")
public class KeycloakRealmSetupProperties {
  @NestedConfigurationProperty
  private KeycloakClientProperties loginClient;
  @NestedConfigurationProperty
  private KeycloakClientProperties m2mClient;
  @NestedConfigurationProperty
  private PasswordResetClientProperties passwordResetClient;
  private Integer clientSecretLength;
  private String impersonationClient;
  private Integer accessCodeLifespan;
  private Integer parRequestUriLifespan;

  private Integer accessTokenLifespan;
  private Integer ssoSessionIdleTimeout;
  private Integer ssoSessionMaxLifespan;
  private Integer clientSessionIdleTimeout;
  private Integer clientSessionMaxLifespan;
}
