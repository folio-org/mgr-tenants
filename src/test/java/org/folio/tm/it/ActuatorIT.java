package org.folio.tm.it;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import org.folio.test.types.IntegrationTest;
import org.folio.tm.base.BaseIntegrationTest;
import org.junit.jupiter.api.Test;

@IntegrationTest
class ActuatorIT extends BaseIntegrationTest {

  @Test
  void getContainerHealth_positive() throws Exception {
    doGet("/admin/health").andExpect(jsonPath("$.status", is("UP")));
  }
}
