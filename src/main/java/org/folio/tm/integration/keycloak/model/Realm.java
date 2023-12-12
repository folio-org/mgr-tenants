package org.folio.tm.integration.keycloak.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serial;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Realm implements Serializable {

  @Serial
  private static final long serialVersionUID = -6978449440846654835L;

  @JsonProperty("id")
  private String id;

  @JsonProperty("enabled")
  private Boolean enabled;

  @JsonProperty("realm")
  private String name;

  @JsonProperty("duplicateEmailsAllowed")
  private Boolean duplicateEmailsAllowed;

  @JsonProperty("loginWithEmailAllowed")
  private Boolean loginWithEmailAllowed;
}
