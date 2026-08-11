package org.folio.tm.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;
import static org.springframework.test.context.jdbc.SqlMergeMode.MergeMode.MERGE;

import java.util.Objects;
import org.folio.test.extensions.EnableKeycloakDataImport;
import org.folio.test.extensions.EnableKeycloakSecurity;
import org.folio.test.extensions.EnableKeycloakTlsMode;
import org.folio.test.extensions.KeycloakRealms;
import org.folio.test.types.IntegrationTest;
import org.folio.tm.base.BaseIntegrationTest;
import org.folio.tm.integration.keycloak.migration.UnmanagedAttributePolicyMigration;
import org.folio.tm.integration.keycloak.migration.UnmanagedAttributePolicyMigration.Summary;
import org.folio.tm.support.KeycloakTestClientConfiguration;
import org.folio.tm.support.KeycloakTestClientConfiguration.KeycloakTestClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.userprofile.config.UPAttribute;
import org.keycloak.representations.userprofile.config.UPConfig.UnmanagedAttributePolicy;
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
class UnmanagedAttributePolicyMigrationIT extends BaseIntegrationTest {

  private static final String TENANT1 = "tenant1";
  private static final String MASTER_REALM = "master";

  @Autowired private UnmanagedAttributePolicyMigration migration;
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
  void migrate_positive_enabledPolicyReplaced() {
    setPolicy(TENANT1, UnmanagedAttributePolicy.ENABLED);
    var masterPolicyBefore = keycloakTestClient.getUserProfileConfig(MASTER_REALM).getUnmanagedAttributePolicy();

    var result = migration.migrate();

    // 4 tenants in the database, only tenant1 has a realm: the 3 missing ones must not break the run
    assertThat(result).isEqualTo(new Summary(4, 1, 0, 3, 0));
    assertThat(keycloakTestClient.getUserProfileConfig(TENANT1).getUnmanagedAttributePolicy())
      .isEqualTo(UnmanagedAttributePolicy.ADMIN_EDIT);
    assertThat(keycloakTestClient.getUserProfileConfig(MASTER_REALM).getUnmanagedAttributePolicy())
      .isEqualTo(masterPolicyBefore);
  }

  @Test
  @Sql("classpath:/sql/populate_tenants.sql")
  @KeycloakRealms(realms = "/json/keycloak/tenant1.json")
  void migrate_positive_nullPolicyReplaced() {
    // a realm imported without user-profile configuration has no unmanaged attribute policy (= disabled)
    assertThat(keycloakTestClient.getUserProfileConfig(TENANT1).getUnmanagedAttributePolicy()).isNull();

    var result = migration.migrate();

    assertThat(result).isEqualTo(new Summary(4, 1, 0, 3, 0));
    assertThat(keycloakTestClient.getUserProfileConfig(TENANT1).getUnmanagedAttributePolicy())
      .isEqualTo(UnmanagedAttributePolicy.ADMIN_EDIT);
  }

  @Test
  @Sql("classpath:/sql/populate_tenants.sql")
  @KeycloakRealms(realms = "/json/keycloak/tenant1.json")
  void migrate_positive_alreadyAdminEditUnchanged() {
    setPolicy(TENANT1, UnmanagedAttributePolicy.ADMIN_EDIT);
    var configBefore = keycloakTestClient.getUserProfileConfig(TENANT1);

    var result = migration.migrate();

    assertThat(result).isEqualTo(new Summary(4, 0, 1, 3, 0));
    var configAfter = keycloakTestClient.getUserProfileConfig(TENANT1);
    assertThat(configAfter).isEqualTo(configBefore);
    assertThat(configAfter.getAttributes()).extracting(UPAttribute::getName).contains("username", "email");
  }

  private void setPolicy(String realm, UnmanagedAttributePolicy policy) {
    var config = keycloakTestClient.getUserProfileConfig(realm);
    config.setUnmanagedAttributePolicy(policy);
    keycloakTestClient.updateUserProfileConfig(realm, config);
  }
}
