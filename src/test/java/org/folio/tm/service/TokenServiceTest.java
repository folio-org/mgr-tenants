package org.folio.tm.service;

import static java.util.Optional.ofNullable;
import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.tm.service.TokenServiceTest.TestCacheConfiguration;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.folio.security.integration.keycloak.configuration.properties.KeycloakAdminProperties;
import org.folio.test.types.UnitTest;
import org.folio.tm.integration.keycloak.KeycloakClient;
import org.folio.tm.integration.keycloak.TokenService;
import org.folio.tm.support.TestConstants;
import org.folio.tm.support.TestUtils;
import org.folio.tools.store.SecureStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;

@UnitTest
@SpringBootTest(classes = {TokenService.class, TestCacheConfiguration.class}, webEnvironment = NONE)
class TokenServiceTest {

  @Autowired
  private TokenService tokenService;
  @MockBean
  private KeycloakAdminProperties keycloakAdminProperties;
  @MockBean
  private KeycloakClient keycloakClient;
  @MockBean
  private SecureStore secureStore;
  @Autowired
  private CacheManager cacheManager;

  @BeforeEach
  void setUp() {
    TestUtils.cleanUpCaches(cacheManager);
  }

  @Test
  void issueToken_positive() {
    var token = TestConstants.tokenResponse();
    var accessToken = TestConstants.getAccessToken();

    when(secureStore.get(anyString())).thenReturn("secret");

    when(keycloakClient.login(loginRequest())).thenReturn(token);
    when(keycloakAdminProperties.getClientId()).thenReturn("admin-cli");
    when(keycloakAdminProperties.getUsername()).thenReturn("admin");
    when(keycloakAdminProperties.getPassword()).thenReturn("supersecret");
    when(keycloakAdminProperties.getGrantType()).thenReturn("password");

    var result = tokenService.issueToken();
    assertThat(result).isEqualTo(accessToken);
    assertThat(getCachedValue()).isEqualTo(Optional.of(accessToken));
  }

  private Optional<Object> getCachedValue() {
    return ofNullable(cacheManager.getCache(TestConstants.TOKEN_CACHE)).map(cache -> cache.get("admin-cli-token"))
      .map(Cache.ValueWrapper::get);
  }

  private static Map<String, String> loginRequest() {
    var loginRequest = new HashMap<String, String>();
    loginRequest.put("client_secret", "secret");
    loginRequest.put("client_id", "admin-cli");
    loginRequest.put("username", "admin");
    loginRequest.put("password", "supersecret");
    loginRequest.put("grant_type", "password");
    return loginRequest;
  }

  @EnableCaching
  @TestConfiguration
  static class TestCacheConfiguration {

    @Bean
    CacheManager cacheManager() {
      return new ConcurrentMapCacheManager(TestConstants.TOKEN_CACHE);
    }
  }
}
