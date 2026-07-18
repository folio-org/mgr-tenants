package org.folio.tm.integration.keycloak.migration;

import jakarta.ws.rs.NotFoundException;
import java.util.EnumMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.tm.integration.keycloak.KeycloakRealmService;
import org.folio.tm.repository.TenantRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;

/**
 * Startup migration enforcing the Keycloak user-profile {@code unmanagedAttributePolicy=ADMIN_EDIT}
 * on all existing tenant realms (MGRTENANT-95, SECURITY-1029 remediation).
 *
 * <p>Idempotent: realms already having ADMIN_EDIT are not updated. A failure for one realm is
 * logged with the {@link #LOG_MARKER} and does not stop the migration for remaining realms.
 * Only realms of tenants managed by this module are touched, so the {@code master} realm and
 * any non-tenant realms are never affected.</p>
 */
@Log4j2
@RequiredArgsConstructor
public class UnmanagedAttributePolicyMigration implements ApplicationRunner {

  static final String LOG_MARKER = "MGRTENANT-95";
  private static final String MASTER_REALM = "master";

  private final TenantRepository tenantRepository;
  private final KeycloakRealmService keycloakRealmService;

  @Override
  public void run(ApplicationArguments args) {
    try {
      migrate();
    } catch (Exception exception) {
      log.error("{}: unmanaged attribute policy migration failed to complete", LOG_MARKER, exception);
    }
  }

  /**
   * Applies the ADMIN_EDIT policy to every tenant realm that does not have it yet.
   *
   * @return migration {@link Summary} with per-outcome counts
   */
  public Summary migrate() {
    var tenants = tenantRepository.findAll().stream()
      .filter(tenant -> !MASTER_REALM.equals(tenant.getName()))
      .toList();
    log.info("{}: enforcing user-profile unmanagedAttributePolicy=ADMIN_EDIT on {} tenant realm(s)",
      LOG_MARKER, tenants.size());

    var counters = new EnumMap<Outcome, Integer>(Outcome.class);
    for (var tenant : tenants) {
      counters.merge(enforceForRealm(tenant.getName()), 1, Integer::sum);
    }

    var summary = new Summary(tenants.size(),
      counters.getOrDefault(Outcome.UPDATED, 0), counters.getOrDefault(Outcome.ALREADY_COMPLIANT, 0),
      counters.getOrDefault(Outcome.MISSING, 0), counters.getOrDefault(Outcome.FAILED, 0));
    log.info("{}: migration finished [scanned: {}, updated: {}, alreadyCompliant: {}, missing: {}, failed: {}]",
      LOG_MARKER, summary.scanned(), summary.updated(), summary.alreadyCompliant(), summary.missing(),
      summary.failed());
    return summary;
  }

  private Outcome enforceForRealm(String realmName) {
    try {
      if (keycloakRealmService.ensureUnmanagedAttributePolicy(realmName)) {
        log.info("{}: realm '{}': unmanagedAttributePolicy updated to ADMIN_EDIT", LOG_MARKER, realmName);
        return Outcome.UPDATED;
      }

      log.debug("{}: realm '{}': already ADMIN_EDIT, skipping", LOG_MARKER, realmName);
      return Outcome.ALREADY_COMPLIANT;
    } catch (NotFoundException exception) {
      log.warn("{}: realm '{}' not found in Keycloak, skipping", LOG_MARKER, realmName);
      return Outcome.MISSING;
    } catch (Exception exception) {
      return logFailure(realmName, exception);
    }
  }

  private Outcome logFailure(String realmName, Exception exception) {
    log.error("{}: failed to enforce ADMIN_EDIT for realm '{}'", LOG_MARKER, realmName, exception);
    return Outcome.FAILED;
  }

  /**
   * Aggregate result of a migration run.
   *
   * @param scanned          - number of tenants read from the database
   * @param updated          - realms updated to ADMIN_EDIT
   * @param alreadyCompliant - realms already having ADMIN_EDIT
   * @param missing          - tenants without a Keycloak realm
   * @param failed           - realms that failed to update
   */
  public record Summary(int scanned, int updated, int alreadyCompliant, int missing, int failed) {}

  enum Outcome {
    UPDATED, ALREADY_COMPLIANT, MISSING, FAILED
  }
}
