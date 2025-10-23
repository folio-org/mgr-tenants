package org.folio.tm.it;

import static java.lang.String.valueOf;
import static java.util.Objects.requireNonNull;
import static org.folio.tm.domain.dto.TenantType.DEFAULT;
import static org.folio.tm.domain.dto.TenantType.VIRTUAL;
import static org.folio.tm.support.TestConstants.AUTH_TOKEN;
import static org.folio.tm.support.TestConstants.TENANT_ID;
import static org.folio.tm.support.TestConstants.TENANT_NAME;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;
import static org.springframework.test.context.jdbc.SqlMergeMode.MergeMode.MERGE;
import static org.springframework.test.json.JsonCompareMode.LENIENT;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;
import org.folio.test.extensions.WireMockStub;
import org.folio.test.types.IntegrationTest;
import org.folio.tm.base.BaseIntegrationTest;
import org.folio.tm.domain.dto.Tenant;
import org.folio.tm.domain.dto.TenantAttribute;
import org.folio.tm.support.TestConstants;
import org.folio.tm.support.TestUtils;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@IntegrationTest
@SqlMergeMode(MERGE)
@Sql(scripts = "classpath:/sql/clear_tenants.sql", executionPhase = AFTER_TEST_METHOD)
class TenantIT extends BaseIntegrationTest {

  private static final Tenant TENANT1 = new Tenant()
    .id(TENANT_ID)
    .name(TENANT_NAME)
    .description(TestConstants.TENANT_DESCRIPTION)
    .type(DEFAULT);

  private static final Tenant TENANT4 = new Tenant()
    .id(UUID.fromString("12a50c0a-b3b7-4992-bd70-442ac1d8e212"))
    .name("tenant4")
    .description("test tenant4")
    .type(VIRTUAL);

  @Test
  @Sql("classpath:/sql/populate_tenants.sql")
  void getById_positive() throws Exception {
    doGet("/tenants/{id}", TENANT_ID)
      .andExpect(json("tenant/get-tenant-response.json"));
  }

  @Test
  @Sql("classpath:/sql/populate_tenants.sql")
  void getAll_positive() throws Exception {
    doGet("/tenants").andExpect(json("tenant/get-all-tenants-response.json"));
  }

  @Test
  void getAll_positive_noTenants() throws Exception {
    doGet("/tenants")
      .andExpect(jsonPath("$.tenants", is(empty())))
      .andExpect(jsonPath("$.totalRecords", is(0)));
  }

  @Test
  @Sql("classpath:/sql/populate_tenants.sql")
  void getByQuery_positive() throws Exception {
    doGet(get("/tenants")
      .queryParam("query", "name==\"tenant1\" or name==\"tenant2\" or name==\"tenant3\""))
      .andExpect(json("tenant/get-tenants-by-query-response.json"));
  }

  @Test
  @WireMockStub(scripts = {
    "/wiremock/stubs/okapi/create-tenant.json",
    "/wiremock/stubs/okapi/get-tenant-not-found.json"
  })
  void createTenant_positive() throws Exception {
    mockMvc.perform(post("/tenants")
        .contentType(APPLICATION_JSON)
        .header(TOKEN, AUTH_TOKEN)
        .content(TestUtils.asJsonString(TENANT4)))
      .andExpect(status().isCreated())
      .andExpect(jsonPath("$.id", is(valueOf(TENANT4.getId()))))
      .andExpect(jsonPath("$.name", is(TENANT4.getName())))
      .andExpect(jsonPath("$.description", is(TENANT4.getDescription())))
      .andExpect(jsonPath("$.type", is(TENANT4.getType().getValue())))
      .andExpect(jsonPath("$.metadata", is(notNullValue())));
  }

  @Test
  @WireMockStub(scripts = {
    "/wiremock/stubs/okapi/create-tenant.json",
    "/wiremock/stubs/okapi/get-tenant-not-found.json"
  })
  void createTenant_positive_withAttributes() throws Exception {
    var tenantToCreate = copyFrom(TENANT4)
      .addAttributesItem(tenantAttribute("attribute key", "attribute value"));

    mockMvc.perform(post("/tenants")
        .contentType(APPLICATION_JSON)
        .header(TOKEN, AUTH_TOKEN)
        .content(TestUtils.asJsonString(tenantToCreate)))
      .andExpect(status().isCreated())
      .andExpect(jsonPath("$.id", notNullValue()))
      .andExpect(jsonPath("$.name", is(tenantToCreate.getName())))
      .andExpect(jsonPath("$.description", is(tenantToCreate.getDescription())))
      .andExpect(jsonPath("$.type", is(tenantToCreate.getType().getValue())))
      .andExpect(jsonPath("$.metadata", is(notNullValue())))
      .andExpect(jsonPath("$.attributes[0].id", notNullValue()))
      .andExpect(jsonPath("$.attributes[0].key", is("attribute key")))
      .andExpect(jsonPath("$.attributes[0].value", is("attribute value")))
      .andExpect(jsonPath("$.attributes[0].metadata", notNullValue()));
  }

  @Test
  @Sql("classpath:/sql/populate_tenants.sql")
  void createTenant_negative_nameExists() throws Exception {
    var tenant = copyFrom(TENANT4).name("tenant3");

    mockMvc.perform(post("/tenants")
        .contentType(APPLICATION_JSON)
        .header(TOKEN, AUTH_TOKEN)
        .content(TestUtils.asJsonString(tenant)))
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.total_records", is(1)))
      .andExpect(jsonPath("$.errors[0].message", is("Tenant's name already taken: tenant3")))
      .andExpect(jsonPath("$.errors[0].type", is("RequestValidationException")))
      .andExpect(jsonPath("$.errors[0].code", is("validation_error")));
  }

  @Test
  void createTenant_negative_nameEmpty() throws Exception {
    var tenant = copyFrom(TENANT4).name(null);

    mockMvc.perform(post("/tenants")
        .contentType(APPLICATION_JSON)
        .header(TOKEN, AUTH_TOKEN)
        .content(TestUtils.asJsonString(tenant)))
      .andExpectAll(argumentNotValidErr("must not be null", "name", null));
  }

  @Test
  @Sql("classpath:/sql/populate_tenants.sql")
  @WireMockStub(scripts = {
    "/wiremock/stubs/okapi/update-tenant.json"
  })
  void updateTenant_positive() throws Exception {
    var tenant = copyFrom(TENANT1).description("modified").type(VIRTUAL);

    mockMvc.perform(put("/tenants/{id}", tenant.getId())
        .contentType(APPLICATION_JSON)
        .header(TOKEN, AUTH_TOKEN)
        .content(TestUtils.asJsonString(tenant)))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.description", is(tenant.getDescription())))
      .andExpect(jsonPath("$.type", is(tenant.getType().getValue())));

    doGet("/tenants/{id}", tenant.getId())
      .andExpect(jsonPath("$.description", is(tenant.getDescription())))
      .andExpect(jsonPath("$.type", is(tenant.getType().getValue())));
  }

  @Test
  @Sql("classpath:/sql/populate_tenants.sql")
  @WireMockStub(scripts = {
    "/wiremock/stubs/okapi/update-tenant.json"
  })
  void updateTenant_positive_addAttributes() throws Exception {
    var tenant = copyFrom(TENANT1).description("modified").type(VIRTUAL)
      .attributes(List.of(tenantAttribute("attr1", "1"), tenantAttribute("attr2", "2")));

    mockMvc.perform(put("/tenants/{id}", tenant.getId())
        .contentType(APPLICATION_JSON)
        .header(TOKEN, AUTH_TOKEN)
        .content(TestUtils.asJsonString(tenant)))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.description", is(tenant.getDescription())))
      .andExpect(jsonPath("$.type", is(tenant.getType().getValue())))
      .andExpect(jsonPath("$.attributes[0].id", notNullValue()))
      .andExpect(jsonPath("$.attributes[0].key", is("attr1")))
      .andExpect(jsonPath("$.attributes[0].value", is("1")))
      .andExpect(jsonPath("$.attributes[0].metadata", notNullValue()))
      .andExpect(jsonPath("$.attributes[1].id", notNullValue()))
      .andExpect(jsonPath("$.attributes[1].key", is("attr2")))
      .andExpect(jsonPath("$.attributes[1].value", is("2")))
      .andExpect(jsonPath("$.attributes[1].metadata", notNullValue()));
  }

  @Test
  @Sql("classpath:/sql/populate_tenants.sql")
  @WireMockStub(scripts = {
    "/wiremock/stubs/okapi/update-tenant.json"
  })
  void updateTenant_negative_notFound() throws Exception {
    var tenant = TENANT4;

    mockMvc.perform(put("/tenants/{id}", tenant.getId())
        .contentType(APPLICATION_JSON)
        .header(TOKEN, AUTH_TOKEN)
        .content(TestUtils.asJsonString(tenant)))
      .andExpectAll(notFoundWithMsg("Tenant is not found: id = " + tenant.getId()));
  }

  @Test
  @Sql("classpath:/sql/populate_tenants.sql")
  void updateTenant_negative_idDoesntMatch() throws Exception {
    var tenant = TENANT4;

    mockMvc.perform(put("/tenants/{id}", TENANT1.getId())
        .contentType(APPLICATION_JSON)
        .header(TOKEN, AUTH_TOKEN)
        .content(TestUtils.asJsonString(tenant)))
      .andExpectAll(requestValidationErr("Tenant id doesn't match", "id", tenant.getId()));
  }

  @Test
  @Sql("classpath:/sql/populate_tenants.sql")
  void updateTenant_negative_nameModified() throws Exception {
    var tenant = copyFrom(TENANT1).name("modified");

    mockMvc.perform(put("/tenants/{id}", tenant.getId())
        .contentType(APPLICATION_JSON)
        .header(TOKEN, AUTH_TOKEN)
        .content(TestUtils.asJsonString(tenant)))
      .andExpectAll(requestValidationErr("Tenant name cannot be modified", "name", tenant.getName()));
  }

  @Test
  @Sql("classpath:/sql/populate_tenants.sql")
  @WireMockStub(scripts = {
    "/wiremock/stubs/okapi/delete-tenant.json",
    "/wiremock/stubs/okapi/get-tenant-exist.json",
    "/wiremock/stubs/mgr-tenant-entitlements/get-entitlements-no-apps.json"
  })
  void deleteTenant_positive() throws Exception {
    var tenantId = TENANT_ID.toString();
    doGet("/tenants/{id}", tenantId).andExpect(jsonPath("$.id", is(tenantId)));

    mockMvc.perform(MockMvcRequestBuilders.delete("/tenants/{id}", tenantId)
        .header(TOKEN, AUTH_TOKEN))
      .andExpect(status().isNoContent());

    attemptGet("/tenants/{id}", tenantId)
      .andExpect(status().isNotFound());
  }

  @Test
  @Sql("classpath:/sql/populate_tenants.sql")
  @WireMockStub(scripts = {
    "/wiremock/stubs/okapi/get-tenant5-exist.json",
    "/wiremock/stubs/okapi/delete-tenant.json",
    "/wiremock/stubs/mgr-tenant-entitlements/get-entitlements-no-apps.json"
  })
  void deleteTenantWithAttributes_positive() throws Exception {
    var tenantId = "42e36904-d009-4884-8338-3df14a18dfef";
    doGet("/tenants/{id}", tenantId)
      .andExpect(jsonPath("$.id", is(tenantId)))
      .andExpect(jsonPath("$.name", is("tenant5")));

    mockMvc.perform(delete("/tenants/{id}", tenantId)
        .header(TOKEN, AUTH_TOKEN))
      .andExpect(status().isNoContent());

    attemptGet("/tenants/{id}", tenantId)
      .andExpect(status().isNotFound());
  }

  @Test
  @Sql("classpath:/sql/populate_tenants.sql")
  void deleteTenant_positive_notPresent() throws Exception {
    var tenantId = requireNonNull(TENANT4.getId()).toString();
    attemptGet("/tenants/{id}", tenantId).andExpect(status().isNotFound());

    mockMvc.perform(delete("/tenants/{id}", TENANT4.getId())
        .header(TOKEN, AUTH_TOKEN))
      .andExpect(status().isNoContent());
  }

  @Test
  @Sql("classpath:/sql/populate_tenants.sql")
  @WireMockStub(scripts = {
    "/wiremock/stubs/okapi/get-tenant-exist.json",
    "/wiremock/stubs/mgr-tenant-entitlements/get-entitlements-with-apps.json"
  })
  void deleteTenant_negative_hasActiveEntitlements() throws Exception {
    var tenantId = TENANT_ID.toString();
    doGet("/tenants/{id}", tenantId).andExpect(jsonPath("$.id", is(tenantId)));

    mockMvc.perform(delete("/tenants/{id}", tenantId)
        .header(TOKEN, AUTH_TOKEN))
      .andExpect(status().isBadRequest())
      .andExpect(content().json("""
        {
            "errors": [
                {
                    "message": "Cannot delete tenant",
                    "type": "RequestValidationException",
                    "code": "validation_error",
                    "parameters": [
                        {
                            "key": "cause",
                            "value": "Please uninstall applications first"
                        }
                    ]
                }
            ]
        }
        """, LENIENT));

    // Verify tenant still exists
    doGet("/tenants/{id}", tenantId)
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.id", is(tenantId)));
  }

  @Test
  @Sql("classpath:/sql/populate_tenants.sql")
  @WireMockStub(scripts = {
    "/wiremock/stubs/okapi/create-tenant.json",
    "/wiremock/stubs/okapi/get-tenant-not-found.json"
  })
  void createTenant_positive_secureTenant() throws Exception {
    var tenant = TENANT4.secure(true);

    mockMvc.perform(post("/tenants")
        .contentType(APPLICATION_JSON)
        .header(TOKEN, AUTH_TOKEN)
        .content(TestUtils.asJsonString(tenant)))
      .andExpect(status().isCreated())
      .andExpect(jsonPath("$.id", is(valueOf(tenant.getId()))))
      .andExpect(jsonPath("$.name", is(tenant.getName())))
      .andExpect(jsonPath("$.description", is(tenant.getDescription())))
      .andExpect(jsonPath("$.type", is(tenant.getType().getValue())))
      .andExpect(jsonPath("$.secure", is(tenant.getSecure())))
      .andExpect(jsonPath("$.metadata", is(notNullValue())));
  }

  @Test
  @Sql(scripts = "classpath:/sql/populate_tenants.sql")
  @WireMockStub(scripts = {
    "/wiremock/stubs/okapi/update-tenant.json"
  })
  void updateTenant_positive_secureTenant() throws Exception {
    var tenant = copyFrom(TENANT1).secure(true);

    mockMvc.perform(put("/tenants/{id}", tenant.getId())
        .contentType(APPLICATION_JSON)
        .header(TOKEN, AUTH_TOKEN)
        .content(TestUtils.asJsonString(tenant)))
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.total_records", is(1)))
      .andExpect(jsonPath("$.errors[0].message", is("Secure field cannot be modified")))
      .andExpect(jsonPath("$.errors[0].type", is("RequestValidationException")))
      .andExpect(jsonPath("$.errors[0].code", is("validation_error")));
  }

  private static Tenant copyFrom(Tenant source) {
    var result = new Tenant();

    result.setId(source.getId());
    result.setName(source.getName());
    result.setDescription(source.getDescription());
    result.setType(source.getType());
    result.setMetadata(source.getMetadata());
    result.setAttributes(source.getAttributes());

    return result;
  }

  private static TenantAttribute tenantAttribute(String attr2, String value) {
    return new TenantAttribute().key(attr2).value(value);
  }
}
