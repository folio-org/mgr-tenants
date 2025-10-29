package org.folio.tm.integration.entitlements;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.folio.tm.integration.okapi.OkapiHeaders.TOKEN;
import static org.mockito.Mockito.when;

import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.folio.test.types.UnitTest;
import org.folio.tm.exception.RequestValidationException;
import org.folio.tm.integration.entitlements.model.Entitlement;
import org.folio.tm.integration.entitlements.model.EntitlementsResponse;
import org.folio.tm.support.TestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@UnitTest
@ExtendWith(MockitoExtension.class)
class TenantEntitlementsServiceTest {

  @InjectMocks private TenantEntitlementsService service;
  @Mock private TenantEntitlementsClient client;

  private MockHttpServletRequest mockRequest;

  @BeforeEach
  void setUp() {
    mockRequest = new MockHttpServletRequest();
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(mockRequest));
  }

  @AfterEach
  void tearDown() {
    RequestContextHolder.resetRequestAttributes();
    TestUtils.verifyNoMoreInteractions(this);
  }

  @Test
  void checkTenantCanBeDeleted_negative_hasEntitlements() {
    var tenantName = "test-tenant";
    var tenantId = UUID.randomUUID();
    var token = "test-token";
    var entitlement = new Entitlement("app-1", tenantId.toString(), List.of("mod-test-1.0.0"));
    var response = new EntitlementsResponse(List.of(entitlement), 1);

    mockRequest.addHeader(TOKEN, token);
    when(client.getEntitlements(1, "tenantId==" + tenantId, token)).thenReturn(response);

    assertThatThrownBy(() -> service.checkTenantCanBeDeleted(tenantName, tenantId))
      .isInstanceOf(RequestValidationException.class)
      .hasMessage("Cannot delete tenant")
      .satisfies(ex -> {
        var exception = (RequestValidationException) ex;
        assertThat(exception.getErrorParameters()).hasSize(1);
        assertThat(exception.getErrorParameters().getFirst().getKey())
          .isEqualTo("cause");
        assertThat(exception.getErrorParameters().getFirst().getValue())
          .isEqualTo("Please uninstall applications first");
      });
  }

  @Test
  void checkTenantCanBeDeleted_positive_noEntitlements() {
    var tenantName = "test-tenant";
    var tenantId = UUID.randomUUID();
    var token = "test-token";
    var response = new EntitlementsResponse(emptyList(), 0);

    mockRequest.addHeader(TOKEN, token);
    when(client.getEntitlements(1, "tenantId==" + tenantId, token)).thenReturn(response);

    assertThatCode(() -> service.checkTenantCanBeDeleted(tenantName, tenantId))
      .doesNotThrowAnyException();
  }

  @Test
  void checkTenantCanBeDeleted_positive_nullResponse() {
    var tenantName = "test-tenant";
    var tenantId = UUID.randomUUID();
    var token = "test-token";

    mockRequest.addHeader(TOKEN, token);
    when(client.getEntitlements(1, "tenantId==" + tenantId, token)).thenReturn(null);

    assertThatCode(() -> service.checkTenantCanBeDeleted(tenantName, tenantId))
      .doesNotThrowAnyException();
  }

  @Test
  void checkTenantCanBeDeleted_positive_notFound() {
    var tenantName = "test-tenant";
    var tenantId = UUID.randomUUID();
    var token = "test-token";
    var feignRequest = Request.create(Request.HttpMethod.GET, "/test", Collections.emptyMap(),
      null, new RequestTemplate());

    mockRequest.addHeader(TOKEN, token);
    when(client.getEntitlements(1, "tenantId==" + tenantId, token))
      .thenThrow(new FeignException.NotFound("Not found", feignRequest, null, null));

    assertThatCode(() -> service.checkTenantCanBeDeleted(tenantName, tenantId))
      .doesNotThrowAnyException();
  }

  @Test
  void checkTenantCanBeDeleted_negative_serviceUnavailable() {
    var tenantName = "test-tenant";
    var tenantId = UUID.randomUUID();
    var token = "test-token";
    var feignRequest = Request.create(Request.HttpMethod.GET, "/test", Collections.emptyMap(),
      null, new RequestTemplate());

    mockRequest.addHeader(TOKEN, token);
    when(client.getEntitlements(1, "tenantId==" + tenantId, token))
      .thenThrow(new FeignException.ServiceUnavailable("Service unavailable", feignRequest, null, null));

    assertThatThrownBy(() -> service.checkTenantCanBeDeleted(tenantName, tenantId))
      .isInstanceOf(RequestValidationException.class)
      .hasMessage("Cannot delete tenant")
      .satisfies(ex -> {
        var exception = (RequestValidationException) ex;
        assertThat(exception.getErrorParameters()).hasSize(1);
        assertThat(exception.getErrorParameters().getFirst().getKey())
          .isEqualTo("cause");
        assertThat(exception.getErrorParameters().getFirst().getValue())
          .isEqualTo("Unable to verify tenant's entitlements state, try again");
      });
  }

  @Test
  void checkTenantCanBeDeleted_negative_unexpectedError() {
    var tenantName = "test-tenant";
    var tenantId = UUID.randomUUID();
    var token = "test-token";

    mockRequest.addHeader(TOKEN, token);
    when(client.getEntitlements(1, "tenantId==" + tenantId, token))
      .thenThrow(new RuntimeException("Unexpected error"));

    assertThatThrownBy(() -> service.checkTenantCanBeDeleted(tenantName, tenantId))
      .isInstanceOf(RequestValidationException.class)
      .hasMessage("Cannot delete tenant")
      .satisfies(ex -> {
        var exception = (RequestValidationException) ex;
        assertThat(exception.getErrorParameters()).hasSize(1);
        assertThat(exception.getErrorParameters().getFirst().getKey())
          .isEqualTo("cause");
        assertThat(exception.getErrorParameters().getFirst().getValue())
          .isEqualTo("Unable to verify tenant's entitlements state, try again");
      });
  }

  @Test
  void checkTenantCanBeDeleted_negative_nullTenantId() {
    var tenantName = "test-tenant";

    assertThatThrownBy(() -> service.checkTenantCanBeDeleted(tenantName, null))
      .isInstanceOf(NullPointerException.class)
      .hasMessageContaining("tenantId cannot be null");
  }

  @Test
  void checkTenantCanBeDeleted_positive_missingToken() {
    var tenantName = "test-tenant";
    var tenantId = UUID.randomUUID();
    var response = new EntitlementsResponse(emptyList(), 0);

    // Don't add token to mockRequest - token will be null
    when(client.getEntitlements(1, "tenantId==" + tenantId, null)).thenReturn(response);

    assertThatCode(() -> service.checkTenantCanBeDeleted(tenantName, tenantId))
      .doesNotThrowAnyException();
  }

  @Test
  void checkTenantCanBeDeleted_positive_emptyToken() {
    var tenantName = "test-tenant";
    var tenantId = UUID.randomUUID();
    var response = new EntitlementsResponse(emptyList(), 0);

    mockRequest.addHeader(TOKEN, "");
    // Empty token is treated as null
    when(client.getEntitlements(1, "tenantId==" + tenantId, null)).thenReturn(response);

    assertThatCode(() -> service.checkTenantCanBeDeleted(tenantName, tenantId))
      .doesNotThrowAnyException();
  }

  @Test
  void checkTenantCanBeDeleted_positive_noRequestContext() {
    var tenantName = "test-tenant";
    var tenantId = UUID.randomUUID();
    var response = new EntitlementsResponse(emptyList(), 0);

    // Clear the request context to simulate no request context available
    RequestContextHolder.resetRequestAttributes();
    when(client.getEntitlements(1, "tenantId==" + tenantId, null)).thenReturn(response);

    assertThatCode(() -> service.checkTenantCanBeDeleted(tenantName, tenantId))
      .doesNotThrowAnyException();
  }

  @Test
  void checkTenantCanBeDeleted_positive_authorizationHeaderWithBearer() {
    var tenantName = "test-tenant";
    var tenantId = UUID.randomUUID();
    var token = "test-bearer-token";
    var response = new EntitlementsResponse(emptyList(), 0);

    mockRequest.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    when(client.getEntitlements(1, "tenantId==" + tenantId, token)).thenReturn(response);

    assertThatCode(() -> service.checkTenantCanBeDeleted(tenantName, tenantId))
      .doesNotThrowAnyException();
  }

  @Test
  void checkTenantCanBeDeleted_positive_authorizationHeaderWithBearerLowercase() {
    var tenantName = "test-tenant";
    var tenantId = UUID.randomUUID();
    var token = "test-bearer-token";
    var response = new EntitlementsResponse(emptyList(), 0);

    // Test case-insensitive Bearer prefix removal
    mockRequest.addHeader(HttpHeaders.AUTHORIZATION, "bearer " + token);
    when(client.getEntitlements(1, "tenantId==" + tenantId, token)).thenReturn(response);

    assertThatCode(() -> service.checkTenantCanBeDeleted(tenantName, tenantId))
      .doesNotThrowAnyException();
  }

  @Test
  void checkTenantCanBeDeleted_positive_okapiTokenHasPriority() {
    var tenantId = UUID.randomUUID();
    var okapiToken = "okapi-token";
    var bearerToken = "bearer-token";
    var response = new EntitlementsResponse(emptyList(), 0);

    mockRequest.addHeader(TOKEN, okapiToken);
    mockRequest.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken);
    when(client.getEntitlements(1, "tenantId==" + tenantId, okapiToken)).thenReturn(response);

    assertThatCode(() -> service.checkTenantCanBeDeleted("test-tenant", tenantId))
      .doesNotThrowAnyException();
  }
}
