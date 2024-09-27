package org.folio.tm.it;

import static java.lang.String.valueOf;
import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.tm.domain.dto.TenantType.VIRTUAL;
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
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@IntegrationTest
@TestPropertySource(properties = {"application.okapi.enabled=false", "application.keycloak.enabled=false"})
@Sql(scripts = "classpath:/sql/populate_tenants.sql", executionPhase = BEFORE_TEST_METHOD)
@Sql(scripts = "classpath:/sql/clear_tenants.sql", executionPhase = AFTER_TEST_METHOD)
class TenantNoItegrationsIT extends BaseIntegrationTest {

  private static final String AUTH_TOKEN = "dGVzdC1hdXRoLnRva2Vu";
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

  @Autowired(required = false)
  private OkapiService okapiService;

  @Test
  void createTenant_positive() throws Exception {
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

    assertNull(keycloakTenantListener);
    assertNull(okapiService);
  }

  @Test
  void updateTenant_positive() throws Exception {
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

    assertNull(keycloakTenantListener);
    assertNull(okapiService);
  }

  @Test
  void deleteTenant_positive() throws Exception {
    var existing = repository.findById(TestConstants.TENANT_ID);
    assertTrue(existing.isPresent());

    mockMvc.perform(MockMvcRequestBuilders.delete("/tenants/{id}", TestConstants.TENANT_ID)
        .header(TOKEN, AUTH_TOKEN))
      .andExpect(status().isNoContent());

    repository.findById(TestConstants.TENANT_ID)
      .ifPresent(tenantEntity -> Assertions.fail("Tenant is not deleted: " + TestConstants.TENANT_ID));

    assertNull(keycloakTenantListener);
    assertNull(okapiService);
  }

  private static Tenant copyFrom() {
    var result = new Tenant();

    result.setId(TenantNoItegrationsIT.TENANT1.getId());
    result.setName(TenantNoItegrationsIT.TENANT1.getName());
    result.setDescription(TenantNoItegrationsIT.TENANT1.getDescription());
    result.setType(TenantNoItegrationsIT.TENANT1.getType());
    result.setMetadata(TenantNoItegrationsIT.TENANT1.getMetadata());
    result.setAttributes(TenantNoItegrationsIT.TENANT1.getAttributes());

    return result;
  }
}
