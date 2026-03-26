package org.folio.tm.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.folio.test.types.UnitTest;
import org.folio.tm.config.JpaAuditingConfig.OkapiAuditorAware;
import org.folio.tm.integration.okapi.OkapiHeaders;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class JpaAuditingConfigTest {

  private static final UUID SYSTEM_UUID = UUID.fromString("00000000-0000-0000-0000-000000000000");

  @InjectMocks private JpaAuditingConfig jpaAuditingConfig;
  @Mock private HttpServletRequest httpServletRequest;

  @Test
  void auditorAware_positive_withHttpServletRequest() {
    var userId = UUID.randomUUID();

    var uuidAuditorAware = (OkapiAuditorAware) jpaAuditingConfig.auditorAware();
    uuidAuditorAware.servletRequest(httpServletRequest);
    when(httpServletRequest.getHeader(OkapiHeaders.USER_ID)).thenReturn(userId.toString());

    var actual = uuidAuditorAware.getCurrentAuditor();

    assertThat(actual).isPresent().get().isEqualTo(userId);
  }

  @Test
  void auditorAware_positive_withHttpServletRequestAndInvalidUuid() {
    var uuidAuditorAware = (OkapiAuditorAware) jpaAuditingConfig.auditorAware();
    uuidAuditorAware.servletRequest(httpServletRequest);
    when(httpServletRequest.getHeader(OkapiHeaders.USER_ID)).thenReturn("unknown");

    var actual = uuidAuditorAware.getCurrentAuditor();

    assertThat(actual).isPresent().get().isEqualTo(SYSTEM_UUID);
  }

  @Test
  void auditorAware_positive_withHttpServletRequestAndEmptyHeader() {
    var uuidAuditorAware = (OkapiAuditorAware) jpaAuditingConfig.auditorAware();
    uuidAuditorAware.servletRequest(httpServletRequest);
    when(httpServletRequest.getHeader(OkapiHeaders.USER_ID)).thenReturn(null);

    var actual = uuidAuditorAware.getCurrentAuditor();

    assertThat(actual).isPresent().get().isEqualTo(SYSTEM_UUID);
  }

  @Test
  void auditorAware_positive_withoutHttpServletRequest() {
    var uuidAuditorAware = jpaAuditingConfig.auditorAware();
    var actual = uuidAuditorAware.getCurrentAuditor();
    assertThat(actual).isPresent().get().isEqualTo(SYSTEM_UUID);
  }

  @Test
  void dateTimeProvider() {
    var actual = jpaAuditingConfig.dateTimeProvider();
    assertThat(actual).isNotNull();
    assertThat(actual.getNow()).isPresent().get().isInstanceOf(OffsetDateTime.class);
  }
}
