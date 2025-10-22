package org.folio.tm.integration.entitlements;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.folio.tm.integration.okapi.OkapiHeaders.TOKEN;
import static org.mockito.Mockito.when;

import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.folio.test.types.UnitTest;
import org.folio.tm.integration.entitlements.model.Entitlement;
import org.folio.tm.integration.entitlements.model.EntitlementsResponse;
import org.folio.tm.support.TestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class TenantEntitlementsServiceTest {

  @InjectMocks private TenantEntitlementsService service;
  @Mock private TenantEntitlementsClient client;
  @Mock private HttpServletRequest request;

  @AfterEach
  void tearDown() {
    TestUtils.verifyNoMoreInteractions(this);
  }

  @Test
  void hasTenantEntitlements_positive() {
    var tenantName = "test-tenant";
    var tenantId = UUID.randomUUID();
    var token = "test-token";
    var entitlement = new Entitlement("app-1", tenantId.toString(), List.of("mod-test-1.0.0"));
    var response = new EntitlementsResponse(List.of(entitlement), 1);

    when(request.getHeader(TOKEN)).thenReturn(token);
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

    when(request.getHeader(TOKEN)).thenReturn(token);
    when(client.getEntitlements(1, "tenantId==" + tenantId, token)).thenReturn(response);

    var result = service.hasTenantEntitlements(tenantName, tenantId);

    assertThat(result).isFalse();
  }

  @Test
  void hasTenantEntitlements_positive_nullResponse() {
    var tenantName = "test-tenant";
    var tenantId = UUID.randomUUID();
    var token = "test-token";

    when(request.getHeader(TOKEN)).thenReturn(token);
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

    when(request.getHeader(TOKEN)).thenReturn(token);
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

    when(request.getHeader(TOKEN)).thenReturn(token);
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

    when(request.getHeader(TOKEN)).thenReturn(token);
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

    when(request.getHeader(TOKEN)).thenReturn(null);

    var result = service.hasTenantEntitlements(tenantName, tenantId);

    assertThat(result).isTrue();
  }

  @Test
  void hasTenantEntitlements_negative_emptyToken() {
    var tenantName = "test-tenant";
    var tenantId = UUID.randomUUID();

    when(request.getHeader(TOKEN)).thenReturn("");

    var result = service.hasTenantEntitlements(tenantName, tenantId);

    assertThat(result).isTrue();
  }
}
