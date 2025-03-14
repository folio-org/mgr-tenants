package org.folio.tm.utils;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.folio.tm.support.TestUtils.OBJECT_MAPPER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import org.apache.commons.lang3.SerializationException;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class JsonHelperTest {

  private static final User TEST_USER = new User("test", "user");
  private static final String TEST_USER_JSON = "{\"name\": \"test\", \"surname\": \"user\"}";

  @InjectMocks private JsonHelper helper;
  @Mock private ObjectMapper mapper;

  @Nested
  @DisplayName("parse")
  class Parse {

    @Test
    void positive_stringInput() throws JsonProcessingException {
      when(mapper.readValue(TEST_USER_JSON, User.class)).thenReturn(TEST_USER);
      var actual = helper.parse(TEST_USER_JSON, User.class);
      assertThat(actual).isEqualTo(TEST_USER);
    }

    @Test
    void positive_stringInputTree() throws JsonProcessingException {
      var tree = new ObjectNode(OBJECT_MAPPER.getNodeFactory(), Map.of(
        "name", new TextNode("test"),
        "surname", new TextNode("user")));

      when(mapper.readTree(TEST_USER_JSON)).thenReturn(tree);
      var actual = helper.parse(TEST_USER_JSON);
      assertThat(actual).isEqualTo(tree);
    }

    @Test
    void positive_stringInputStreamTree() throws IOException {
      var tree = new ObjectNode(OBJECT_MAPPER.getNodeFactory(), Map.of(
        "name", new TextNode("test"),
        "surname", new TextNode("user")));
      var inputStream = new ByteArrayInputStream(TEST_USER_JSON.getBytes(UTF_8));
      when(mapper.readTree(inputStream)).thenReturn(tree);
      var actual = helper.parse(inputStream);
      assertThat(actual).isEqualTo(tree);
    }

    @Test
    void positive_nullStringInput() {
      var actual = helper.parse((String) null, User.class);
      assertThat(actual).isNull();
    }

    @Test
    void positive_nullStringInputSingleArgument() {
      var actual = helper.parse((String) null);
      assertThat(actual).isNull();
    }

    @Test
    void positive_nullStringForTypeReference() {
      var typeReference = new TypeReference<User>() {};
      var actual = helper.parse((String) null, typeReference);
      assertThat(actual).isNull();
    }

    @Test
    void negative_stringInputMapperException() throws JsonProcessingException {
      when(mapper.readValue(TEST_USER_JSON, User.class)).thenThrow(new TestJsonProcessingException("Failed"));
      assertThatThrownBy(() -> helper.parse(TEST_USER_JSON, User.class))
        .isInstanceOf(SerializationException.class)
        .hasMessage("Failed to deserialize: value = %s, message = Failed", TEST_USER_JSON);
    }

    @Test
    void negative_stringInputSingleArgumentMapperException() throws JsonProcessingException {
      when(mapper.readTree(TEST_USER_JSON)).thenThrow(new TestJsonProcessingException("Failed"));
      assertThatThrownBy(() -> helper.parse(TEST_USER_JSON))
        .isInstanceOf(SerializationException.class)
        .hasMessage("Failed to deserialize: value = %s, message = Failed", TEST_USER_JSON);
    }

    @Test
    void positive_stringInputTypeReference() throws JsonProcessingException {
      var typeReference = new TypeReference<User>() {};
      when(mapper.readValue(TEST_USER_JSON, typeReference)).thenReturn(TEST_USER);
      var actual = helper.parse(TEST_USER_JSON, typeReference);
      assertThat(actual).isEqualTo(TEST_USER);
    }

    @Test
    void negative_stringInputTypeRefMapperException() throws JsonProcessingException {
      var typeReference = new TypeReference<User>() {};
      when(mapper.readValue(TEST_USER_JSON, typeReference)).thenThrow(new TestJsonProcessingException("Failed"));
      assertThatThrownBy(() -> helper.parse(TEST_USER_JSON, typeReference))
        .isInstanceOf(SerializationException.class)
        .hasMessage("Failed to deserialize: value = %s, message = Failed", TEST_USER_JSON);
    }

    @Test
    void positive_inputStream() throws IOException {
      var inputStream = new ByteArrayInputStream(TEST_USER_JSON.getBytes(UTF_8));
      when(mapper.readValue(inputStream, User.class)).thenReturn(TEST_USER);
      var actual = helper.parse(inputStream, User.class);
      assertThat(actual).isEqualTo(TEST_USER);
    }

    @Test
    void positive_inputStreamWithTypeReference() throws IOException {
      var typeReference = new TypeReference<User>() {};
      var inputStream = new ByteArrayInputStream(TEST_USER_JSON.getBytes(UTF_8));
      when(mapper.readValue(inputStream, typeReference)).thenReturn(TEST_USER);
      var actual = helper.parse(inputStream, typeReference);
      assertThat(actual).isEqualTo(TEST_USER);
    }

    @Test
    void positive_nullInputStream() {
      var actual = helper.parse((InputStream) null);
      assertThat(actual).isNull();
    }

    @Test
    void positive_nullInputStreamClass() {
      var actual = helper.parse((InputStream) null, User.class);
      assertThat(actual).isNull();
    }

    @Test
    void positive_nullInputStreamTypeReference() {
      var typeReference = new TypeReference<User>() {};
      var actual = helper.parse((InputStream) null, typeReference);
      assertThat(actual).isNull();
    }

    @Test
    void positive_inputStreamMapperException() throws IOException {
      var inputStream = new ByteArrayInputStream(TEST_USER_JSON.getBytes(UTF_8));
      when(mapper.readValue(inputStream, User.class)).thenThrow(new TestJsonProcessingException("Failed"));
      assertThatThrownBy(() -> helper.parse(inputStream, User.class))
        .isInstanceOf(SerializationException.class)
        .hasMessage("Failed to deserialize: value = %s, message = Failed", inputStream);
    }

    @Test
    void positive_inputStreamTypeRefMapperException() throws IOException {
      var inputStream = new ByteArrayInputStream(TEST_USER_JSON.getBytes(UTF_8));
      var typeReference = new TypeReference<User>() {};
      when(mapper.readValue(inputStream, typeReference)).thenThrow(new TestJsonProcessingException("Failed"));
      assertThatThrownBy(() -> helper.parse(inputStream, typeReference))
        .isInstanceOf(SerializationException.class)
        .hasMessage("Failed to deserialize: value = %s, message = Failed", inputStream);
    }

    @Test
    void positive_inputStreamSingleArgumentMapperException() throws IOException {
      var inputStream = new ByteArrayInputStream(TEST_USER_JSON.getBytes(UTF_8));
      when(mapper.readTree(inputStream)).thenThrow(new TestJsonProcessingException("Failed"));
      assertThatThrownBy(() -> helper.parse(inputStream))
        .isInstanceOf(SerializationException.class)
        .hasMessage("Failed to deserialize: value = %s, message = Failed", inputStream);
    }
  }

  @Nested
  @DisplayName("asJsonStringSafe")
  class AsJsonStringSafe {

    @Test
    void positive() throws JsonProcessingException {
      when(mapper.writeValueAsString(TEST_USER)).thenReturn(TEST_USER_JSON);
      var actual = helper.asJsonStringSafe(TEST_USER);
      assertEquals(TEST_USER_JSON, actual);
    }

    @Test
    void positive_mapperException() throws JsonProcessingException {
      when(mapper.writeValueAsString(TEST_USER)).thenThrow(new TestJsonProcessingException("Failed"));
      var actual = helper.asJsonStringSafe(TEST_USER);
      assertThat(actual).isEmpty();
    }
  }

  @Nested
  @DisplayName("asJsonString")
  class AsJsonString {

    @Test
    void positive() throws JsonProcessingException {
      when(mapper.writeValueAsString(TEST_USER)).thenReturn(TEST_USER_JSON);

      var actual = helper.asJsonString(TEST_USER);

      assertThat(actual).isEqualTo(TEST_USER_JSON);
      verify(mapper).writeValueAsString(TEST_USER);
    }

    @Test
    void toJson_positive_nullValue() {
      var actual = helper.asJsonString(null);
      assertThat(actual).isNull();
    }

    @Test
    void asJsonString_negative_mapperException() throws JsonProcessingException {
      when(mapper.writeValueAsString(TEST_USER)).thenThrow(new TestJsonProcessingException("Failed"));
      assertThatThrownBy(() -> helper.asJsonString(TEST_USER))
        .isInstanceOf(SerializationException.class)
        .hasMessage("Failed to serialize value: message = Failed");
    }
  }

  private record User(String name, String surname) {}

  private static final class TestJsonProcessingException extends JsonProcessingException {

    TestJsonProcessingException(String msg) {
      super(msg);
    }
  }
}

