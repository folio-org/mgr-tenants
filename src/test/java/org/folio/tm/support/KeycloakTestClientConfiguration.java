package org.folio.tm.support;

import static feign.Request.HttpMethod.POST;
import static jakarta.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static jakarta.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static java.net.URLEncoder.encode;
import static java.net.http.HttpRequest.BodyPublishers.ofString;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.tm.support.TestUtils.OBJECT_MAPPER;
import static org.folio.tm.support.TestUtils.httpClientWithDummySslContext;
import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED_VALUE;

import jakarta.ws.rs.NotFoundException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.folio.security.integration.keycloak.configuration.properties.KeycloakProperties;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;

@Log4j2
@TestConfiguration
@RequiredArgsConstructor
public class KeycloakTestClientConfiguration {

  public static final HttpClient HTTP_CLIENT_DUMMY_SSL = httpClientWithDummySslContext();

  @Bean
  public KeycloakTestClient keycloakTestClient(Keycloak keycloak, KeycloakProperties keycloakConfiguration) {
    return new KeycloakTestClient(keycloak, keycloakConfiguration);
  }

  @RequiredArgsConstructor
  public static final class KeycloakTestClient {

    public static final ClientCredentials TEST_LOGIN_CLIENT_CREDENTIALS =
      ClientCredentials.of("test-login-application", "test-login-application-secret");
    public static final ClientCredentials TEST_IMPERSONATION_CLIENT_CREDENTIALS =
      ClientCredentials.of("test-login-application", "test-login-application-secret");

    private final Keycloak keycloak;
    private final KeycloakProperties keycloakConfiguration;

    public Optional<ClientRepresentation> findClientByClientId(String realm, String clientId) {
      return keycloak.realm(realm).clients().findByClientId(clientId).stream().findFirst();
    }

    public RealmRepresentation getRealm(String realm) {
      try {
        return keycloak.realm(realm).toRepresentation();
      } catch (NotFoundException exception) {
        throw new AssertionError("Failed to find realm: " + realm, exception);
      }
    }

    public String loginAsFolioAdmin() {
      return login("master", ClientCredentials.of("folio-backend-admin-client", "supersecret").asTokenRequestBody());
    }

    @SneakyThrows
    public String login(String tenant, Map<String, String> tokenRequestBody) {
      var keycloakBaseUrl = StringUtils.removeEnd(keycloakConfiguration.getUrl(), "/");
      var uri = URI.create(String.format("%s/realms/%s/protocol/openid-connect/token", keycloakBaseUrl, tenant));
      var request = HttpRequest.newBuilder(uri)
        .method(POST.name(), ofString(toFormUrlencodedValue(tokenRequestBody), UTF_8))
        .header("Content-Type", APPLICATION_FORM_URLENCODED_VALUE)
        .build();

      var response = HTTP_CLIENT_DUMMY_SSL.send(request, BodyHandlers.ofString(UTF_8));
      assertThat(response.statusCode()).isLessThan(400);
      var keycloakTokenJson = OBJECT_MAPPER.readTree(response.body());
      return keycloakTokenJson.path("access_token").asText();
    }

    @SneakyThrows
    public HttpStatus verifyToken(String tenant, String token, String audience) {
      var keycloakBaseUrl = StringUtils.removeEnd(keycloakConfiguration.getUrl(), "/");
      var uri = URI.create(String.format("%s/realms/%s/protocol/openid-connect/token", keycloakBaseUrl, tenant));
      var requestBody = Map.of(
        "grant_type", "urn:ietf:params:oauth:grant-type:uma-ticket",
        "audience", tenant + "-login-application",
        "permission", audience);

      var request = HttpRequest.newBuilder(uri)
        .method(POST.name(), ofString(toFormUrlencodedValue(requestBody), UTF_8))
        .header(CONTENT_TYPE, APPLICATION_FORM_URLENCODED_VALUE)
        .header(AUTHORIZATION, "Bearer " + token)
        .build();

      var response = HTTP_CLIENT_DUMMY_SSL.send(request, BodyHandlers.ofString(UTF_8));
      return HttpStatus.resolve(response.statusCode());
    }

    @SneakyThrows
    public String impersonateUser(String tenant, String username, ClientCredentials impersonationClientCredentials) {
      var keycloakBaseUrl = StringUtils.removeEnd(keycloakConfiguration.getUrl(), "/");
      var uri = URI.create(String.format("%s/realms/%s/protocol/openid-connect/token", keycloakBaseUrl, tenant));
      var requestBody = Map.of(
        "grant_type", "urn:ietf:params:oauth:grant-type:token-exchange",
        "client_id", impersonationClientCredentials.getClientId(),
        "client_secret", impersonationClientCredentials.getClientSecret(),
        "requested_subject", username);

      var request = HttpRequest.newBuilder(uri)
        .method(POST.name(), ofString(toFormUrlencodedValue(requestBody), UTF_8))
        .header(CONTENT_TYPE, APPLICATION_FORM_URLENCODED_VALUE)
        .build();

      var response = HTTP_CLIENT_DUMMY_SSL.send(request, BodyHandlers.ofString(UTF_8));
      assertThat(response.statusCode()).isLessThan(400);
      var keycloakTokenJson = OBJECT_MAPPER.readTree(response.body());
      return keycloakTokenJson.path("access_token").asText();
    }

    private static String toFormUrlencodedValue(Map<String, String> params) {
      return params.entrySet()
        .stream()
        .map(entry -> String.format("%s=%s", encode(entry.getKey(), UTF_8), encode(entry.getValue(), UTF_8)))
        .collect(Collectors.joining("&"));
    }
  }

  @Data
  @RequiredArgsConstructor(staticName = "of")
  public static final class UserCredentials {

    private final String username;
    private final String password;

    public Map<String, String> asTokenRequestBody(ClientCredentials clientCredentials) {
      return Map.of(
        "client_id", clientCredentials.getClientId(),
        "client_secret", clientCredentials.getClientSecret(),
        "username", username,
        "password", password,
        "grant_type", "password"
      );
    }
  }

  @Data
  @RequiredArgsConstructor(staticName = "of")
  public static final class ClientCredentials {

    private final String clientId;
    private final String clientSecret;

    public Map<String, String> asTokenRequestBody() {
      return Map.of(
        "client_id", clientId,
        "client_secret", clientSecret,
        "grant_type", "client_credentials");
    }
  }
}
