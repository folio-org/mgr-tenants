package org.folio.tm.configuration;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "application.cache")
public record CacheProperties(CacheSpec token) {

  public record CacheSpec(Duration ttl, long maximumSize) {}
}
