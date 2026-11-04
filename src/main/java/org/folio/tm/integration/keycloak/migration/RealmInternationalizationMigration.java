package org.folio.tm.integration.keycloak.migration;

import org.folio.tm.integration.keycloak.KeycloakRealmService;
import org.folio.tm.repository.TenantRepository;

/**
 * Enables Keycloak realm Internationalization (so login pages expose the {@code lang} attribute) on every
 * tenant realm. Locale values already configured on a realm are preserved; realms that are already
 * compliant are not updated. Failures are logged with the {@link #LOG_MARKER} and never stop the migration.
 */
public class RealmInternationalizationMigration extends AbstractRealmMigration {

  static final String LOG_MARKER = "REALM I18N MIGRATION";

  private final KeycloakRealmService keycloakRealmService;

  public RealmInternationalizationMigration(TenantRepository tenantRepository,
    KeycloakRealmService keycloakRealmService) {
    super(tenantRepository, LOG_MARKER, "realm internationalization");
    this.keycloakRealmService = keycloakRealmService;
  }

  @Override
  protected boolean enforce(String realmName) {
    return keycloakRealmService.ensureInternationalization(realmName);
  }
}
