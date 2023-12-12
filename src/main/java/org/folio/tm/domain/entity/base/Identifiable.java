package org.folio.tm.domain.entity.base;

import java.util.UUID;
import org.springframework.lang.Nullable;

public interface Identifiable {

  /**
   * Returns identifiable object id.
   *
   * @return object id as {@link UUID} value
   */
  @Nullable
  UUID getId();

  /**
   * Sets object id.
   *
   * @param id - object id as {@link UUID} value
   */
  void setId(@Nullable UUID id);
}
