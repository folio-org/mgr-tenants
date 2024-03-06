package org.folio.tm.it;

import static java.lang.String.valueOf;
import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.tm.domain.dto.TenantType.VIRTUAL;
import static org.folio.tm.integration.keycloak.model.ProtocolMapper.USER_ATTRIBUTE_MAPPER_TYPE;
import static org.folio.tm.integration.keycloak.model.ProtocolMapper.USER_PROPERTY_MAPPER_TYPE;
import static org.folio.tm.support.TestConstants.protocolMapper;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_METHOD;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.folio.test.extensions.EnableKeycloak;
import org.folio.test.extensions.EnableKeycloakDataImport;
import org.folio.test.extensions.EnableKeycloakSecurity;
import org.folio.test.extensions.KeycloakRealms;
import org.folio.test.types.IntegrationTest;
import org.folio.tm.base.BaseIntegrationTest;
import org.folio.tm.domain.dto.Tenant;
import org.folio.tm.domain.dto.TenantType;
import org.folio.tm.integration.keycloak.KeycloakClient;
import org.folio.tm.integration.keycloak.KeycloakClientService;
import org.folio.tm.integration.keycloak.KeycloakRealmService;
import org.folio.tm.integration.okapi.OkapiService;
import org.folio.tm.repository.TenantRepository;
import org.folio.tm.support.TestConstants;
import org.folio.tm.support.TestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@IntegrationTest
@EnableKeycloak(tlsEnabled = true)
@EnableKeycloakSecurity
@EnableKeycloakDataImport
@KeycloakRealms(realms = "/json/keycloak/tenant1.json")
@TestPropertySource(properties = "application.okapi.enabled=false")
@Sql(scripts = "classpath:/sql/populate_tenants.sql", executionPhase = BEFORE_TEST_METHOD)
@Sql(scripts = "classpath:/sql/clear_tenants.sql", executionPhase = AFTER_TEST_METHOD)
class TenantKeycloakIT extends BaseIntegrationTest {

  private static final Tenant TENANT1 = new Tenant()
    .id(TestConstants.TENANT_ID)
    .name(TestConstants.TENANT_NAME)
    .description(TestConstants.TENANT_DESCRIPTION)
    .type(TenantType.DEFAULT);
  private static final Tenant TENANT4 = new Tenant()
    .id(UUID.fromString("12a50c0a-b3b7-4992-bd70-442ac1d8e212"))
    .name("tenant4")
    .description("test tenant4")
    .type(TenantType.VIRTUAL);

  @Autowired
  private TenantRepository repository;

  @Autowired
  private KeycloakRealmService keycloakRealmService;

  @Autowired
  private KeycloakClientService keycloakClientService;

  @Autowired(required = false)
  private OkapiService okapiService;

  @Autowired
  private KeycloakClient keycloakClient;

  @Value("${application.keycloak.admin.client_id}")
  private String clientId;

  @Value("${application.keycloak.admin.client_secret}")
  private String clientSecret;

  @Value("${application.keycloak.admin.grant_type}")
  private String grantType;

  @Test
  void create_tenant_positive() throws Exception {
    var tenant = TENANT4;
    var token = keycloakClient.login(requestBody());
    mockMvc.perform(post("/tenants")
        .contentType(APPLICATION_JSON)
        .header(TOKEN, token.getAccessToken())
        .content(TestUtils.asJsonString(tenant)))
      .andExpect(status().isCreated())
      .andExpect(jsonPath("$.id", is(valueOf(tenant.getId()))))
      .andExpect(jsonPath("$.name", is(tenant.getName())))
      .andExpect(jsonPath("$.description", is(tenant.getDescription())))
      .andExpect(jsonPath("$.type", is(tenant.getType().getValue())))
      .andExpect(jsonPath("$.metadata", is(notNullValue())));

    assertTrue(keycloakRealmService.isRealmExist(tenant.getName()));
    assertNull(okapiService);

    var client = keycloakClientService.findClientByClientId(TENANT4.getName(), "impersonation-client");
    var mappers = client.getProtocolMappers();
    var expectedMappers = List.of(
      protocolMapper(USER_PROPERTY_MAPPER_TYPE, "username", "username", "sub"),
      protocolMapper(USER_ATTRIBUTE_MAPPER_TYPE, "user_id mapper", "user_id", "user_id")
    );

    assertThat(mappers).containsAll(expectedMappers);
  }

  @Test
  void createTenant_positive_nameContainingUnderscore() throws Exception {
    var tenantName = "test_tenant";
    var tenant = new Tenant().name(tenantName).description("Test tenant with underscore in name");
    var token = keycloakClient.login(requestBody());
    mockMvc.perform(post("/tenants")
        .contentType(APPLICATION_JSON)
        .header(TOKEN, token.getAccessToken())
        .content(TestUtils.asJsonString(tenant)))
      .andExpect(status().isCreated())
      .andExpect(jsonPath("$.id", is(notNullValue())))
      .andExpect(jsonPath("$.name", is(tenantName)))
      .andExpect(jsonPath("$.description", is(tenant.getDescription())))
      .andExpect(jsonPath("$.type", is(tenant.getType().getValue())))
      .andExpect(jsonPath("$.metadata", is(notNullValue())));

    assertTrue(keycloakRealmService.isRealmExist(tenantName));
    assertNull(okapiService);

    var client = keycloakClientService.findClientByClientId(tenantName, "impersonation-client");
    var mappers = client.getProtocolMappers();

    var expectedMappers = List.of(
      protocolMapper(USER_PROPERTY_MAPPER_TYPE, "username", "username", "sub"),
      protocolMapper(USER_ATTRIBUTE_MAPPER_TYPE, "user_id mapper", "user_id", "user_id")
    );

    assertThat(mappers).containsAll(expectedMappers);
  }

  @Test
  void create_tenant_negative_tenant_exists() throws Exception {
    var token = keycloakClient.login(requestBody());
    mockMvc.perform(post("/tenants")
        .contentType(APPLICATION_JSON)
        .header(TOKEN, token.getAccessToken())
        .content(TestUtils.asJsonString(TENANT1)))
      .andExpect(status().isBadRequest())
      .andExpect(
        jsonPath("$.errors[0].message", containsString("Tenant's name already taken: " + TENANT1.getName())))
      .andExpect(jsonPath("$.errors[0].code", is("validation_error")))
      .andExpect(jsonPath("$.errors[0].type", is("RequestValidationException")))
      .andExpect(jsonPath("$.total_records", is(1)));
  }

  @Test
  void update_tenant_positive() throws Exception {
    var tenant = copyFrom().description("modified").type(VIRTUAL);
    var token = keycloakClient.login(requestBody());
    mockMvc.perform(put("/tenants/{id}", tenant.getId())
        .contentType(APPLICATION_JSON)
        .header(TOKEN, token.getAccessToken())
        .content(TestUtils.asJsonString(tenant)))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.description", is(tenant.getDescription())))
      .andExpect(jsonPath("$.type", is(tenant.getType().getValue())));

    var saved = repository.findById(requireNonNull(tenant.getId())).orElseThrow();
    assertThat(saved.getDescription()).isEqualTo(tenant.getDescription());
    assertThat(saved.getType().name()).isEqualTo(tenant.getType().name());

    assertTrue(keycloakRealmService.isRealmExist(tenant.getName()));
    assertNull(okapiService);
  }

  @Test
  void delete_tenant_positive() throws Exception {
    var existing = repository.findById(TestConstants.TENANT_ID);
    var token = keycloakClient.login(requestBody());

    assertTrue(existing.isPresent());

    mockMvc.perform(MockMvcRequestBuilders.delete("/tenants/{id}", TestConstants.TENANT_ID)
        .header(TOKEN, token.getAccessToken()))
      .andExpect(status().isNoContent());

    repository.findById(TestConstants.TENANT_ID)
      .ifPresent(tenantEntity -> Assertions.fail("Tenant is not deleted: " + TestConstants.TENANT_ID));

    assertNull(okapiService);
  }

  private static Tenant copyFrom() {
    var result = new Tenant();

    result.setId(TenantKeycloakIT.TENANT1.getId());
    result.setName(TenantKeycloakIT.TENANT1.getName());
    result.setDescription(TenantKeycloakIT.TENANT1.getDescription());
    result.setType(TenantKeycloakIT.TENANT1.getType());
    result.setMetadata(TenantKeycloakIT.TENANT1.getMetadata());
    result.setAttributes(TenantKeycloakIT.TENANT1.getAttributes());

    return result;
  }

  private Map<String, String> requestBody() {
    var loginRequest = new HashMap<String, String>();
    loginRequest.put("client_id", clientId);
    loginRequest.put("client_secret", clientSecret);
    loginRequest.put("grant_type", grantType);
    return loginRequest;
  }
}
