package org.folio.tm.service;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.folio.tm.domain.entity.TenantAttributeEntity.SORT_BY_KEY;
import static org.folio.tm.service.ServiceUtils.example;
import static org.folio.tm.service.ServiceUtils.initId;
import static org.folio.tm.service.ServiceUtils.mergeAndSave;
import static org.folio.tm.service.ServiceUtils.setId;

import jakarta.persistence.EntityNotFoundException;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.common.domain.model.OffsetRequest;
import org.folio.tm.domain.dto.TenantAttribute;
import org.folio.tm.domain.dto.TenantAttributes;
import org.folio.tm.domain.entity.TenantAttributeEntity;
import org.folio.tm.domain.entity.TenantEntity;
import org.folio.tm.exception.RequestValidationException;
import org.folio.tm.mapper.TenantAttributeMapper;
import org.folio.tm.repository.TenantAttributeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Log4j2
@Service
@RequiredArgsConstructor
@Transactional
public class TenantAttributeService {

  private final TenantAttributeRepository repository;
  private final TenantAttributeMapper mapper;

  @Transactional(readOnly = true)
  public TenantAttributes getAll(UUID tenantId, String query, int offset, int limit) {
    var offsetReq = OffsetRequest.of(offset, limit, SORT_BY_KEY);

    var page = isBlank(query)
      ? repository.findAll(example(TenantAttributeEntity::new, setTenantId(tenantId)), offsetReq)
      : repository.findByCql(query + " and tenant.id == " + tenantId, offsetReq);

    return mapper.toDtoCollection(page);
  }

  public TenantAttributes upsertAll(UUID tenantId, TenantAttributes dtos) {
    var stored = repository.findAll(example(TenantAttributeEntity::new, setTenantId(tenantId)));

    var incoming = mapper.toEntities(dtos.getTenantAttributes());
    incoming.forEach(setTenantId(tenantId).andThen(initId()));

    var saved = mergeAndSave(incoming, stored, repository, this::copyData);

    return mapper.toDtoCollection(saved);
  }

  @Transactional(readOnly = true)
  public TenantAttribute get(UUID tenantId, UUID id) {
    var ta = getOne(tenantId, id);

    return mapper.toDto(ta);
  }

  public TenantAttribute update(UUID tenantId, UUID id, TenantAttribute dto) {
    var existing = getOne(tenantId, id);

    UUID attrId = dto.getId();
    if (!Objects.equals(existing.getId(), attrId)) {
      throw new RequestValidationException("Tenant attribute id doesn't match to the one in the path", "id", attrId);
    }

    existing.setKey(dto.getKey());
    existing.setValue(dto.getValue());

    var updated = repository.saveAndFlush(existing);

    return mapper.toDto(updated);
  }

  public void delete(UUID tenantId, UUID id) {
    findOne(tenantId, id).ifPresent(repository::delete);
  }

  private TenantAttributeEntity getOne(UUID tenantId, UUID id) {
    return findOne(tenantId, id)
      .orElseThrow(() -> new EntityNotFoundException("Tenant attribute is not found: id = " + id
        + ", tenantId = " + tenantId));
  }

  private Optional<TenantAttributeEntity> findOne(UUID tenantId, UUID id) {
    return repository.findOne(example(TenantAttributeEntity::new, setId(id), setTenantId(tenantId)));
  }

  private void copyData(TenantAttributeEntity from, TenantAttributeEntity to) {
    to.setKey(from.getKey());
    to.setValue(from.getValue());
  }

  private static Consumer<TenantAttributeEntity> setTenantId(UUID tenantId) {
    return ta -> ta.setTenant(TenantEntity.of(tenantId));
  }
}
