package org.folio.tm.config;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import jakarta.servlet.http.HttpServletRequest;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.log4j.Log4j2;
import org.folio.tm.integration.okapi.OkapiHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.lang.NonNull;

@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorAware", dateTimeProviderRef = "dateTimeProvider", modifyOnCreate = false)
public class JpaAuditingConfig {

  @Bean
  public AuditorAware<UUID> auditorAware() {
    return new OkapiAuditorAware();
  }

  @Bean
  public DateTimeProvider dateTimeProvider() {
    return () -> Optional.of(OffsetDateTime.now());
  }

  @Log4j2
  static final class OkapiAuditorAware implements AuditorAware<UUID> {

    private static final UUID SYSTEM_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");
    private HttpServletRequest httpServletRequest;

    @NonNull
    @Override
    public Optional<UUID> getCurrentAuditor() {
      if (httpServletRequest != null && isNotBlank(httpServletRequest.getHeader(OkapiHeaders.USER_ID))) {
        var okapiUserId = httpServletRequest.getHeader(OkapiHeaders.USER_ID);
        try {
          return Optional.of(UUID.fromString(okapiUserId));
        } catch (IllegalArgumentException e) {
          log.warn("Failed to parse user id as UUID from header 'x-okapi-user-id', using default system id");
          return Optional.of(SYSTEM_USER_ID);
        }
      }
      // replace this temporary implementation when current user identity is available
      return Optional.of(SYSTEM_USER_ID);
    }

    @Autowired(required = false)
    @SuppressWarnings("SpringJavaAutowiredMembersInspection")
    public OkapiAuditorAware servletRequest(HttpServletRequest servletRequest) {
      this.httpServletRequest = servletRequest;
      return this;
    }
  }
}
