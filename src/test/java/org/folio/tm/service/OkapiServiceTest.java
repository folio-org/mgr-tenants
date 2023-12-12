package org.folio.tm.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.folio.test.TestConstants.OKAPI_AUTH_TOKEN;
import static org.folio.tm.integration.okapi.OkapiHeaders.TOKEN;
import static org.folio.tm.support.TestConstants.TENANT_NAME;
import static org.folio.tm.support.TestConstants.tenant;
import static org.folio.tm.support.TestConstants.tenantDescriptor;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import feign.FeignException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.folio.test.types.UnitTest;
import org.folio.tm.integration.okapi.OkapiClient;
import org.folio.tm.integration.okapi.OkapiService;
import org.folio.tm.integration.okapi.exception.OkapiRequestException;
import org.folio.tm.integration.okapi.model.TenantDescriptor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class OkapiServiceTest {

  @InjectMocks private OkapiService okapiService;
  @Mock private HttpServletRequest httpServletRequest;
  @Mock private OkapiClient okapiClient;

  @Test
  void get_positive() {
    var expected = tenantDescriptor();
    when(okapiClient.getTenantById(TENANT_NAME, OKAPI_AUTH_TOKEN)).thenReturn(expected);
    when(httpServletRequest.getHeader(TOKEN)).thenReturn(OKAPI_AUTH_TOKEN);

    var result = okapiService.getTenant(TENANT_NAME);

    assertThat(expected).isEqualTo(result);
  }

  @Test
  void get_negative() {
    when(okapiClient.getTenantById(TENANT_NAME, OKAPI_AUTH_TOKEN)).thenThrow(EntityNotFoundException.class);
    when(httpServletRequest.getHeader(TOKEN)).thenReturn(OKAPI_AUTH_TOKEN);

    assertThatThrownBy(() -> okapiService.getTenant(TENANT_NAME))
      .isInstanceOf(OkapiRequestException.class)
      .hasMessage("Failed to get tenant by id: " + TENANT_NAME)
      .hasCauseInstanceOf(EntityNotFoundException.class);
  }

  @Test
  void create_positive() {
    var expected = tenantDescriptor();
    when(okapiClient.createTenant(expected, OKAPI_AUTH_TOKEN)).thenReturn(expected);
    when(okapiClient.getTenantById(expected.getName(), OKAPI_AUTH_TOKEN)).thenThrow(FeignException.NotFound.class);
    when(httpServletRequest.getHeader(TOKEN)).thenReturn(OKAPI_AUTH_TOKEN);

    var result = okapiService.createTenant(tenant());

    assertThat(result).isEqualTo(expected);
  }

  @Test
  void create_negative() {
    var expected = tenantDescriptor();
    when(okapiClient.createTenant(expected, OKAPI_AUTH_TOKEN)).thenThrow(FeignException.class);
    when(httpServletRequest.getHeader(TOKEN)).thenReturn(OKAPI_AUTH_TOKEN);

    var tenant = tenant();
    assertThatThrownBy(() -> okapiService.createTenant(tenant))
      .isInstanceOf(OkapiRequestException.class)
      .hasMessage("Failed to create tenant")
      .hasCauseInstanceOf(FeignException.class);
  }

  @Test
  void update_positive() {
    var expected = tenantDescriptor();
    when(okapiClient.updateTenant(TENANT_NAME, expected, OKAPI_AUTH_TOKEN)).thenReturn(expected);
    when(httpServletRequest.getHeader(TOKEN)).thenReturn(OKAPI_AUTH_TOKEN);

    var result = okapiService.updateTenantById(tenant());

    assertThat(result).isEqualTo(expected);
  }

  @Test
  void update_negative() {
    var expected = tenantDescriptor();
    when(httpServletRequest.getHeader(TOKEN)).thenReturn(null);
    when(okapiClient.updateTenant(TENANT_NAME, expected, null)).thenThrow(FeignException.class);

    var tenant = tenant();
    assertThatThrownBy(() -> okapiService.updateTenantById(tenant))
      .isInstanceOf(OkapiRequestException.class)
      .hasMessage("Failed to update tenant: " + TENANT_NAME)
      .hasCauseInstanceOf(FeignException.class);
  }

  @Test
  void delete_positive() {
    when(httpServletRequest.getHeader(TOKEN)).thenReturn(OKAPI_AUTH_TOKEN);
    when(okapiClient.getTenantById(TENANT_NAME, OKAPI_AUTH_TOKEN)).thenReturn(new TenantDescriptor());
    okapiService.deleteTenantById(TENANT_NAME);
    verify(okapiClient).deleteTenant(TENANT_NAME, OKAPI_AUTH_TOKEN);
  }

  @Test
  void delete_negative() {
    when(httpServletRequest.getHeader(TOKEN)).thenThrow(FeignException.class);
    assertThatThrownBy(() -> okapiService.deleteTenantById(TENANT_NAME))
      .isInstanceOf(OkapiRequestException.class)
      .hasMessage("Failed to delete tenant by id: " + TENANT_NAME)
      .hasCauseInstanceOf(FeignException.class);
  }
}
