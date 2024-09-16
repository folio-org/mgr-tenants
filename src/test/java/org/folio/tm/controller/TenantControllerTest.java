package org.folio.tm.controller;

import static feign.Request.HttpMethod.GET;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.tm.integration.okapi.OkapiHeaders.TOKEN;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import feign.FeignException.Forbidden;
import feign.FeignException.NotFound;
import feign.Request;
import feign.RequestTemplate;
import java.util.List;
import java.util.UUID;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.folio.jwt.openid.JsonWebTokenParser;
import org.folio.security.exception.NotAuthorizedException;
import org.folio.security.integration.keycloak.client.KeycloakAuthClient;
import org.folio.test.extensions.EnableKeycloakSecurity;
import org.folio.test.types.UnitTest;
import org.folio.tm.domain.dto.Tenant;
import org.folio.tm.domain.dto.Tenants;
import org.folio.tm.service.TenantService;
import org.folio.tm.support.TestConstants;
import org.folio.tm.support.TestUtils;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@UnitTest
@EnableKeycloakSecurity
@Import({ControllerTestConfiguration.class, TenantController.class})
@WebMvcTest(TenantController.class)
class TenantControllerTest {

  private static final String AUTH_TOKEN = "dGVzdC1hdXRoLnRva2Vu";
  private static final String TOKEN_ISSUER = "https://keycloak/realms/test";
  private static final String TOKEN_SUB = UUID.randomUUID().toString();

  @Autowired private MockMvc mockMvc;
  @Mock private JsonWebToken jsonWebToken;
  @MockBean private JsonWebTokenParser jsonWebTokenParser;
  @MockBean private TenantService tenantService;
  @MockBean private KeycloakAuthClient authClient;

  @Test
  void getById_positive() throws Exception {
    var tenant = TestConstants.tenant();

    when(tenantService.getTenantById(TestConstants.TENANT_ID)).thenReturn(tenant);
    var mvcResult = mockMvc.perform(MockMvcRequestBuilders.get("/tenants/{id}", TestConstants.TENANT_ID)
        .contentType(APPLICATION_JSON)
        .header(TOKEN, AUTH_TOKEN))
      .andExpect(status().isOk())
      .andReturn();

    var actual = TestUtils.parseResponse(mvcResult, Tenant.class);
    assertThat(actual).isEqualTo(tenant);
  }

  @Test
  void getById_negative() throws Exception {
    var errorMessage = "Tenant not found by id: " + TestConstants.TENANT_ID;
    var request = Request.create(GET, "/tenants/" + TestConstants.TENANT_ID, emptyMap(), null, (RequestTemplate) null);
    var error = new NotFound(errorMessage, request, null, emptyMap());

    when(tenantService.getTenantById(TestConstants.TENANT_ID)).thenThrow(error);
    mockMvc.perform(MockMvcRequestBuilders.get("/tenants/{id}", TestConstants.TENANT_ID)
        .contentType(APPLICATION_JSON)
        .header(TOKEN, AUTH_TOKEN))
      .andExpect(status().isNotFound())
      .andExpect(jsonPath("$.errors[0].message", is(errorMessage)))
      .andExpect(jsonPath("$.errors[0].type", is("NotFound")))
      .andExpect(jsonPath("$.errors[0].code", is("not_found_error")));
  }

  @Test
  void getById_negative_forbidden() throws Exception {
    var request = Request.create(GET, "/tenants/" + TestConstants.TENANT_ID, emptyMap(), null, (RequestTemplate) null);
    var error = new Forbidden("Forbidden", request, null, emptyMap());

    when(tenantService.getTenantById(TestConstants.TENANT_ID)).thenThrow(error);
    mockMvc.perform(MockMvcRequestBuilders.get("/tenants/{id}", TestConstants.TENANT_ID)
        .contentType(APPLICATION_JSON)
        .header(TOKEN, AUTH_TOKEN))
      .andExpect(status().isInternalServerError())
      .andExpect(jsonPath("$.errors[0].message", is("Forbidden")))
      .andExpect(jsonPath("$.errors[0].type", is("Forbidden")))
      .andExpect(jsonPath("$.errors[0].code", is("unknown_error")));
  }

  @Test
  void create_positive() throws Exception {
    var tenant = TestConstants.tenant();

    when(tenantService.createTenant(tenant)).thenReturn(tenant);
    when(jsonWebTokenParser.parse(AUTH_TOKEN)).thenReturn(jsonWebToken);
    when(jsonWebToken.getIssuer()).thenReturn(TOKEN_ISSUER);
    when(jsonWebToken.getSubject()).thenReturn(TOKEN_SUB);

    var mvcResult = mockMvc.perform(post("/tenants")
        .content(TestUtils.asJsonString(tenant))
        .header(TOKEN, AUTH_TOKEN)
        .contentType(APPLICATION_JSON))
      .andExpect(status().isCreated())
      .andReturn();

    var actual = TestUtils.parseResponse(mvcResult, Tenant.class);
    assertThat(actual).isEqualTo(tenant);
  }

  @Test
  void update_positive() throws Exception {
    var tenant = TestConstants.tenant();
    when(tenantService.updateTenantById(TestConstants.TENANT_ID, tenant)).thenReturn(tenant);
    when(jsonWebTokenParser.parse(AUTH_TOKEN)).thenReturn(jsonWebToken);
    when(jsonWebToken.getIssuer()).thenReturn(TOKEN_ISSUER);
    when(jsonWebToken.getSubject()).thenReturn(TOKEN_SUB);

    var mvcResult = mockMvc.perform(MockMvcRequestBuilders.put("/tenants/{id}", TestConstants.TENANT_ID)
        .content(TestUtils.asJsonString(tenant))
        .contentType(APPLICATION_JSON)
        .header(TOKEN, AUTH_TOKEN))
      .andExpect(status().isOk())
      .andReturn();

    var actual = TestUtils.parseResponse(mvcResult, Tenant.class);
    assertThat(actual).isEqualTo(tenant);
  }

  @Test
  void delete_positive() throws Exception {
    doNothing().when(tenantService).deleteTenantById(TestConstants.TENANT_ID);
    when(jsonWebTokenParser.parse(AUTH_TOKEN)).thenReturn(jsonWebToken);
    when(jsonWebToken.getIssuer()).thenReturn(TOKEN_ISSUER);
    when(jsonWebToken.getSubject()).thenReturn(TOKEN_SUB);

    mockMvc.perform(MockMvcRequestBuilders.delete("/tenants/{id}", TestConstants.TENANT_ID)
        .contentType(APPLICATION_JSON)
        .header(TOKEN, AUTH_TOKEN))
      .andExpect(status().isNoContent());
  }

  @Test
  void getByQuery_negative_invalidLimit() throws Exception {
    var errorMessage = "getTenantsByQuery.limit must be greater than or equal to 0";
    mockMvc.perform(get("/tenants")
        .param("offset", String.valueOf(0))
        .param("limit", String.valueOf(-100))
        .contentType(APPLICATION_JSON)
        .header(TOKEN, AUTH_TOKEN))
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.total_records", is(1)))
      .andExpect(jsonPath("$.errors[0].message", is(errorMessage)))
      .andExpect(jsonPath("$.errors[0].type", is("ConstraintViolationException")))
      .andExpect(jsonPath("$.errors[0].code", is("validation_error")));
  }

  @Test
  void getByQuery_positive() throws Exception {
    var tenant = TestConstants.tenant();
    var tenants = new Tenants().tenants(List.of(tenant)).totalRecords(1);

    when(tenantService.getTenantsByQuery("query", 0, 10)).thenReturn(tenants);
    var mvcResult = mockMvc.perform(get("/tenants")
        .param("query", "query")
        .param("offset", String.valueOf(0))
        .param("limit", String.valueOf(10))
        .contentType(APPLICATION_JSON)
        .header(TOKEN, AUTH_TOKEN))
      .andExpect(status().isOk())
      .andReturn();

    var actual = TestUtils.parseResponse(mvcResult, Tenants.class);
    assertThat(actual).isEqualTo(tenants);
  }

  @Test
  void getByQuery_positive_limitIsZero() throws Exception {
    var tenants = new Tenants().tenants(emptyList()).totalRecords(1);

    when(tenantService.getTenantsByQuery("query", 0, 0)).thenReturn(tenants);
    var mvcResult = mockMvc.perform(get("/tenants")
        .param("query", "query")
        .param("offset", String.valueOf(0))
        .param("limit", String.valueOf(0))
        .contentType(APPLICATION_JSON)
        .header(TOKEN, AUTH_TOKEN))
      .andExpect(status().isOk())
      .andReturn();

    var actual = TestUtils.parseResponse(mvcResult, Tenants.class);
    assertThat(actual).isEqualTo(tenants);
  }

  @Test
  void delete_negative_unauthorized() throws Exception {
    doNothing().when(tenantService).deleteTenantById(TestConstants.TENANT_ID);
    when(authClient.evaluatePermissions(anyMap(), anyString())).thenThrow(new NotAuthorizedException("test"));

    mockMvc.perform(MockMvcRequestBuilders.delete("/tenants/{id}", TestConstants.TENANT_ID)
        .contentType(APPLICATION_JSON)
        .header(TOKEN, AUTH_TOKEN))
      .andExpect(status().isUnauthorized());
  }

  @Test
  void delete_negative_noAuthToken() throws Exception {
    doNothing().when(tenantService).deleteTenantById(TestConstants.TENANT_ID);

    mockMvc.perform(MockMvcRequestBuilders.delete("/tenants/{id}", TestConstants.TENANT_ID)
        .contentType(APPLICATION_JSON))
      .andExpect(status().isUnauthorized());
  }
}
