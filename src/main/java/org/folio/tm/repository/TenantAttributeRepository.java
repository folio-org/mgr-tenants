package org.folio.tm.repository;

import java.util.UUID;
import org.folio.spring.cql.JpaCqlRepository;
import org.folio.tm.domain.entity.TenantAttributeEntity;
import org.springframework.stereotype.Repository;

@Repository
public interface TenantAttributeRepository extends JpaCqlRepository<TenantAttributeEntity, UUID> {

}
