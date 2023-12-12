package org.folio.tm.integration.keycloak.model;

import java.io.Serial;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Role implements Serializable {

  @Serial
  private static final long serialVersionUID = 185362513440168750L;

  private String id;
  private String name;
  private String description;
  private boolean composite;
  private boolean clientRole;
  private String containerId;
}
