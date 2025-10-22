package org.folio.tm.integration.entitlements;

import static org.assertj.core.api.Assertions.assertThat;
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
  void hasTenantEntitlements_positive() {
    var tenantName = "test-tenant";
    var tenantId = UUID.randomUUID();
    var token = "test-token";
    var entitlement = new Entitlement("app-1", tenantId.toString(), List.of("mod-test-1.0.0"));
    var response = new EntitlementsResponse(List.of(entitlement), 1);

    mockRequest.addHeader(TOKEN, token);
    when(client.getEntitlements(1, "tenantId==" + tenantId, token)).thenReturn(response);

    var result = service.hasTenantEntitlements(tenantName, tenantId);

    assertThat(result).isTrue();
  }

  @Test
  void hasTenantEntitlements_positive_noEntitlements() {
    var tenantName = "test-tenant";
    var tenantId = UUID.randomUUID();
    var token = "test-token";
    var response = new EntitlementsResponse(Collections.emptyList(), 0);

    mockRequest.addHeader(TOKEN, token);
    when(client.getEntitlements(1, "tenantId==" + tenantId, token)).thenReturn(response);

    var result = service.hasTenantEntitlements(tenantName, tenantId);

    assertThat(result).isFalse();
  }

  @Test
  void hasTenantEntitlements_positive_nullResponse() {
    var tenantName = "test-tenant";
    var tenantId = UUID.randomUUID();
    var token = "test-token";

    mockRequest.addHeader(TOKEN, token);
    when(client.getEntitlements(1, "tenantId==" + tenantId, token)).thenReturn(null);

    var result = service.hasTenantEntitlements(tenantName, tenantId);

    assertThat(result).isFalse();
  }

  @Test
  void hasTenantEntitlements_positive_notFound() {
    var tenantName = "test-tenant";
    var tenantId = UUID.randomUUID();
    var token = "test-token";
    var feignRequest = Request.create(Request.HttpMethod.GET, "/test", Collections.emptyMap(),
      null, new RequestTemplate());

    mockRequest.addHeader(TOKEN, token);
    when(client.getEntitlements(1, "tenantId==" + tenantId, token))
      .thenThrow(new FeignException.NotFound("Not found", feignRequest, null, null));

    var result = service.hasTenantEntitlements(tenantName, tenantId);

    assertThat(result).isFalse();
  }

  @Test
  void hasTenantEntitlements_negative_serviceUnavailable() {
    var tenantName = "test-tenant";
    var tenantId = UUID.randomUUID();
    var token = "test-token";
    var feignRequest = Request.create(Request.HttpMethod.GET, "/test", Collections.emptyMap(),
      null, new RequestTemplate());

    mockRequest.addHeader(TOKEN, token);
    when(client.getEntitlements(1, "tenantId==" + tenantId, token))
      .thenThrow(new FeignException.ServiceUnavailable("Service unavailable", feignRequest, null, null));

    var result = service.hasTenantEntitlements(tenantName, tenantId);

    assertThat(result).isTrue();
  }

  @Test
  void hasTenantEntitlements_negative_unexpectedError() {
    var tenantName = "test-tenant";
    var tenantId = UUID.randomUUID();
    var token = "test-token";

    mockRequest.addHeader(TOKEN, token);
    when(client.getEntitlements(1, "tenantId==" + tenantId, token))
      .thenThrow(new RuntimeException("Unexpected error"));

    var result = service.hasTenantEntitlements(tenantName, tenantId);

    assertThat(result).isTrue();
  }

  @Test
  void hasTenantEntitlements_negative_nullTenantId() {
    var tenantName = "test-tenant";

    assertThatThrownBy(() -> service.hasTenantEntitlements(tenantName, null))
      .isInstanceOf(NullPointerException.class)
      .hasMessageContaining("tenantId cannot be null");
  }

  @Test
  void hasTenantEntitlements_negative_missingToken() {
    var tenantName = "test-tenant";
    var tenantId = UUID.randomUUID();

    // Don't add token to mockRequest - it will be null

    var result = service.hasTenantEntitlements(tenantName, tenantId);

    assertThat(result).isTrue();
  }

  @Test
  void hasTenantEntitlements_negative_emptyToken() {
    var tenantName = "test-tenant";
    var tenantId = UUID.randomUUID();

    mockRequest.addHeader(TOKEN, "");

    var result = service.hasTenantEntitlements(tenantName, tenantId);

    assertThat(result).isTrue();
  }

  @Test
  void hasTenantEntitlements_negative_noRequestContext() {
    var tenantName = "test-tenant";
    var tenantId = UUID.randomUUID();

    // Clear the request context to simulate no request context available
    RequestContextHolder.resetRequestAttributes();

    var result = service.hasTenantEntitlements(tenantName, tenantId);

    assertThat(result).isTrue();
  }
}
