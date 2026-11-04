package org.folio.tm.integration.keycloak.configuration;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.Data;

@Data
public class KeycloakLocalizationProperties {

  private String defaultLocale = "en";
  private Set<String> supportedLocales = new LinkedHashSet<>(List.of("en"));
  private boolean migrationEnabled = true;
}
