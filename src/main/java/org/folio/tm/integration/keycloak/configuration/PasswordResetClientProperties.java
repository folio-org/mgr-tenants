package org.folio.tm.integration.keycloak.configuration;

import jakarta.validation.constraints.Max;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@ConfigurationProperties
public class PasswordResetClientProperties {
  /**
   * Client identifier.
   */
  private String clientId;
  /**
   * Access Token Lifespan in seconds. Max value is 4 weeks in seconds.
   */
  @Max(2419200)
  private Long tokenLifespan;
}
