package org.folio.tm.it;

import static jakarta.ws.rs.core.HttpHeaders.LOCATION;
import static jakarta.ws.rs.core.Response.Status.Family.SUCCESSFUL;
import static org.apache.commons.lang3.StringUtils.substringAfterLast;
import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.tm.domain.dto.TenantType.VIRTUAL;
import static org.folio.tm.support.TestUtils.OBJECT_MAPPER;
import static org.folio.tm.support.TestUtils.asJsonString;
import static org.hamcrest.Matchers.is;
import static org.keycloak.representations.idm.authorization.DecisionStrategy.UNANIMOUS;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;
import static org.springframework.test.context.jdbc.SqlMergeMode.MergeMode.MERGE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.ws.rs.core.Response;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import lombok.SneakyThrows;
import org.apache.commons.collections4.CollectionUtils;
import org.folio.test.extensions.EnableKeycloakDataImport;
import org.folio.test.extensions.EnableKeycloakSecurity;
import org.folio.test.extensions.EnableKeycloakTlsMode;
import org.folio.test.types.IntegrationTest;
import org.folio.tm.base.BaseIntegrationTest;
import org.folio.tm.domain.dto.Tenant;
import org.folio.tm.support.KeycloakTestClientConfiguration;
import org.folio.tm.support.KeycloakTestClientConfiguration.ClientCredentials;
import org.folio.tm.support.KeycloakTestClientConfiguration.KeycloakTestClient;
import org.folio.tm.support.KeycloakTestClientConfiguration.UserCredentials;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.authorization.Logic;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.keycloak.representations.idm.authorization.ScopePermissionRepresentation;
import org.keycloak.representations.idm.authorization.UserPolicyRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;

@IntegrationTest
@SqlMergeMode(MERGE)
@EnableKeycloakTlsMode
@EnableKeycloakSecurity
@EnableKeycloakDataImport
@Import(KeycloakTestClientConfiguration.class)
@TestPropertySource(properties = "application.okapi.enabled=false")
@Sql(scripts = "classpath:/sql/clear_tenants.sql", executionPhase = AFTER_TEST_METHOD)
public class KeycloakInteractionsIT extends BaseIntegrationTest {

  private static final String TENANT = "test";
  private static final String TEST_USER_POLICY_ID = UUID.randomUUID().toString();
  private static final UUID TENANT_ID = UUID.fromString("12a50c0a-b3b7-4992-bd70-442ac1d8e212");
  private static final String FOLIO_USER_ID = UUID.randomUUID().toString();
  @Autowired private Keycloak keycloak;
  @Autowired private KeycloakTestClient kcTestClient;

  @BeforeEach
  void setUp() throws Exception {
    mockMvc.perform(post("/tenants")
        .contentType(APPLICATION_JSON)
        .header(TOKEN, kcTestClient.loginAsFolioAdmin())
        .content(asJsonString(tenant())))
      .andExpect(status().isCreated())
      .andExpect(jsonPath("$.name", is(TENANT)));

    prepareTestKeycloakRealm();
  }

  @AfterEach
  void tearDown() throws Exception {
    mockMvc.perform(delete("/tenants/{id}", TENANT_ID)
        .contentType(APPLICATION_JSON)
        .header(TOKEN, kcTestClient.loginAsFolioAdmin()))
      .andExpect(status().isNoContent());
  }

  @Test
  void moduleAccessClientCheck() {
    var clientCredentials = ClientCredentials.of("sidecar-module-access-client", "supersecret");
    var accessToken = kcTestClient.login(TENANT, clientCredentials.asTokenRequestBody());

    assertThat(kcTestClient.verifyToken(TENANT, accessToken, "/bar/entities#GET")).isEqualTo(OK);
    assertThat(kcTestClient.verifyToken(TENANT, accessToken, "/foo/entities#GET")).isEqualTo(OK);
    assertThat(kcTestClient.verifyToken(TENANT, accessToken, "/foo/entities/{id}#GET")).isEqualTo(OK);
    assertThat(kcTestClient.verifyToken(TENANT, accessToken, "/foo/entities/{id}#PUT")).isEqualTo(OK);
    assertThat(kcTestClient.verifyToken(TENANT, accessToken, "/foo/entities/{id}#DELETE")).isEqualTo(OK);
  }

  @Test
  void userAuthorizationCheck() {
    var clientCredentials = ClientCredentials.of(TENANT + "-login-application", "supersecret");
    var loginCredentials = UserCredentials.of("test_user", "test_user-password");
    var accessToken = kcTestClient.login(TENANT, loginCredentials.asTokenRequestBody(clientCredentials));

    var parsedTokenTree = parseToken(accessToken);
    assertThat(parsedTokenTree.path("sub").asText()).isEqualTo("test_user");
    assertThat(parsedTokenTree.path("user_id").asText()).isEqualTo(FOLIO_USER_ID);

    assertThat(kcTestClient.verifyToken(TENANT, accessToken, "/bar/entities#GET")).isEqualTo(FORBIDDEN);
    assertThat(kcTestClient.verifyToken(TENANT, accessToken, "/foo/entities#GET")).isEqualTo(OK);
    assertThat(kcTestClient.verifyToken(TENANT, accessToken, "/foo/entities/{id}#GET")).isEqualTo(FORBIDDEN);
    assertThat(kcTestClient.verifyToken(TENANT, accessToken, "/foo/entities/{id}#PUT")).isEqualTo(FORBIDDEN);
    assertThat(kcTestClient.verifyToken(TENANT, accessToken, "/foo/entities/{id}#DELETE")).isEqualTo(FORBIDDEN);
  }

  @Test
  void userCanBeImpersonated() {
    var impersonationClientCredentials = ClientCredentials.of("impersonation-client", "supersecret");
    var accessToken = kcTestClient.impersonateUser(TENANT, "test_user", impersonationClientCredentials);

    var parsedTokenTree = parseToken(accessToken);
    assertThat(parsedTokenTree.path("sub").asText()).isEqualTo("test_user");
    assertThat(parsedTokenTree.path("user_id").asText()).isEqualTo(FOLIO_USER_ID);

    assertThat(kcTestClient.verifyToken(TENANT, accessToken, "/bar/entities#GET")).isEqualTo(FORBIDDEN);
    assertThat(kcTestClient.verifyToken(TENANT, accessToken, "/foo/entities#GET")).isEqualTo(OK);
    assertThat(kcTestClient.verifyToken(TENANT, accessToken, "/foo/entities/{id}#GET")).isEqualTo(FORBIDDEN);
    assertThat(kcTestClient.verifyToken(TENANT, accessToken, "/foo/entities/{id}#PUT")).isEqualTo(FORBIDDEN);
    assertThat(kcTestClient.verifyToken(TENANT, accessToken, "/foo/entities/{id}#DELETE")).isEqualTo(FORBIDDEN);
  }

  @Test
  void passwordResetTokenCanBeRetrieved() {
    var loginRequest = Map.of(
      "client_id", "password-reset-client",
      "client_secret", "supersecret",
      "grant_type", "client_credentials",
      "passwordResetActionId", UUID.randomUUID().toString());

    var passwordResetToken = kcTestClient.login(TENANT, loginRequest);
  }

  private static Tenant tenant() {
    return new Tenant()
      .id(TENANT_ID)
      .name(TENANT)
      .description("Description for tenant: " + TENANT)
      .type(VIRTUAL);
  }

  private void prepareTestKeycloakRealm() {
    var loginClientId = getLoginClientId();
    var authorizationResource = keycloak.realm(TENANT).clients().get(loginClientId).authorization();

    var resources = authorizationResource.resources();
    executeRequest(() -> resources.create(keycloakResource("/bar/entities", "GET")));
    executeRequest(() -> resources.create(keycloakResource("/foo/entities", "GET", "POST")));
    executeRequest(() -> resources.create(keycloakResource("/foo/entities/{id}", "GET", "PUT", "DELETE")));

    var userId = createTestUser();
    var policiesResource = authorizationResource.policies();
    executeRequest(() -> policiesResource.user().create(userPolicy(userId)));
    var permissionsResource = authorizationResource.permissions();
    executeRequest(() -> permissionsResource.scope().create(userPermission(userId)));
  }

  private String createTestUser() {
    var realmResource = keycloak.realm(TENANT);
    var userId = executeRequest(() -> realmResource.users().create(keycloakUser()));
    realmResource.users().get(userId).resetPassword(userCredentials());
    return userId;
  }

  private String getLoginClientId() {
    var keycloakClients = keycloak.realm(TENANT).clients().findByClientId(TENANT + "-login-application");
    assertThat(keycloakClients).hasSize(1);
    return keycloakClients.get(0).getId();
  }

  private static UserPolicyRepresentation userPolicy(String userId) {
    var userPolicy = new UserPolicyRepresentation();
    userPolicy.setId(TEST_USER_POLICY_ID);
    userPolicy.setUsers(Set.of(userId));
    userPolicy.setLogic(Logic.POSITIVE);
    userPolicy.setName("Policy for user: " + userId);
    return userPolicy;
  }

  private static ScopePermissionRepresentation userPermission(String userId) {
    var scopePermission = new ScopePermissionRepresentation();
    scopePermission.setName("GET access for user '" + userId + "' to '/foo/entities'");
    scopePermission.setDecisionStrategy(UNANIMOUS);
    scopePermission.setResources(Set.of("/foo/entities"));
    scopePermission.setScopes(Set.of("GET"));
    scopePermission.setPolicies(Set.of(TEST_USER_POLICY_ID));
    return scopePermission;
  }

  private static ResourceRepresentation keycloakResource(String name, String... scopes) {
    return new ResourceRepresentation(name, scopes);
  }

  private static CredentialRepresentation userCredentials() {
    var credentialRepresentation = new CredentialRepresentation();
    credentialRepresentation.setType("password");
    credentialRepresentation.setValue("test_user-password");
    credentialRepresentation.setTemporary(false);
    return credentialRepresentation;
  }

  private static UserRepresentation keycloakUser() {
    var keycloakUser = new UserRepresentation();
    keycloakUser.setUsername("test_user");
    keycloakUser.setEnabled(true);
    keycloakUser.setEmail("test_user@sample.dev");
    keycloakUser.setAttributes(Map.of("user_id", List.of(FOLIO_USER_ID)));

    return keycloakUser;
  }

  private static String executeRequest(Supplier<Response> keycloakRequest) {
    try (var response = keycloakRequest.get()) {
      assertThat(response.getStatusInfo().getFamily()).isEqualTo(SUCCESSFUL);
      return Optional.ofNullable(response.getHeaders())
        .map(headers -> headers.get(LOCATION))
        .filter(CollectionUtils::isNotEmpty)
        .map(locationHeaderValue -> locationHeaderValue.get(0))
        .filter(String.class::isInstance)
        .map(String.class::cast)
        .map(stringHeader -> substringAfterLast(stringHeader, "/"))
        .orElse(null);
    }
  }

  @SneakyThrows
  private static JsonNode parseToken(String accessToken) {
    var parts = accessToken.split("\\.");
    assertThat(parts).hasSize(3);
    return OBJECT_MAPPER.readTree(Base64.getDecoder().decode(parts[1]));
  }
}
