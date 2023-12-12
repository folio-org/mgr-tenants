package org.folio.tm.mapper;

import static org.apache.commons.collections4.ListUtils.emptyIfNull;

import java.util.List;
import org.folio.tm.domain.dto.Tenant;
import org.folio.tm.domain.dto.TenantAttribute;
import org.folio.tm.domain.dto.Tenants;
import org.folio.tm.domain.entity.TenantEntity;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.data.domain.Page;

@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR, uses = MappingMethods.class)
public interface TenantMapper {

  @Mapping(target = "attributes", ignore = true)
  TenantEntity toEntity(Tenant dto);

  List<TenantEntity> toEntities(Iterable<Tenant> dtos);

  @AuditableMapping
  Tenant toDto(TenantEntity entity);

  @AuditableMapping
  @Mapping(target = "attributes", source = "attributes")
  Tenant toDto(TenantEntity entity, List<TenantAttribute> attributes);

  List<Tenant> toDtos(Iterable<TenantEntity> entities);

  default Tenants toDtoCollection(Page<TenantEntity> pageable) {
    List<Tenant> dtos = emptyIfNull(toDtos(pageable));

    return new Tenants()
      .tenants(dtos)
      .totalRecords((int) pageable.getTotalElements());
  }
}
