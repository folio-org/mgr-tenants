package org.folio.tm.it;

import static java.lang.String.valueOf;
import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.tm.domain.dto.TenantType.VIRTUAL;
import static org.folio.tm.support.TestConstants.AUTH_TOKEN;
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

import java.util.UUID;
import org.folio.test.extensions.EnableOkapiSecurity;
import org.folio.test.extensions.WireMockStub;
import org.folio.test.types.IntegrationTest;
import org.folio.tm.base.BaseIntegrationTest;
import org.folio.tm.domain.dto.Tenant;
import org.folio.tm.domain.dto.TenantType;
import org.folio.tm.integration.okapi.OkapiService;
import org.folio.tm.repository.TenantRepository;
import org.folio.tm.service.listeners.TenantServiceListener;
import org.folio.tm.support.TestConstants;
import org.folio.tm.support.TestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@IntegrationTest
@EnableOkapiSecurity
@Sql(scripts = "classpath:/sql/populate_tenants.sql", executionPhase = BEFORE_TEST_METHOD)
@Sql(scripts = "classpath:/sql/clear_tenants.sql", executionPhase = AFTER_TEST_METHOD)
class TenantOkapiIT extends BaseIntegrationTest {

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

  @Autowired(required = false)
  @Qualifier("keycloakTenantListener")
  private TenantServiceListener keycloakTenantListener;

  @Autowired
  private OkapiService okapiService;

  @Test
  @WireMockStub(scripts = {
    "/wiremock/stubs/mod-authtoken/verify-token-create-tenant.json",
    "/wiremock/stubs/okapi/create-tenant.json",
    "/wiremock/stubs/okapi/get-tenant-no-token.json",
    "/wiremock/stubs/okapi/get-tenant-not-found.json"
  })
  void create_tenant_positive() throws Exception {
    var tenant = TENANT4;

    mockMvc.perform(post("/tenants")
        .contentType(APPLICATION_JSON)
        .header(TOKEN, AUTH_TOKEN)
        .content(TestUtils.asJsonString(tenant)))
      .andExpect(status().isCreated())
      .andExpect(jsonPath("$.id", is(valueOf(tenant.getId()))))
      .andExpect(jsonPath("$.name", is(tenant.getName())))
      .andExpect(jsonPath("$.description", is(tenant.getDescription())))
      .andExpect(jsonPath("$.type", is(tenant.getType().getValue())))
      .andExpect(jsonPath("$.metadata", is(notNullValue())));

    assertThat(okapiService.getTenant(tenant.getName())).isNotNull();
    assertNull(keycloakTenantListener);
  }

  @Test
  @WireMockStub(scripts = {
    "/wiremock/stubs/mod-authtoken/verify-token-update-tenant.json",
    "/wiremock/stubs/okapi/update-tenant.json",
    "/wiremock/stubs/okapi/get-tenant-no-token.json",
  })
  void update_tenant_positive() throws Exception {
    var tenant = copyFrom().description("modified").type(VIRTUAL);

    mockMvc.perform(put("/tenants/{id}", tenant.getId())
        .contentType(APPLICATION_JSON)
        .header(TOKEN, AUTH_TOKEN)
        .content(TestUtils.asJsonString(tenant)))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.description", is(tenant.getDescription())))
      .andExpect(jsonPath("$.type", is(tenant.getType().getValue())));

    var saved = repository.findById(requireNonNull(tenant.getId())).orElseThrow();
    assertThat(saved.getDescription()).isEqualTo(tenant.getDescription());
    assertThat(saved.getType().name()).isEqualTo(tenant.getType().name());

    assertThat(okapiService.getTenant(TENANT4.getName())).isNotNull();
    assertNull(keycloakTenantListener);
  }

  @Test
  @WireMockStub(scripts = {
    "/wiremock/stubs/mod-authtoken/verify-token-delete-tenant.json",
    "/wiremock/stubs/okapi/delete-tenant.json",
    "/wiremock/stubs/okapi/get-tenant-exist.json",
    "/wiremock/stubs/okapi/get-tenant-no-token.json",
    "/wiremock/stubs/mgr-tenant-entitlements/get-entitlements-no-apps.json"
  })
  void delete_tenant_positive() throws Exception {
    var existing = repository.findById(TestConstants.TENANT_ID);
    assertTrue(existing.isPresent());

    mockMvc.perform(MockMvcRequestBuilders.delete("/tenants/{id}", TestConstants.TENANT_ID)
        .header(TOKEN, AUTH_TOKEN))
      .andExpect(status().isNoContent());

    repository.findById(TestConstants.TENANT_ID)
      .ifPresent(tenantEntity -> Assertions.fail("Tenant is not deleted: " + TestConstants.TENANT_ID));

    assertThat(okapiService.getTenant(TENANT4.getName())).isNotNull();
    assertNull(keycloakTenantListener);
  }

  @Test
  @WireMockStub("/wiremock/stubs/mod-authtoken/verify-token-update-not-authorized.json")
  void update_negative_unauthorized() throws Exception {
    var tenant = copyFrom().description("modified").type(VIRTUAL);

    mockMvc.perform(put("/tenants/{id}", tenant.getId())
        .contentType(APPLICATION_JSON)
        .header(TOKEN, AUTH_TOKEN)
        .content(TestUtils.asJsonString(tenant)))
      .andExpect(status().isUnauthorized());
  }

  @Test
  @WireMockStub("/wiremock/stubs/mod-authtoken/verify-token-update-permission-denied.json")
  void update_negative_forbidden() throws Exception {
    var tenant = copyFrom().description("modified").type(VIRTUAL);

    mockMvc.perform(put("/tenants/{id}", tenant.getId())
        .contentType(APPLICATION_JSON)
        .header(TOKEN, AUTH_TOKEN)
        .content(TestUtils.asJsonString(tenant)))
      .andExpect(status().isForbidden());
  }

  private static Tenant copyFrom() {
    var result = new Tenant();

    result.setId(TenantOkapiIT.TENANT1.getId());
    result.setName(TenantOkapiIT.TENANT1.getName());
    result.setDescription(TenantOkapiIT.TENANT1.getDescription());
    result.setType(TenantOkapiIT.TENANT1.getType());
    result.setMetadata(TenantOkapiIT.TENANT1.getMetadata());
    result.setAttributes(TenantOkapiIT.TENANT1.getAttributes());

    return result;
  }
}
