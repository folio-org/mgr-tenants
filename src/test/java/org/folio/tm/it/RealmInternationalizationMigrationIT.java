package org.folio.tm.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;
import static org.springframework.test.context.jdbc.SqlMergeMode.MergeMode.MERGE;

import java.util.Objects;
import java.util.Set;
import org.folio.test.extensions.EnableKeycloakDataImport;
import org.folio.test.extensions.EnableKeycloakSecurity;
import org.folio.test.extensions.EnableKeycloakTlsMode;
import org.folio.test.extensions.KeycloakRealms;
import org.folio.test.types.IntegrationTest;
import org.folio.tm.base.BaseIntegrationTest;
import org.folio.tm.integration.keycloak.migration.AbstractRealmMigration.Summary;
import org.folio.tm.integration.keycloak.migration.RealmInternationalizationMigration;
import org.folio.tm.support.KeycloakTestClientConfiguration;
import org.folio.tm.support.KeycloakTestClientConfiguration.KeycloakTestClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;

@IntegrationTest
@SqlMergeMode(MERGE)
@EnableKeycloakTlsMode
@EnableKeycloakSecurity
@EnableKeycloakDataImport
@Import(KeycloakTestClientConfiguration.class)
@Sql(scripts = "classpath:/sql/clear_tenants.sql", executionPhase = AFTER_TEST_METHOD)
class RealmInternationalizationMigrationIT extends BaseIntegrationTest {

  private static final String TENANT1 = "tenant1";
  private static final String MASTER_REALM = "master";

  @Autowired private RealmInternationalizationMigration migration;
  @Autowired private KeycloakTestClient keycloakTestClient;

  @AfterEach
  void tearDown(@Autowired Keycloak keycloak) {
    var realms = keycloak.realms().findAll();
    for (var realm : realms) {
      var realmName = realm.getRealm();
      if (!Objects.equals(realmName, MASTER_REALM)) {
        keycloak.realm(realmName).remove();
      }
    }
  }

  @Test
  @Sql("classpath:/sql/populate_tenants.sql")
  @KeycloakRealms(realms = "/json/keycloak/tenant1.json")
  void migrate_positive_disabledRealmEnabled() {
    assertThat(keycloakTestClient.getRealm(TENANT1).isInternationalizationEnabled()).isNotEqualTo(true);
    var masterBefore = keycloakTestClient.getRealm(MASTER_REALM);

    var result = migration.migrate();

    // 4 tenants in the database, only tenant1 has a realm: the 3 missing ones must not break the run
    assertThat(result).isEqualTo(new Summary(4, 1, 0, 3, 0));
    var realm = keycloakTestClient.getRealm(TENANT1);
    assertThat(realm.isInternationalizationEnabled()).isTrue();
    assertThat(realm.getDefaultLocale()).isEqualTo("en");
    assertThat(realm.getSupportedLocales()).containsExactly("en");
    var masterAfter = keycloakTestClient.getRealm(MASTER_REALM);
    assertThat(masterAfter.isInternationalizationEnabled()).isEqualTo(masterBefore.isInternationalizationEnabled());
    assertThat(masterAfter.getSupportedLocales()).isEqualTo(masterBefore.getSupportedLocales());
  }

  @Test
  @Sql("classpath:/sql/populate_tenants.sql")
  @KeycloakRealms(realms = "/json/keycloak/tenant1.json")
  void migrate_positive_customLocalesPreserved() {
    keycloakTestClient.updateRealm(TENANT1, realm -> {
      realm.setInternationalizationEnabled(false);
      realm.setDefaultLocale("de");
      realm.setSupportedLocales(Set.of("de", "fr"));
    });

    var result = migration.migrate();

    assertThat(result).isEqualTo(new Summary(4, 1, 0, 3, 0));
    var realm = keycloakTestClient.getRealm(TENANT1);
    assertThat(realm.isInternationalizationEnabled()).isTrue();
    assertThat(realm.getDefaultLocale()).isEqualTo("de");
    assertThat(realm.getSupportedLocales()).containsExactlyInAnyOrder("de", "fr");
  }

  @Test
  @Sql("classpath:/sql/populate_tenants.sql")
  @KeycloakRealms(realms = "/json/keycloak/tenant1.json")
  void migrate_positive_secondRunIdempotent() {
    migration.migrate();
    var realmBefore = keycloakTestClient.getRealm(TENANT1);

    var result = migration.migrate();

    assertThat(result).isEqualTo(new Summary(4, 0, 1, 3, 0));
    assertThat(keycloakTestClient.getRealm(TENANT1)).usingRecursiveComparison().isEqualTo(realmBefore);
  }

  @Test
  @Sql("classpath:/sql/populate_tenants.sql")
  @KeycloakRealms(realms = "/json/keycloak/tenant1.json")
  void migrate_positive_unrelatedSettingsUntouched() {
    var realmBefore = keycloakTestClient.getRealm(TENANT1);

    migration.migrate();

    var realmAfter = keycloakTestClient.getRealm(TENANT1);
    assertThat(realmAfter)
      .usingRecursiveComparison()
      .ignoringFields("internationalizationEnabled", "defaultLocale", "supportedLocales", "attributes")
      .isEqualTo(realmBefore);
    // Keycloak materializes server-side default attributes (e.g. scimApiEnabled) on any realm update
    assertThat(realmAfter.getAttributes()).containsAllEntriesOf(realmBefore.getAttributes());
  }
}
