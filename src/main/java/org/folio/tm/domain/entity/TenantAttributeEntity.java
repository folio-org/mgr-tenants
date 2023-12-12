package org.folio.tm.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.folio.tm.domain.entity.base.Auditable;
import org.folio.tm.domain.entity.base.Identifiable;
import org.springframework.data.domain.Sort;

@Data
@Entity
@Table(name = "tenant_attribute")
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class TenantAttributeEntity extends Auditable implements Identifiable {

  public static final Sort SORT_BY_KEY = Sort.by(Sort.Direction.ASC, "key");

  @Id
  private UUID id;

  @Column(name = "key", nullable = false)
  private String key;

  @Column(name = "value", nullable = false)
  private String value;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "tenant_id")
  private TenantEntity tenant;
}
