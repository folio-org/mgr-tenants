package org.folio.tm.base;

import lombok.extern.log4j.Log4j2;
import org.folio.test.base.BaseBackendIntegrationTest;
import org.folio.test.extensions.EnableWireMock;
import org.folio.tm.exception.RequestValidationException;
import org.folio.tm.extension.EnablePostgres;
import org.folio.tm.support.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.ResultMatcher;

@Log4j2
@EnableWireMock
@EnablePostgres
@SpringBootTest
@ActiveProfiles("it")
@AutoConfigureMockMvc
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public abstract class BaseIntegrationTest extends BaseBackendIntegrationTest {

  static {
    TestUtils.disableSslVerification();
  }

  @Autowired
  private CacheManager cacheManager;

  @BeforeEach
  void setUp() {
    TestUtils.cleanUpCaches(cacheManager);
  }

  protected static ResultMatcher[] requestValidationErr(String errMsg, String fieldName, Object fieldValue) {
    return validationErr(RequestValidationException.class.getSimpleName(), errMsg, fieldName, fieldValue);
  }
}
