package org.folio.tm.it;

import static org.assertj.core.api.Assertions.assertThat;

import org.folio.test.extensions.EnableKeycloakDataImport;
import org.folio.test.extensions.EnableKeycloakSecurity;
import org.folio.test.extensions.EnableKeycloakTlsMode;
import org.folio.test.types.IntegrationTest;
import org.folio.tm.base.BaseIntegrationTest;
import org.folio.tm.integration.keycloak.migration.RealmInternationalizationMigration;
import org.folio.tm.integration.keycloak.migration.UnmanagedAttributePolicyMigration;
import org.folio.tm.support.KeycloakTestClientConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

@IntegrationTest
@EnableKeycloakTlsMode
@EnableKeycloakSecurity
@EnableKeycloakDataImport
@Import(KeycloakTestClientConfiguration.class)
@TestPropertySource(properties = "application.keycloak.realm-setup.localization.migration-enabled=false")
class RealmInternationalizationMigrationDisabledIT extends BaseIntegrationTest {

  @Autowired private ObjectProvider<RealmInternationalizationMigration> internationalizationMigration;
  @Autowired private ObjectProvider<UnmanagedAttributePolicyMigration> unmanagedAttributePolicyMigration;

  @Test
  void migrationBean_positive_notRegisteredWhenDisabled() {
    assertThat(internationalizationMigration.getIfAvailable()).isNull();
    assertThat(unmanagedAttributePolicyMigration.getIfAvailable()).isNotNull();
  }
}
