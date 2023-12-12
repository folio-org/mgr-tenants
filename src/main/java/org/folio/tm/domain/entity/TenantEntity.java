package org.folio.tm.domain.entity;

import io.hypersistence.utils.hibernate.type.basic.PostgreSQLEnumType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.folio.tm.domain.entity.base.Auditable;
import org.folio.tm.domain.entity.base.Identifiable;
import org.folio.tm.domain.model.TenantType;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.Type;

@Data
@Entity
@Table(name = "tenant")
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class TenantEntity extends Auditable implements Identifiable {

  @Id
  private UUID id;

  @Column(name = "name", nullable = false)
  private String name;

  @Column(name = "description")
  private String description;

  @Enumerated(EnumType.STRING)
  @Type(PostgreSQLEnumType.class)
  @Column(name = "type", columnDefinition = "tenant_type")
  private TenantType type;

  @OrderBy("key")
  @Fetch(FetchMode.SUBSELECT)
  @OneToMany(cascade = CascadeType.ALL,
    fetch = FetchType.EAGER,
    mappedBy = "tenant",
    orphanRemoval = true)
  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  private List<TenantAttributeEntity> attributes = new ArrayList<>();

  public static TenantEntity of(UUID id) {
    var entity = new TenantEntity();
    entity.id = id;
    return entity;
  }
}
