package org.folio.tm.integration.keycloak.model;

import com.fasterxml.jackson.annotation.JsonProperty;
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
public class User implements Serializable {

  @Serial
  private static final long serialVersionUID = 204690406679107038L;

  private String id;
  @JsonProperty("username")
  private String userName;
  private long createdTimestamp;

  private boolean enabled;
  private boolean totp;
  private boolean emailVerified;
}
