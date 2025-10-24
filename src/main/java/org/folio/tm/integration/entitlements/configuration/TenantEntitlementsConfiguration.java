package org.folio.tm.integration.entitlements.configuration;

import static org.folio.common.utils.tls.FeignClientTlsUtils.buildTargetFeignClient;

import feign.Contract;
import feign.codec.Decoder;
import feign.codec.Encoder;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.extern.log4j.Log4j2;
import okhttp3.OkHttpClient;
import org.folio.common.configuration.properties.TlsProperties;
import org.folio.tm.integration.entitlements.TenantEntitlementsClient;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.openfeign.FeignClientsConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.validation.annotation.Validated;

@Data
@Log4j2
@Validated
@Configuration
@Import(FeignClientsConfiguration.class)
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
   * Creates Feign client for tenant entitlements service.
   *
   * @param okHttpClient - HTTP client for Feign
   * @param contract - Feign contract
   * @param encoder - Feign encoder
   * @param decoder - Feign decoder
   * @return configured {@link TenantEntitlementsClient}
   */
  @Bean
  public TenantEntitlementsClient tenantEntitlementsClient(OkHttpClient okHttpClient, Contract contract,
    Encoder encoder, Decoder decoder) {
    log.info("Initializing TenantEntitlementsClient [url: {}]", url);
    return buildTargetFeignClient(okHttpClient, contract, encoder, decoder, tls, url,
      TenantEntitlementsClient.class);
  }
}
