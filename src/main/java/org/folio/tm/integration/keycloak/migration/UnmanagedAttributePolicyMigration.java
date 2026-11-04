package org.folio.tm.integration.keycloak.migration;

import org.folio.tm.integration.keycloak.KeycloakRealmService;
import org.folio.tm.repository.TenantRepository;

/**
 * Idempotent: realms already having ADMIN_EDIT are not updated. A failure for one realm is
 * logged with the {@link #LOG_MARKER} and does not stop the migration for remaining realms.
 * Only realms of tenants managed by this module are touched, so the {@code master} realm and
 * any non-tenant realms are never affected.
 */
public class UnmanagedAttributePolicyMigration extends AbstractRealmMigration {

  static final String LOG_MARKER = "REALM SETTINGS MIGRATION";

  private final KeycloakRealmService keycloakRealmService;

  public UnmanagedAttributePolicyMigration(TenantRepository tenantRepository,
    KeycloakRealmService keycloakRealmService) {
    super(tenantRepository, LOG_MARKER, "user-profile unmanagedAttributePolicy=ADMIN_EDIT");
    this.keycloakRealmService = keycloakRealmService;
  }

  @Override
  protected boolean enforce(String realmName) {
    return keycloakRealmService.ensureUnmanagedAttributePolicy(realmName);
  }
}
