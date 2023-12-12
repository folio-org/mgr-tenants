package org.folio.tm.support;

import static java.util.Objects.requireNonNull;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;
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

  public static void cleanUpCaches(CacheManager cacheManager) {
    cacheManager.getCacheNames().forEach(name -> requireNonNull(cacheManager.getCache(name)).clear());
  }
}
