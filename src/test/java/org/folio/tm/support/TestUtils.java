package org.folio.tm.support;

import static java.util.Objects.requireNonNull;
import static javax.net.ssl.HttpsURLConnection.setDefaultHostnameVerifier;
import static javax.net.ssl.HttpsURLConnection.setDefaultSSLSocketFactory;
import static javax.net.ssl.SSLContext.getInstance;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.util.ReflectionTestUtils.getField;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.net.Socket;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Objects;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509ExtendedTrustManager;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.springframework.cache.CacheManager;
import org.springframework.test.web.servlet.MvcResult;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class TestUtils {

  public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
    .setSerializationInclusion(Include.NON_NULL)
    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    .configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);

  @SneakyThrows
  public static String asJsonString(Object value) {
    return OBJECT_MAPPER.writeValueAsString(value);
  }

  @SneakyThrows
  public static <T> T parseResponse(MvcResult result, Class<T> type) {
    return OBJECT_MAPPER.readValue(result.getResponse().getContentAsString(), type);
  }

  @SneakyThrows
  public static <T> T parseResponse(MvcResult result, TypeReference<T> type) {
    return OBJECT_MAPPER.readValue(result.getResponse().getContentAsString(), type);
  }

  @SneakyThrows
  public static String readString(String path) {
    var resource = TestUtils.class.getClassLoader().getResource(path);
    var file = new File(Objects.requireNonNull(resource).toURI());

    return FileUtils.readFileToString(file, StandardCharsets.UTF_8.name());
  }

  public static void assertEqualsUsingRecursiveComparison(Object actual, Object expected, String... ignoredFields) {
    assertThat(actual)
      .usingRecursiveComparison()
      .ignoringFields(ignoredFields)
      .isEqualTo(expected);
  }

  public static void verifyNoMoreInteractions(Object testClassInstance) {
    var declaredFields = testClassInstance.getClass().getDeclaredFields();
    var mocks = Arrays.stream(declaredFields)
      .filter(field -> field.getAnnotation(Mock.class) != null || field.getAnnotation(Spy.class) != null)
      .map(field -> getField(testClassInstance, field.getName()))
      .toArray();

    Mockito.verifyNoMoreInteractions(mocks);
  }

  public static void cleanUpCaches(CacheManager cacheManager) {
    cacheManager.getCacheNames().forEach(name -> requireNonNull(cacheManager.getCache(name)).clear());
  }

  public static void disableSslVerification() {
    try {
      var sc = dummySslContext();
      setDefaultSSLSocketFactory(sc.getSocketFactory());
      var allHostsValid = new HostnameVerifier() {
        public boolean verify(String hostname, SSLSession session) {
          return true;
        }
      };

      setDefaultHostnameVerifier(allHostsValid);
    } catch (Exception e) {
      throw new RuntimeException("Failed to disable SSL verification", e);
    }
  }

  public static HttpClient httpClientWithDummySslContext() {
    return HttpClient.newBuilder().sslContext(dummySslContext()).build();
  }

  @SneakyThrows
  public static SSLContext dummySslContext() {
    var dummyTrustManager = new X509ExtendedTrustManager() {

      @Override
      public void checkClientTrusted(X509Certificate[] chain, String authType, Socket socket) {
        // not implemented
      }

      @Override
      public void checkClientTrusted(X509Certificate[] chain, String authType, SSLEngine engine) {
        // not implemented
      }

      @Override
      public void checkClientTrusted(X509Certificate[] chain, String authType) {
        // not implemented
      }

      @Override
      public void checkServerTrusted(X509Certificate[] chain, String authType, Socket socket) {
        // not implemented
      }

      @Override
      public void checkServerTrusted(X509Certificate[] chain, String authType, SSLEngine engine) {
        // not implemented
      }

      @Override
      public void checkServerTrusted(X509Certificate[] chain, String authType) {
        // not implemented
      }

      @Override
      public X509Certificate[] getAcceptedIssuers() {
        return new X509Certificate[0];
      }
    };

    var sslContext = getInstance("TLS");
    sslContext.init(null, new TrustManager[] {dummyTrustManager}, new SecureRandom());
    return sslContext;
  }
}
