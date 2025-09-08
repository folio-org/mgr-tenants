package org.folio.tm.integration.okapi.configuration;

import feign.Contract;
import feign.Feign;
import feign.codec.Decoder;
import feign.codec.Encoder;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.log4j.Log4j2;
import org.folio.tm.integration.okapi.OkapiClient;
import org.folio.tm.integration.okapi.OkapiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.openfeign.FeignClientsConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Log4j2
@Configuration
@Import(FeignClientsConfiguration.class)
@ConditionalOnProperty("application.okapi.enabled")
public class OkapiConfiguration {

  @Bean
  public OkapiClient okapiClient(OkapiConfigurationProperties configuration,
                                 Contract contract, Encoder encoder, Decoder decoder) {
    return Feign.builder()
      .contract(contract).encoder(encoder).decoder(decoder)
      .target(OkapiClient.class, configuration.getUrl());
  }

  @Bean
  public OkapiService okapiService(OkapiClient okapiClient,
                                   @Autowired(required = false) HttpServletRequest httpServletRequest) {
    return new OkapiService(okapiClient, httpServletRequest);
  }
}
