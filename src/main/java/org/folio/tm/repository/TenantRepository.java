package org.folio.tm.repository;

import java.util.UUID;
import org.folio.spring.cql.JpaCqlRepository;
import org.folio.tm.domain.entity.TenantEntity;
import org.springframework.stereotype.Repository;

@Repository
public interface TenantRepository extends JpaCqlRepository<TenantEntity, UUID> {

  /**
   * Retrieves true if {@link TenantEntity} is found by name.
   *
   * @param name - tenant name as {@link String} object
   * @return true if {@link TenantEntity} is found by name, false - otherwise
   */
  boolean existsByName(String name);
}
