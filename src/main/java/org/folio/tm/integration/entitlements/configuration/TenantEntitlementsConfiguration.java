package org.folio.tm.integration.entitlements.configuration;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.extern.log4j.Log4j2;
import org.folio.common.configuration.properties.TlsProperties;
import org.folio.common.utils.tls.HttpClientTlsUtils;
import org.folio.tm.integration.entitlements.TenantEntitlementsClient;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.client.RestClient;

@Data
@Log4j2
@Validated
@Configuration
@ConfigurationProperties(prefix = "application.mte")
public class TenantEntitlementsConfiguration {

  /**
   * Base URL for mgr-tenant-entitlements service.
   */
  @NotBlank(message = "Tenant entitlements service URL must be configured")
  private String url;

  /**
   * TLS configuration for secure communication.
   */
  private TlsProperties tls;

  /**
   * Creates HTTP service client for tenant entitlements service.
   *
   * @return configured {@link TenantEntitlementsClient}
   */
  @Bean
  public TenantEntitlementsClient tenantEntitlementsClient() {
    return HttpClientTlsUtils.buildHttpServiceClient(
      RestClient.builder(), tls, url, TenantEntitlementsClient.class);
  }
}
