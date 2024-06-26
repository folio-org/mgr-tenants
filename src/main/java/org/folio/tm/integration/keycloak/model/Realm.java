package org.folio.tm.integration.keycloak.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
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

  private Map<String, List<Map<String, Object>>> components;

  /**
   * Sets id for {@link Realm} and returns {@link Realm}.
   *
   * @return this {@link Realm} with new id value
   */
  public Realm id(String id) {
    this.id = id;
    return this;
  }

  /**
   * Sets enabled for {@link Realm} and returns {@link Realm}.
   *
   * @return this {@link Realm} with new enabled value
   */
  public Realm enabled(Boolean enabled) {
    this.enabled = enabled;
    return this;
  }

  /**
   * Sets name for {@link Realm} and returns {@link Realm}.
   *
   * @return this {@link Realm} with new name value
   */
  public Realm name(String name) {
    this.name = name;
    return this;
  }

  /**
   * Sets duplicateEmailsAllowed for {@link Realm} and returns {@link Realm}.
   *
   * @return this {@link Realm} with new duplicateEmailsAllowed value
   */
  public Realm duplicateEmailsAllowed(Boolean duplicateEmailsAllowed) {
    this.duplicateEmailsAllowed = duplicateEmailsAllowed;
    return this;
  }

  /**
   * Sets loginWithEmailAllowed for {@link Realm} and returns {@link Realm}.
   *
   * @return this {@link Realm} with new loginWithEmailAllowed value
   */
  public Realm loginWithEmailAllowed(Boolean loginWithEmailAllowed) {
    this.loginWithEmailAllowed = loginWithEmailAllowed;
    return this;
  }

  /**
   * Sets components for {@link Realm} and returns {@link Realm}.
   *
   * @return this {@link Realm} with new components value
   */
  public Realm components(Map<String, List<Map<String, Object>>> components) {
    this.components = components;
    return this;
  }
}
