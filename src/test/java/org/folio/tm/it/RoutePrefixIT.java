package org.folio.tm.it;

import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_METHOD;

import org.folio.test.types.IntegrationTest;
import org.folio.tm.base.BaseIntegrationTest;
import org.folio.tm.support.TestConstants;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;

@IntegrationTest
@Sql(scripts = "classpath:/sql/populate_tenants.sql", executionPhase = BEFORE_TEST_METHOD)
@Sql(scripts = "classpath:/sql/clear_tenants.sql", executionPhase = AFTER_TEST_METHOD)
@TestPropertySource(properties = {
  "application.router.path-prefix=mgr-tenants"
})
class RoutePrefixIT extends BaseIntegrationTest {

  @Test
  void getById_positive() throws Exception {
    doGet("/mgr-tenants/tenants/{id}", TestConstants.TENANT_ID)
      .andExpect(json("tenant/get-tenant-response.json"));
  }
}
