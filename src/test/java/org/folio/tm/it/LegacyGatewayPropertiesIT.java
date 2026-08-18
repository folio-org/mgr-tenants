package org.folio.tm.it;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.folio.test.types.IntegrationTest;
import org.folio.tools.kong.configuration.ApiGatewayAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;

/**
 * Boots a minimal, real {@link org.springframework.boot.SpringApplication} (no active profile, so
 * {@code application-it.yml}'s hard-coded {@code application.apigw.*} overrides stay out of play) to verify
 * that the legacy {@code application.kong.*} / {@code KONG_*} configuration still resolves through the
 * {@code application.apigw.*} chain declared in {@code application.yml}, and that the library's deprecation
 * warning fires for it.
 */
@IntegrationTest
class LegacyGatewayPropertiesIT {

  private static final String DEPRECATION_WARNING =
    "Configuration property 'application.kong.enabled' is deprecated and will be removed "
      + "in the Vetch release. Use 'application.apigw.enabled' instead.";

  @Test
  void legacyKongPropertiesOnly_bindApiGatewayBean_andLogDeprecationWarning() {
    var warnAppender = new WarnCollectingAppender();
    try (var context = bootContext(warnAppender,
      "--application.kong.enabled=true",
      "--application.kong.register-module=false")) {

      assertThat(context.containsBean("folioKongAdminClient")).isTrue();
      assertThat(warnAppender.messages()).anyMatch(message -> message.contains(DEPRECATION_WARNING));
    } finally {
      warnAppender.detach();
    }
  }

  @Test
  void newEnvVarOverridesLegacyEnvVar_bindsApiGatewayBeans() {
    try (var context = bootContext(null,
      "--KONG_INTEGRATION_ENABLED=false",
      "--APIGW_ENABLED=true",
      "--REGISTER_MODULE_IN_KONG=false")) {

      assertThat(context.containsBean("folioKongAdminClient")).isTrue();
      assertThat(context.containsBean("folioKongGatewayService")).isTrue();
      assertThat(context.containsBean("folioKongRouteTenantService")).isTrue();
    }
  }

  /**
   * Boots the app with the given args. When {@code warnAppender} is non-null, it is attached to the root
   * logger only once real logging is initialized for this run (on {@link ApplicationEnvironmentPreparedEvent},
   * after {@code LoggingApplicationListener} has (re)configured Log4j2) so that it observes the deprecation
   * warning, which Spring Boot buffers and replays through the real logger later, at
   * {@code ApplicationPreparedEvent}.
   */
  private static ConfigurableApplicationContext bootContext(WarnCollectingAppender warnAppender, String... args) {
    var builder = new SpringApplicationBuilder(TestConfig.class).web(WebApplicationType.NONE);
    if (warnAppender != null) {
      builder.listeners((ApplicationListener<ApplicationEnvironmentPreparedEvent>) event -> warnAppender.attach());
    }
    return builder.run(args);
  }

  @Configuration
  @ImportAutoConfiguration({JacksonAutoConfiguration.class, ApiGatewayAutoConfiguration.class})
  static class TestConfig {
  }

  private static final class WarnCollectingAppender extends AbstractAppender {

    private final List<String> messages = Collections.synchronizedList(new ArrayList<>());

    private WarnCollectingAppender() {
      super("warn-collector-" + UUID.randomUUID(), null, null, true, Property.EMPTY_ARRAY);
      start();
    }

    void attach() {
      var context = (LoggerContext) LogManager.getContext(false);
      context.getConfiguration().getRootLogger().addAppender(this, null, null);
      context.updateLoggers();
    }

    void detach() {
      var context = (LoggerContext) LogManager.getContext(false);
      context.getConfiguration().getRootLogger().removeAppender(getName());
      context.updateLoggers();
    }

    @Override
    public void append(LogEvent event) {
      messages.add(event.getMessage().getFormattedMessage());
    }

    List<String> messages() {
      return messages;
    }
  }
}
