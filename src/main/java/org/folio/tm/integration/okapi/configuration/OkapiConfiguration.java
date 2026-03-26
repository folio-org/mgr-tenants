package org.folio.tm.integration.okapi.configuration;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.log4j.Log4j2;
import org.folio.tm.integration.okapi.OkapiClient;
import org.folio.tm.integration.okapi.OkapiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Log4j2
@Configuration
@ConditionalOnProperty("application.okapi.enabled")
public class OkapiConfiguration {

  @Bean
  public OkapiClient okapiClient(OkapiConfigurationProperties configuration) {
    var restClient = RestClient.builder()
      .baseUrl(configuration.getUrl())
      .build();
    var adapter = RestClientAdapter.create(restClient);
    var factory = HttpServiceProxyFactory.builderFor(adapter).build();
    return factory.createClient(OkapiClient.class);
  }

  @Bean
  public OkapiService okapiService(OkapiClient okapiClient,
                                   @Autowired(required = false) HttpServletRequest httpServletRequest) {
    return new OkapiService(okapiClient, httpServletRequest);
  }
}
