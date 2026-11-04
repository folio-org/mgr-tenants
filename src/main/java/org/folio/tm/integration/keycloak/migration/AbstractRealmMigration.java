package org.folio.tm.integration.keycloak.migration;

import jakarta.ws.rs.NotFoundException;
import java.util.EnumMap;
import lombok.extern.log4j.Log4j2;
import org.folio.tm.repository.TenantRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;

/**
 * Base for idempotent startup migrations that enforce a setting on every tenant realm.
 *
 * <p>A failure for one realm is logged with the migration log marker and does not stop the migration for
 * the remaining realms. Only realms of tenants managed by this module are touched, so the {@code master}
 * realm and any non-tenant realms are never affected.</p>
 */
@Log4j2
public abstract class AbstractRealmMigration implements ApplicationRunner {

  private static final String MASTER_REALM = "master";

  private final TenantRepository tenantRepository;
  private final String logMarker;
  private final String setting;

  /**
   * Creates the migration.
   *
   * @param tenantRepository - repository used to list tenants managed by this module
   * @param logMarker        - marker prefixed to every log line of the migration
   * @param setting          - human-readable name of the enforced setting, used in log lines
   */
  protected AbstractRealmMigration(TenantRepository tenantRepository, String logMarker, String setting) {
    this.tenantRepository = tenantRepository;
    this.logMarker = logMarker;
    this.setting = setting;
  }

  @Override
  public void run(ApplicationArguments args) {
    try {
      migrate();
    } catch (Exception exception) {
      log.error("{}: {} migration failed to complete", logMarker, setting, exception);
    }
  }

  /**
   * Applies the migration to every tenant realm that is not compliant yet.
   *
   * @return migration {@link Summary} with per-outcome counts
   */
  public Summary migrate() {
    var tenants = tenantRepository.findAll().stream()
      .filter(tenant -> !MASTER_REALM.equals(tenant.getName()))
      .toList();
    log.info("{}: enforcing {} on {} tenant realm(s)", logMarker, setting, tenants.size());

    var counters = new EnumMap<Outcome, Integer>(Outcome.class);
    for (var tenant : tenants) {
      counters.merge(enforceForRealm(tenant.getName()), 1, Integer::sum);
    }

    var summary = new Summary(tenants.size(),
      counters.getOrDefault(Outcome.UPDATED, 0), counters.getOrDefault(Outcome.ALREADY_COMPLIANT, 0),
      counters.getOrDefault(Outcome.MISSING, 0), counters.getOrDefault(Outcome.FAILED, 0));
    log.info("{}: migration finished [scanned: {}, updated: {}, alreadyCompliant: {}, missing: {}, failed: {}]",
      logMarker, summary.scanned(), summary.updated(), summary.alreadyCompliant(), summary.missing(),
      summary.failed());
    return summary;
  }

  /**
   * Applies the migration to a single realm. {@link NotFoundException} must propagate for a missing realm.
   *
   * @param realmName - Keycloak realm name
   * @return true if the realm was updated, false if it was already compliant
   */
  protected abstract boolean enforce(String realmName);

  private Outcome enforceForRealm(String realmName) {
    try {
      if (enforce(realmName)) {
        log.info("{}: realm '{}': {} updated", logMarker, realmName, setting);
        return Outcome.UPDATED;
      }

      log.debug("{}: realm '{}': {} already compliant, skipping", logMarker, realmName, setting);
      return Outcome.ALREADY_COMPLIANT;
    } catch (NotFoundException exception) {
      log.warn("{}: realm '{}' not found in Keycloak, skipping", logMarker, realmName);
      return Outcome.MISSING;
    } catch (Exception exception) {
      log.error("{}: failed to enforce {} for realm '{}'", logMarker, setting, realmName, exception);
      return Outcome.FAILED;
    }
  }

  /**
   * Aggregate result of a migration run.
   *
   * @param scanned          - number of tenants read from the database
   * @param updated          - realms updated by the migration
   * @param alreadyCompliant - realms that already had the setting
   * @param missing          - tenants without a Keycloak realm
   * @param failed           - realms that failed to update
   */
  public record Summary(int scanned, int updated, int alreadyCompliant, int missing, int failed) {}

  enum Outcome {
    UPDATED, ALREADY_COMPLIANT, MISSING, FAILED
  }
}
