package org.folio.tm.it;

import static org.assertj.core.api.Assertions.assertThat;

import org.folio.test.extensions.EnableOkapiSecurity;
import org.folio.test.types.IntegrationTest;
import org.folio.tm.base.BaseIntegrationTest;
import org.folio.tm.extension.EnableKongGateway;
import org.folio.tools.kong.client.KongAdminClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

@IntegrationTest
@EnableKongGateway
@EnableOkapiSecurity
@TestPropertySource(properties = {
  "application.keycloak.enabled=false",
  "application.okapi.enabled=false",
  "application.kong.enabled=true",
  "application.kong.module-self-url=https://test-mgr-tenants:443",
  "application.kong.register-module=true"
})
class KongRegistrationIT extends BaseIntegrationTest {

  @Autowired private KongAdminClient kongAdminClient;

  @Test
  void verifyModuleRegistration() {
    var moduleName = "mgr-tenants-1.0.0";
    var service = kongAdminClient.getService(moduleName);
    assertThat(service).satisfies(s -> {
      assertThat(s.getProtocol()).isEqualTo("https");
      assertThat(s.getPort()).isEqualTo(443);
      assertThat(s.getHost()).isEqualTo("test-mgr-tenants");
    });

    var routes = kongAdminClient.getRoutesByTag(moduleName, null);
    assertThat(routes.getData()).hasSize(10);
  }
}
