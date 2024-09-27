package org.folio.tm.service;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.folio.tm.support.TestConstants.TENANT_ID;
import static org.folio.tm.support.TestConstants.TENANT_NAME;
import static org.folio.tm.support.TestConstants.tenant;
import static org.folio.tm.support.TestConstants.tenantAttributes;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import feign.FeignException;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.folio.common.domain.model.OffsetRequest;
import org.folio.test.types.UnitTest;
import org.folio.tm.domain.dto.Tenants;
import org.folio.tm.domain.entity.TenantEntity;
import org.folio.tm.exception.RequestValidationException;
import org.folio.tm.mapper.TenantMapper;
import org.folio.tm.repository.TenantRepository;
import org.folio.tm.service.listeners.TenantEventsPublisher;
import org.folio.tm.support.TestConstants;
import org.folio.tm.support.TestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;

@UnitTest
@ExtendWith(MockitoExtension.class)
class TenantServiceTest {

  @InjectMocks private TenantService tenantService;

  @Mock private TenantMapper mapper;
  @Mock private TenantRepository repository;
  @Mock private TenantEventsPublisher tenantEventsPublisher;
  @Mock private TenantAttributeService tenantAttributeService;

  @AfterEach
  void tearDown() {
    TestUtils.verifyNoMoreInteractions(this);
  }

  @Test
  void getById_positive() {
    var expected = tenant();
    var entity = TenantEntity.of(TENANT_ID);

    when(repository.findById(TENANT_ID)).thenReturn(Optional.of(entity));
    when(mapper.toDto(entity)).thenReturn(expected);

    var result = tenantService.getTenantById(TENANT_ID);

    assertThat(result).isEqualTo(expected);
  }

  @Test
  void getById_negative() {
    var errorMessage = "Tenant is not found: id = " + TENANT_ID;

    when(repository.findById(TENANT_ID)).thenThrow(new EntityNotFoundException(errorMessage));

    assertThatThrownBy(() -> tenantService.getTenantById(TENANT_ID))
      .isInstanceOf(EntityNotFoundException.class)
      .hasMessage(errorMessage);
  }

  @Test
  void create_positive() {
    var entity = tenantEntity();
    var expectedTenant = tenant();

    when(mapper.toEntity(expectedTenant)).thenReturn(entity);
    when(repository.existsByName(TestConstants.TENANT_NAME)).thenReturn(false);
    when(repository.saveAndFlush(entity)).thenReturn(entity);
    when(tenantAttributeService.upsertAll(TENANT_ID, tenantAttributes())).thenReturn(tenantAttributes());
    when(mapper.toDto(entity, emptyList())).thenReturn(expectedTenant);

    var result = tenantService.createTenant(expectedTenant);

    assertThat(result).isEqualTo(expectedTenant);
    verify(tenantEventsPublisher).onTenantCreate(expectedTenant);
  }

  @Test
  void create_negative_existsByName() {
    when(repository.existsByName(TestConstants.TENANT_NAME)).thenReturn(true);

    var expectedTenant = tenant();
    assertThatThrownBy(() -> tenantService.createTenant(expectedTenant))
      .isInstanceOf(RequestValidationException.class)
      .hasMessage("Tenant's name already taken: " + TestConstants.TENANT_NAME);
  }

  @Test
  void create_negative_keycloakError() {
    var expectedTenant = tenant();
    var entity = tenantEntity();

    when(repository.existsByName(TestConstants.TENANT_NAME)).thenReturn(false);
    when(mapper.toEntity(expectedTenant)).thenReturn(entity);
    when(repository.saveAndFlush(entity)).thenReturn(entity);
    when(tenantAttributeService.upsertAll(TENANT_ID, tenantAttributes())).thenReturn(tenantAttributes());
    doThrow(FeignException.Conflict.class).when(tenantEventsPublisher).onTenantCreate(expectedTenant);

    assertThatThrownBy(() -> tenantService.createTenant(expectedTenant))
      .isInstanceOf(FeignException.Conflict.class);
  }

  @Test
  void create_negative_okapiError() {
    var expectedTenant = tenant();
    var entity = tenantEntity();

    when(mapper.toEntity(expectedTenant)).thenReturn(entity);
    when(repository.existsByName(TestConstants.TENANT_NAME)).thenReturn(false);
    when(repository.saveAndFlush(entity)).thenReturn(entity);
    when(tenantAttributeService.upsertAll(TENANT_ID, tenantAttributes())).thenReturn(tenantAttributes());
    doThrow(FeignException.Conflict.class).when(tenantEventsPublisher).onTenantCreate(expectedTenant);

    assertThatThrownBy(() -> tenantService.createTenant(expectedTenant))
      .isInstanceOf(FeignException.Conflict.class);
  }

  @Test
  void update_positive() {
    var entity = tenantEntity();
    var expectedTenant = tenant();

    when(mapper.toEntity(expectedTenant)).thenReturn(entity);
    when(repository.findById(TENANT_ID)).thenReturn(Optional.of(entity));
    when(repository.saveAndFlush(entity)).thenReturn(entity);
    when(tenantAttributeService.upsertAll(TENANT_ID, tenantAttributes())).thenReturn(tenantAttributes());
    when(mapper.toDto(entity, emptyList())).thenReturn(expectedTenant);

    var result = tenantService.updateTenantById(TENANT_ID, expectedTenant);

    assertThat(result).isEqualTo(expectedTenant);
    verify(tenantEventsPublisher).onTenantUpdate(expectedTenant);
  }

  @Test
  void update_negative_tenantNotFound() {
    var expectedTenant = tenant();
    var errorMessage = "Tenant is not found: id = " + TENANT_ID;

    when(repository.findById(TENANT_ID)).thenThrow(new EntityNotFoundException(errorMessage));

    assertThatThrownBy(() -> tenantService.updateTenantById(TENANT_ID, expectedTenant))
      .isInstanceOf(EntityNotFoundException.class)
      .hasMessage(errorMessage);
  }

  @Test
  void update_negative_realmNotFound() {
    var expectedTenant = tenant();
    var entity = tenantEntity();

    when(mapper.toEntity(expectedTenant)).thenReturn(entity);
    when(repository.findById(TENANT_ID)).thenReturn(Optional.of(entity));
    when(repository.saveAndFlush(entity)).thenReturn(entity);
    doThrow(EntityNotFoundException.class).when(tenantEventsPublisher).onTenantUpdate(expectedTenant);
    when(tenantAttributeService.upsertAll(TENANT_ID, tenantAttributes())).thenReturn(tenantAttributes());

    assertThatThrownBy(() -> tenantService.updateTenantById(TENANT_ID, expectedTenant))
      .isInstanceOf(EntityNotFoundException.class);
  }

  @Test
  void update_negative_idDoesntMatch() {
    var expectedTenant = tenant();
    expectedTenant.setId(UUID.randomUUID());
    var entity = tenantEntity();

    when(repository.findById(TENANT_ID)).thenReturn(Optional.of(entity));

    assertThatThrownBy(() -> tenantService.updateTenantById(TENANT_ID, expectedTenant))
      .isInstanceOf(RequestValidationException.class)
      .hasMessage("Tenant id doesn't match to the one in the path");
  }

  @Test
  void update_negative_nameModified() {
    var expectedTenant = tenant();
    expectedTenant.setName("Modified");
    var entity = tenantEntity();

    when(repository.findById(TENANT_ID)).thenReturn(Optional.of(entity));

    assertThatThrownBy(() -> tenantService.updateTenantById(TENANT_ID, expectedTenant))
      .isInstanceOf(RequestValidationException.class)
      .hasMessage("Tenant name cannot be modified");
    verify(tenantEventsPublisher, never()).onTenantUpdate(any());
  }

  @Test
  void delete_positive() {
    var entity = tenantEntity();
    when(repository.findById(TENANT_ID)).thenReturn(Optional.of(entity));

    tenantService.deleteTenantById(TENANT_ID);

    verify(repository).delete(entity);
    verify(tenantEventsPublisher).onTenantDelete(TENANT_NAME);
  }

  @Test
  void delete_positive_notFound() {
    var entity = tenantEntity();

    when(repository.findById(TENANT_ID)).thenReturn(Optional.empty());

    tenantService.deleteTenantById(TENANT_ID);

    verify(repository, never()).delete(entity);
    verify(tenantEventsPublisher, never()).onTenantDelete(anyString());
  }

  @Test
  void getTenantsByQuery_positive() {
    var query = "cql.allRecords=1";
    var tenantEntities = new PageImpl<>(List.of(tenantEntity()));
    var expectedTenants = new Tenants().tenants(List.of(tenant())).totalRecords(1);

    when(repository.findByCql(query, OffsetRequest.of(10, 5))).thenReturn(tenantEntities);
    when(mapper.toDtoCollection(tenantEntities)).thenReturn(expectedTenants);

    var actual = tenantService.getTenantsByQuery(query, 10, 5);

    assertThat(actual).isEqualTo(expectedTenants);
  }

  @Test
  void getTenantsByQuery_positive_queryIsBlank() {
    var tenantEntities = new PageImpl<>(List.of(tenantEntity()));
    var expectedTenants = new Tenants().tenants(List.of(tenant())).totalRecords(1);

    when(repository.findAll(OffsetRequest.of(10, 5))).thenReturn(tenantEntities);
    when(mapper.toDtoCollection(tenantEntities)).thenReturn(expectedTenants);

    var actual = tenantService.getTenantsByQuery(null, 10, 5);

    assertThat(actual).isEqualTo(expectedTenants);
  }

  private static TenantEntity tenantEntity() {
    var entity = TenantEntity.of(TENANT_ID);
    entity.setName(TestConstants.TENANT_NAME);
    entity.setDescription(TestConstants.TENANT_DESCRIPTION);
    return entity;
  }
}
