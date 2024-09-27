package org.folio.tm.it;

import static java.lang.String.valueOf;
import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.tm.domain.dto.TenantType.VIRTUAL;
import static org.folio.tm.integration.keycloak.model.ProtocolMapper.USER_ATTRIBUTE_MAPPER_TYPE;
import static org.folio.tm.integration.keycloak.model.ProtocolMapper.USER_PROPERTY_MAPPER_TYPE;
import static org.folio.tm.support.TestConstants.TENANT_ID;
import static org.folio.tm.support.TestConstants.protocolMapper;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;
import static org.springframework.test.context.jdbc.SqlMergeMode.MergeMode.MERGE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;
import org.folio.common.utils.CollectionUtils;
import org.folio.test.extensions.EnableKeycloakDataImport;
import org.folio.test.extensions.EnableKeycloakSecurity;
import org.folio.test.extensions.EnableKeycloakTlsMode;
import org.folio.test.extensions.KeycloakRealms;
import org.folio.test.types.IntegrationTest;
import org.folio.tm.base.BaseIntegrationTest;
import org.folio.tm.domain.dto.Tenant;
import org.folio.tm.domain.dto.TenantType;
import org.folio.tm.repository.TenantRepository;
import org.folio.tm.support.KeycloakTestClientConfiguration;
import org.folio.tm.support.KeycloakTestClientConfiguration.KeycloakTestClient;
import org.folio.tm.support.TestConstants;
import org.folio.tm.support.TestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@IntegrationTest
@SqlMergeMode(MERGE)
@EnableKeycloakTlsMode
@EnableKeycloakSecurity
@EnableKeycloakDataImport
@Import(KeycloakTestClientConfiguration.class)
@TestPropertySource(properties = "application.okapi.enabled=false")
@Sql(scripts = "classpath:/sql/clear_tenants.sql", executionPhase = AFTER_TEST_METHOD)
class TenantKeycloakIT extends BaseIntegrationTest {

  private static final Tenant TENANT1 = new Tenant()
    .id(TENANT_ID)
    .name(TestConstants.TENANT_NAME)
    .description(TestConstants.TENANT_DESCRIPTION)
    .type(TenantType.DEFAULT);

  private static final Tenant TENANT4 = new Tenant()
    .id(UUID.fromString("12a50c0a-b3b7-4992-bd70-442ac1d8e212"))
    .name("tenant4")
    .description("test tenant4")
    .type(TenantType.VIRTUAL);

  @Autowired private TenantRepository repository;
  @Autowired private KeycloakTestClient keycloakTestClient;

  @BeforeAll
  static void beforeAll(@Autowired ApplicationContext applicationContext) {
    assertThat(applicationContext.containsBean("okapiService")).isFalse();
  }

  @Test
  void createTenant_positive() throws Exception {
    var tenant = TENANT4;
    mockMvc.perform(post("/tenants")
        .contentType(APPLICATION_JSON)
        .header(TOKEN, keycloakTestClient.loginAsFolioAdmin())
        .content(TestUtils.asJsonString(tenant)))
      .andExpect(status().isCreated())
      .andExpect(jsonPath("$.id", is(valueOf(tenant.getId()))))
      .andExpect(jsonPath("$.name", is(tenant.getName())))
      .andExpect(jsonPath("$.description", is(tenant.getDescription())))
      .andExpect(jsonPath("$.type", is(tenant.getType().getValue())))
      .andExpect(jsonPath("$.metadata", is(notNullValue())));

    var tenantName = TENANT4.getName();
    var realm = keycloakTestClient.getRealm(tenantName);
    assertThat(realm.getRealm()).isEqualTo(tenantName);
    checkImpersonationClient(tenantName);
  }

  @Test
  void createTenant_positive_nameContainingUnderscore() throws Exception {
    var tenantName = "test_tenant";
    var tenant = new Tenant().name(tenantName).description("Test tenant with underscore in name");
    mockMvc.perform(post("/tenants")
        .contentType(APPLICATION_JSON)
        .header(TOKEN, keycloakTestClient.loginAsFolioAdmin())
        .content(TestUtils.asJsonString(tenant)))
      .andExpect(status().isCreated())
      .andExpect(jsonPath("$.id", is(notNullValue())))
      .andExpect(jsonPath("$.name", is(tenantName)))
      .andExpect(jsonPath("$.description", is(tenant.getDescription())))
      .andExpect(jsonPath("$.type", is(tenant.getType().getValue())))
      .andExpect(jsonPath("$.metadata", is(notNullValue())));

    var realm = keycloakTestClient.getRealm(tenantName);
    assertThat(realm.getRealm()).isEqualTo(tenantName);
    checkImpersonationClient(tenantName);
  }

  @Test
  @Sql("classpath:/sql/populate_tenants.sql")
  void createTenant_negative_tenantExists() throws Exception {
    mockMvc.perform(post("/tenants")
        .contentType(APPLICATION_JSON)
        .header(TOKEN, keycloakTestClient.loginAsFolioAdmin())
        .content(TestUtils.asJsonString(TENANT1)))
      .andExpect(status().isBadRequest())
      .andExpect(
        jsonPath("$.errors[0].message", containsString("Tenant's name already taken: " + TENANT1.getName())))
      .andExpect(jsonPath("$.errors[0].code", is("validation_error")))
      .andExpect(jsonPath("$.errors[0].type", is("RequestValidationException")))
      .andExpect(jsonPath("$.total_records", is(1)));
  }

  @Test
  @Sql("classpath:/sql/populate_tenants.sql")
  @KeycloakRealms(realms = "/json/keycloak/tenant1.json")
  void updateTenant_positive() throws Exception {
    var tenant = copyFrom().description("modified").type(VIRTUAL);
    mockMvc.perform(put("/tenants/{id}", tenant.getId())
        .contentType(APPLICATION_JSON)
        .header(TOKEN, keycloakTestClient.loginAsFolioAdmin())
        .content(TestUtils.asJsonString(tenant)))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.description", is(tenant.getDescription())))
      .andExpect(jsonPath("$.type", is(tenant.getType().getValue())));

    var saved = repository.findById(requireNonNull(tenant.getId())).orElseThrow();
    assertThat(saved.getDescription()).isEqualTo(tenant.getDescription());
    assertThat(saved.getType().name()).isEqualTo(tenant.getType().name());

    var tenantName = tenant.getName();
    var realm = keycloakTestClient.getRealm(tenantName);
    assertThat(realm.getRealm()).isEqualTo(tenantName);
  }

  @Test
  @Sql("classpath:/sql/populate_tenants.sql")
  @KeycloakRealms(realms = "/json/keycloak/tenant1.json")
  void deleteTenant_positive() throws Exception {
    var existing = repository.findById(TENANT_ID);
    assertTrue(existing.isPresent());

    mockMvc.perform(MockMvcRequestBuilders.delete("/tenants/{id}", TENANT_ID)
        .header(TOKEN, keycloakTestClient.loginAsFolioAdmin()))
      .andExpect(status().isNoContent());

    repository.findById(TENANT_ID)
      .ifPresent(tenantEntity -> Assertions.fail("Tenant is not deleted: " + TENANT_ID));
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

  private static ProtocolMapperRepresentation getMapperByName(
    List<ProtocolMapperRepresentation> protocolMappers, String name) {
    return CollectionUtils.toStream(protocolMappers)
      .filter(mapper -> mapper.getName().equals(name))
      .findFirst()
      .orElseThrow(() -> new AssertionError("Failed to find protocol mapper by name: " + name));
  }

  private void checkImpersonationClient(String tenantName) {
    var client = keycloakTestClient.findClientByClientId(tenantName, "impersonation-client");
    assertThat(client).isPresent();

    var mappers = client.get().getProtocolMappers();
    assertThat(getMapperByName(mappers, "username"))
      .usingRecursiveComparison()
      .ignoringFields("id")
      .isEqualTo(protocolMapper(USER_PROPERTY_MAPPER_TYPE, "username", "username", "sub"));

    assertThat(getMapperByName(mappers, "user_id mapper"))
      .usingRecursiveComparison()
      .ignoringFields("id")
      .isEqualTo(protocolMapper(USER_ATTRIBUTE_MAPPER_TYPE, "user_id mapper", "user_id", "user_id"));
  }
}
