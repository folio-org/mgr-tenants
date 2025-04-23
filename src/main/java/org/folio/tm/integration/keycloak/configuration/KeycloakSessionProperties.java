package org.folio.tm.integration.keycloak.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties
public class KeycloakSessionProperties {

  private Integer idleTimeout;
  private Integer maxLifespan;
}
