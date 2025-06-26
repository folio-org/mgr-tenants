package org.folio.tm.integration.kafka.configuration;

import org.apache.kafka.clients.admin.AdminClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaAdmin;

@Configuration
public class KafkaAdminConfiguration {

  @Bean
  public AdminClient adminClient(@Autowired KafkaAdmin kafkaAdmin) {
    return AdminClient.create(kafkaAdmin.getConfigurationProperties());
  }
}
