package org.folio.tm.mapper;

import static java.util.Comparator.comparing;
import static org.apache.commons.collections4.ListUtils.emptyIfNull;

import java.util.List;
import org.folio.tm.domain.dto.TenantAttribute;
import org.folio.tm.domain.dto.TenantAttributes;
import org.folio.tm.domain.entity.TenantAttributeEntity;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.springframework.data.domain.Page;

@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR, uses = MappingMethods.class)
public interface TenantAttributeMapper {

  TenantAttributeEntity toEntity(TenantAttribute dto);

  List<TenantAttributeEntity> toEntities(Iterable<TenantAttribute> dtos);

  @AuditableMapping
  TenantAttribute toDto(TenantAttributeEntity entity);

  List<TenantAttribute> toDtos(Iterable<TenantAttributeEntity> entities);

  default TenantAttributes toDtoCollection(Page<TenantAttributeEntity> pageable) {
    List<TenantAttribute> dtos = emptyIfNull(toDtos(pageable));

    return new TenantAttributes()
      .tenantAttributes(dtos)
      .totalRecords((int) pageable.getTotalElements());
  }

  default TenantAttributes toDtoCollection(Iterable<TenantAttributeEntity> entities) {
    var dtos = emptyIfNull(toDtos(entities));
    dtos.sort(comparing(TenantAttribute::getKey));

    return new TenantAttributes()
      .tenantAttributes(dtos)
      .totalRecords(dtos.size());
  }
}
