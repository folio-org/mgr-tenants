package org.folio.tm.service;

import static java.util.Collections.emptyList;
import static org.apache.commons.collections4.ListUtils.defaultIfNull;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.folio.tm.service.ServiceUtils.initId;

import jakarta.persistence.EntityNotFoundException;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.common.domain.model.OffsetRequest;
import org.folio.tm.domain.dto.Tenant;
import org.folio.tm.domain.dto.TenantAttributes;
import org.folio.tm.domain.dto.Tenants;
import org.folio.tm.domain.entity.TenantEntity;
import org.folio.tm.exception.RequestValidationException;
import org.folio.tm.integration.entitlements.TenantEntitlementsService;
import org.folio.tm.integration.kafka.KafkaService;
import org.folio.tm.mapper.TenantMapper;
import org.folio.tm.repository.TenantRepository;
import org.folio.tm.service.listeners.TenantEventsPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Log4j2
@Service
@RequiredArgsConstructor
@Transactional
public class TenantService {

  private final TenantMapper mapper;
  private final TenantRepository repository;
  private final TenantAttributeService tenantAttributeService;
  private final TenantEventsPublisher tenantEventsPublisher;
  private final KafkaService kafkaService;
  private final TenantEntitlementsService tenantEntitlementsService;

  public Tenant createTenant(Tenant tenant) {
    var name = tenant.getName();
    if (repository.existsByName(name)) {
      throw new RequestValidationException("Tenant's name already taken: " + name);
    }

    var tenantWithId = initId(tenant);
    var saved = repository.saveAndFlush(mapper.toEntity(tenantWithId));
    var attributes = new TenantAttributes().tenantAttributes(defaultIfNull(tenant.getAttributes(), emptyList()));
    var tenantAttributes = tenantAttributeService.upsertAll(tenantWithId.getId(), attributes).getTenantAttributes();

    tenantEventsPublisher.onTenantCreate(tenant);

    return mapper.toDto(saved, tenantAttributes);
  }

  @Transactional(readOnly = true)
  public Tenant getTenantById(UUID id) {
    var entity = getOne(id);
    return mapper.toDto(entity);
  }

  @Transactional(readOnly = true)
  public Tenants getTenantsByQuery(String query, Integer offset, Integer limit) {
    var offsetReq = OffsetRequest.of(offset, limit);

    var page = isBlank(query)
      ? repository.findAll(offsetReq)
      : repository.findByCql(query, offsetReq);

    return mapper.toDtoCollection(page);
  }

  public Tenant updateTenantById(UUID id, Tenant tenant) {
    var existing = getOne(id);
    var tenantId = tenant.getId();

    if (!Objects.equals(existing.getId(), tenantId)) {
      throw new RequestValidationException("Tenant id doesn't match to the one in the path", "id", tenantId);
    }
    if (!Objects.equals(existing.getName(), tenant.getName())) {
      throw new RequestValidationException("Tenant name cannot be modified", "name", tenant.getName());
    }
    if (!Objects.equals(existing.getSecure(), tenant.getSecure())) {
      throw new RequestValidationException("Secure field cannot be modified", "secure", tenant.getSecure());
    }

    var saved = repository.saveAndFlush(mapper.toEntity(tenant));

    var attributes = new TenantAttributes().tenantAttributes(defaultIfNull(tenant.getAttributes(), emptyList()));
    var tenantAttributes = tenantAttributeService.upsertAll(tenantId, attributes).getTenantAttributes();

    tenantEventsPublisher.onTenantUpdate(tenant);

    return mapper.toDto(saved, tenantAttributes);
  }

  public void deleteTenantById(UUID id, Boolean purgeKafkaTopics) {
    repository.findById(id).ifPresent(entity -> {
      checkEntitlementsBeforeDeletion(entity.getName(), entity.getId());
      performDeletion(entity, purgeKafkaTopics);
    });
  }

  private void checkEntitlementsBeforeDeletion(String tenantName, UUID tenantId) {
    log.debug("Checking for active entitlements before deleting tenant: {}", tenantName);
    if (tenantEntitlementsService.hasTenantEntitlements(tenantName, tenantId)) {
      log.warn("Cannot delete tenant '{}': tenant has active entitlements", tenantName);
      throw new RequestValidationException(
        "Cannot delete tenant with active entitlements. Please uninstall all applications before deleting.");
    }
  }

  private void performDeletion(TenantEntity entity, Boolean purgeKafkaTopics) {
    var tenantName = entity.getName();
    log.info("Deleting tenant: {}", tenantName);
    repository.delete(entity);
    tenantEventsPublisher.onTenantDelete(tenantName);
    kafkaService.deleteTopics(tenantName, purgeKafkaTopics);
  }

  private TenantEntity getOne(UUID id) {
    return repository.findById(id)
      .orElseThrow(() -> new EntityNotFoundException("Tenant is not found: id = " + id));
  }
}
