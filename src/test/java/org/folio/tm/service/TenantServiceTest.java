package org.folio.tm.service;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
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
import org.folio.tm.domain.dto.TenantAttributes;
import org.folio.tm.domain.dto.Tenants;
import org.folio.tm.domain.entity.TenantEntity;
import org.folio.tm.exception.RequestValidationException;
import org.folio.tm.integration.keycloak.KeycloakRealmManagementService;
import org.folio.tm.integration.okapi.OkapiService;
import org.folio.tm.mapper.TenantMapper;
import org.folio.tm.repository.TenantRepository;
import org.folio.tm.service.listeners.TenantEventsPublisher;
import org.folio.tm.support.TestConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;

@UnitTest
@ExtendWith(MockitoExtension.class)
class TenantServiceTest {

  @Mock private TenantMapper mapper;
  @Mock private OkapiService okapiService;
  @Spy  private KeycloakRealmManagementService keycloakService =
    new KeycloakRealmManagementService(null, null, null, null, null, null, null, null, null);
  @Mock private TenantRepository repository;
  @Mock private TenantAttributeService tenantAttributeService;
  @InjectMocks private TenantService tenantService;

  @BeforeEach
  void setUp() {
    var tenantEventsPublisher = new TenantEventsPublisher(
      List.of(okapiService, keycloakService.tenantServiceListener()));
    tenantService = new TenantService(mapper, repository, tenantAttributeService, tenantEventsPublisher);
  }

  @Test
  void getById_positive() {
    var expected = TestConstants.tenant();
    var entity = TenantEntity.of(TestConstants.TENANT_ID);

    when(repository.findById(TestConstants.TENANT_ID)).thenReturn(Optional.of(entity));
    when(mapper.toDto(entity)).thenReturn(expected);

    var result = tenantService.getTenantById(TestConstants.TENANT_ID);

    assertThat(result).isEqualTo(expected);
  }

  @Test
  void getById_negative() {
    var errorMessage = "Tenant is not found: id = " + TestConstants.TENANT_ID;

    when(repository.findById(TestConstants.TENANT_ID)).thenThrow(new EntityNotFoundException(errorMessage));

    assertThatThrownBy(() -> tenantService.getTenantById(TestConstants.TENANT_ID))
      .isInstanceOf(EntityNotFoundException.class)
      .hasMessage(errorMessage);
  }

  @Test
  void create_positive() {
    var expectedTenant = TestConstants.tenant();
    var expectedDescriptor = TestConstants.tenantDescriptor();
    var entity = tenantEntity();
    var tenantAttributes = new TenantAttributes().tenantAttributes(emptyList());

    when(mapper.toEntity(expectedTenant)).thenReturn(entity);
    when(repository.existsByName(TestConstants.TENANT_NAME)).thenReturn(false);
    when(repository.saveAndFlush(entity)).thenReturn(entity);
    doNothing().when(keycloakService).setupRealm(expectedTenant);
    doCallRealMethod().when(okapiService).onTenantCreate(expectedTenant);
    when(okapiService.createTenant(expectedTenant)).thenReturn(expectedDescriptor);
    when(tenantAttributeService.upsertAll(TestConstants.TENANT_ID, tenantAttributes)).thenReturn(tenantAttributes);
    when(mapper.toDto(entity, emptyList())).thenReturn(expectedTenant);

    var result = tenantService.createTenant(expectedTenant);

    assertThat(result).isEqualTo(expectedTenant);
  }

  @Test
  void create_negative_existsByName() {
    var expectedTenant = TestConstants.tenant();
    when(repository.existsByName(TestConstants.TENANT_NAME)).thenReturn(true);

    assertThatThrownBy(() -> tenantService.createTenant(expectedTenant))
      .isInstanceOf(RequestValidationException.class)
      .hasMessage("Tenant's name already taken: " + TestConstants.TENANT_NAME);
  }

  @Test
  void create_negative_keycloak_error() {
    var expectedTenant = TestConstants.tenant();
    var entity = tenantEntity();
    var tenantAttributes = new TenantAttributes().tenantAttributes(emptyList());

    when(repository.existsByName(TestConstants.TENANT_NAME)).thenReturn(false);
    when(mapper.toEntity(expectedTenant)).thenReturn(entity);
    when(repository.saveAndFlush(entity)).thenReturn(entity);
    when(tenantAttributeService.upsertAll(TestConstants.TENANT_ID, tenantAttributes)).thenReturn(tenantAttributes);
    doThrow(FeignException.Conflict.class).when(keycloakService).setupRealm(expectedTenant);

    assertThatThrownBy(() -> tenantService.createTenant(expectedTenant))
      .isInstanceOf(FeignException.Conflict.class);
  }

  @Test
  void create_negative_okapi_error() {
    var expectedTenant = TestConstants.tenant();
    var entity = tenantEntity();
    var tenantAttributes = new TenantAttributes().tenantAttributes(emptyList());

    when(mapper.toEntity(expectedTenant)).thenReturn(entity);
    when(repository.existsByName(TestConstants.TENANT_NAME)).thenReturn(false);
    when(repository.saveAndFlush(entity)).thenReturn(entity);
    doCallRealMethod().when(okapiService).onTenantCreate(expectedTenant);
    when(tenantAttributeService.upsertAll(TestConstants.TENANT_ID, tenantAttributes)).thenReturn(tenantAttributes);
    when(okapiService.createTenant(expectedTenant)).thenThrow(FeignException.Conflict.class);

    assertThatThrownBy(() -> tenantService.createTenant(expectedTenant))
      .isInstanceOf(FeignException.Conflict.class);
  }

  @Test
  void update_positive() {
    var expectedTenant = TestConstants.tenant();
    var expectedDescriptor = TestConstants.tenantDescriptor();
    var expectedRealm = TestConstants.realmDescriptor();
    var entity = tenantEntity();
    var tenantAttributes = new TenantAttributes().tenantAttributes(emptyList());

    when(mapper.toEntity(expectedTenant)).thenReturn(entity);
    when(repository.findById(TestConstants.TENANT_ID)).thenReturn(Optional.of(entity));
    when(repository.saveAndFlush(entity)).thenReturn(entity);
    doCallRealMethod().when(okapiService).onTenantUpdate(expectedTenant);
    when(okapiService.updateTenantById(expectedTenant)).thenReturn(expectedDescriptor);
    doReturn(expectedRealm).when(keycloakService).updateRealm(expectedTenant);
    when(tenantAttributeService.upsertAll(TestConstants.TENANT_ID, tenantAttributes)).thenReturn(tenantAttributes);
    when(mapper.toDto(entity, emptyList())).thenReturn(expectedTenant);

    var result = tenantService.updateTenantById(TestConstants.TENANT_ID, expectedTenant);

    assertThat(result).isEqualTo(expectedTenant);
  }

  @Test
  void update_negative_tenantNotFound() {
    var expectedTenant = TestConstants.tenant();
    var errorMessage = "Tenant is not found: id = " + TestConstants.TENANT_ID;

    when(repository.findById(TestConstants.TENANT_ID)).thenThrow(new EntityNotFoundException(errorMessage));

    assertThatThrownBy(() -> tenantService.updateTenantById(TestConstants.TENANT_ID, expectedTenant))
      .isInstanceOf(EntityNotFoundException.class)
      .hasMessage(errorMessage);
  }

  @Test
  void update_negative_realmNotFound() {
    var expectedTenant = TestConstants.tenant();
    var expectedDescriptor = TestConstants.tenantDescriptor();
    var entity = tenantEntity();
    var tenantAttributes = new TenantAttributes().tenantAttributes(emptyList());

    when(mapper.toEntity(expectedTenant)).thenReturn(entity);
    when(repository.findById(TestConstants.TENANT_ID)).thenReturn(Optional.of(entity));
    when(repository.saveAndFlush(entity)).thenReturn(entity);
    doCallRealMethod().when(okapiService).onTenantUpdate(expectedTenant);
    when(okapiService.updateTenantById(expectedTenant)).thenReturn(expectedDescriptor);
    doThrow(EntityNotFoundException.class).when(keycloakService).updateRealm(expectedTenant);
    when(tenantAttributeService.upsertAll(TestConstants.TENANT_ID, tenantAttributes)).thenReturn(tenantAttributes);

    assertThatThrownBy(() -> tenantService.updateTenantById(TestConstants.TENANT_ID, expectedTenant))
      .isInstanceOf(EntityNotFoundException.class);
  }

  @Test
  void update_negative_idDoesntMatch() {
    var expectedTenant = TestConstants.tenant();
    expectedTenant.setId(UUID.randomUUID());
    var entity = tenantEntity();

    when(repository.findById(TestConstants.TENANT_ID)).thenReturn(Optional.of(entity));

    assertThatThrownBy(() -> tenantService.updateTenantById(TestConstants.TENANT_ID, expectedTenant))
      .isInstanceOf(RequestValidationException.class)
      .hasMessage("Tenant id doesn't match to the one in the path");
  }

  @Test
  void update_negative_nameModified() {
    var expectedTenant = TestConstants.tenant();
    expectedTenant.setName("Modified");
    var entity = tenantEntity();

    when(repository.findById(TestConstants.TENANT_ID)).thenReturn(Optional.of(entity));

    assertThatThrownBy(() -> tenantService.updateTenantById(TestConstants.TENANT_ID, expectedTenant))
      .isInstanceOf(RequestValidationException.class)
      .hasMessage("Tenant name cannot be modified");
  }

  @Test
  void delete_positive() {
    var entity = tenantEntity();

    when(repository.findById(TestConstants.TENANT_ID)).thenReturn(Optional.of(entity));
    doNothing().when(keycloakService).destroyRealm(TestConstants.TENANT_NAME);

    tenantService.deleteTenantById(TestConstants.TENANT_ID);

    verify(repository).delete(entity);
    verify(okapiService).onTenantDelete(TestConstants.TENANT_NAME);
  }

  @Test
  void delete_positive_notFound() {
    var entity = tenantEntity();

    when(repository.findById(TestConstants.TENANT_ID)).thenReturn(Optional.empty());

    tenantService.deleteTenantById(TestConstants.TENANT_ID);

    verify(repository, never()).delete(entity);
    verify(okapiService, never()).deleteTenantById(TestConstants.TENANT_NAME);
    verify(keycloakService, never()).destroyRealm(TestConstants.TENANT_NAME);
  }

  @Test
  void getTenantsByQuery_positive() {
    var query = "cql.allRecords=1";
    var tenantEntities = new PageImpl<>(List.of(tenantEntity()));
    var expectedTenants = new Tenants().tenants(List.of(TestConstants.tenant())).totalRecords(1);

    when(repository.findByCql(query, OffsetRequest.of(10, 5))).thenReturn(tenantEntities);
    when(mapper.toDtoCollection(tenantEntities)).thenReturn(expectedTenants);

    var actual = tenantService.getTenantsByQuery(query, 10, 5);

    assertThat(actual).isEqualTo(expectedTenants);
  }

  @Test
  void getTenantsByQuery_positive_queryIsBlank() {
    var tenantEntities = new PageImpl<>(List.of(tenantEntity()));
    var expectedTenants = new Tenants().tenants(List.of(TestConstants.tenant())).totalRecords(1);

    when(repository.findAll(OffsetRequest.of(10, 5))).thenReturn(tenantEntities);
    when(mapper.toDtoCollection(tenantEntities)).thenReturn(expectedTenants);

    var actual = tenantService.getTenantsByQuery(null, 10, 5);

    assertThat(actual).isEqualTo(expectedTenants);
  }

  private static TenantEntity tenantEntity() {
    var entity = TenantEntity.of(TestConstants.TENANT_ID);
    entity.setName(TestConstants.TENANT_NAME);
    entity.setDescription(TestConstants.TENANT_DESCRIPTION);
    return entity;
  }
}
