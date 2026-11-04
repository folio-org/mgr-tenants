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
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;

@IntegrationTest
@SqlMergeMode(MERGE)
@EnableKeycloakTlsMode
@EnableKeycloakSecurity
@EnableKeycloakDataImport
@Import(KeycloakTestClientConfiguration.class)
@Sql(scripts = "classpath:/sql/clear_tenants.sql", executionPhase = AFTER_TEST_METHOD)
@TestPropertySource(properties = "application.keycloak.realm-setup.localization.supported-locales=en,de,fr")
class RealmInternationalizationMigrationSeveralLocalesIT extends BaseIntegrationTest {

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
  void migrate_positive_severalSupportedLocalesPopulated() {
    var result = migration.migrate();

    assertThat(result).isEqualTo(new Summary(4, 1, 0, 3, 0));
    var realm = keycloakTestClient.getRealm(TENANT1);
    assertThat(realm.isInternationalizationEnabled()).isTrue();
    assertThat(realm.getDefaultLocale()).isEqualTo("en");
    assertThat(realm.getSupportedLocales()).containsExactlyInAnyOrder("en", "de", "fr");
  }

  @Test
  @Sql("classpath:/sql/populate_tenants.sql")
  @KeycloakRealms(realms = "/json/keycloak/tenant1.json")
  void migrate_positive_existingSupportedLocalesNotExtended() {
    keycloakTestClient.updateRealm(TENANT1, realm -> {
      realm.setInternationalizationEnabled(true);
      realm.setDefaultLocale("de");
      realm.setSupportedLocales(Set.of("de"));
    });

    var result = migration.migrate();

    assertThat(result).isEqualTo(new Summary(4, 0, 1, 3, 0));
    assertThat(keycloakTestClient.getRealm(TENANT1).getSupportedLocales()).containsExactly("de");
  }
}
