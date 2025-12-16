package org.folio.tm.integration.keycloak.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties
public class KeycloakRefreshTokenProperties {

  private Boolean revokeEnabled = true;
  private Integer maxReuse = 0;
}
