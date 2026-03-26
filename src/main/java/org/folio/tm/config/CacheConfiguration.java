package org.folio.tm.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.List;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
@EnableConfigurationProperties(CacheProperties.class)
public class CacheConfiguration {

  private static final String TOKEN_CACHE = "token";

  @Bean
  public CacheManager cacheManager(CacheProperties cacheProperties) {
    var cacheManager = new SimpleCacheManager();
    cacheManager.setCaches(List.of(buildCache(TOKEN_CACHE, cacheProperties.token())));
    return cacheManager;
  }

  private static CaffeineCache buildCache(String name, CacheProperties.CacheSpec spec) {
    return new CaffeineCache(name, Caffeine.newBuilder()
      .maximumSize(spec.maximumSize())
      .expireAfterWrite(spec.ttl())
      .build());
  }
}
