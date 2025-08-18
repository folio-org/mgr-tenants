package org.folio.tm.integration.keycloak.model;

import static java.lang.String.valueOf;
import static org.folio.tm.integration.keycloak.utils.KeycloakClientUtils.applyIfNotNull;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.HashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProtocolMapperConfig {

  public static final String STRING_TYPE_LABEL = "String";
  public static final String ID_TOKEN_CLAIM = "id.token.claim";
  public static final String ACCESS_TOKEN_CLAIM = "access.token.claim";
  public static final String USERINFO_TOKEN_CLAIM = "userinfo.token.claim";
  public static final String LIGHTWEIGHT_CLAIM = "lightweight.claim";
  public static final String USER_ATTRIBUTE = "user.attribute";
  public static final String CLAIM_NAME = "claim.name";
  public static final String JSON_TYPE_LABEL = "jsonType.label";

  @JsonProperty(ID_TOKEN_CLAIM)
  private boolean idTokenClaim;

  @JsonProperty(ACCESS_TOKEN_CLAIM)
  private boolean accessTokenClaim;

  @JsonProperty(USERINFO_TOKEN_CLAIM)
  private boolean userInfoTokenClaim;

  @JsonProperty(LIGHTWEIGHT_CLAIM)
  private boolean lightweightClaim;

  @JsonProperty(USER_ATTRIBUTE)
  private String userAttribute;

  @JsonProperty(CLAIM_NAME)
  private String claimName;

  @JsonProperty(JSON_TYPE_LABEL)
  private String jsonTypeLabel;

  public static ProtocolMapperConfig defaultValue() {
    return new ProtocolMapperConfig(true, true, true, true, null, null, null);
  }

  public static ProtocolMapperConfig forUserAttribute(String userAttribute, String claimName) {
    return new ProtocolMapperConfig(true, true, true, true, userAttribute, claimName, STRING_TYPE_LABEL);
  }

  public Map<String, String> asMap() {
    var resultMap = new HashMap<String, String>();
    resultMap.put(ID_TOKEN_CLAIM, valueOf(idTokenClaim));
    resultMap.put(ACCESS_TOKEN_CLAIM, valueOf(accessTokenClaim));
    resultMap.put(USERINFO_TOKEN_CLAIM, valueOf(userInfoTokenClaim));
    resultMap.put(LIGHTWEIGHT_CLAIM, valueOf(lightweightClaim));

    applyIfNotNull(userAttribute, value -> resultMap.put(USER_ATTRIBUTE, value));
    applyIfNotNull(claimName, value -> resultMap.put(CLAIM_NAME, value));
    applyIfNotNull(jsonTypeLabel, value -> resultMap.put(JSON_TYPE_LABEL, value));

    return resultMap;
  }

  /**
   * Sets idTokenClaim for {@link ProtocolMapperConfig} and returns {@link ProtocolMapperConfig}.
   *
   * @return this {@link ProtocolMapperConfig} with new idTokenClaim value
   */
  public ProtocolMapperConfig idTokenClaim(boolean idTokenClaim) {
    this.idTokenClaim = idTokenClaim;
    return this;
  }

  /**
   * Sets accessTokenClaim for {@link ProtocolMapperConfig} and returns {@link ProtocolMapperConfig}.
   *
   * @return this {@link ProtocolMapperConfig} with new accessTokenClaim value
   */
  public ProtocolMapperConfig accessTokenClaim(boolean accessTokenClaim) {
    this.accessTokenClaim = accessTokenClaim;
    return this;
  }

  /**
   * Sets userInfoTokenClaim for {@link ProtocolMapperConfig} and returns {@link ProtocolMapperConfig}.
   *
   * @return this {@link ProtocolMapperConfig} with new userInfoTokenClaim value
   */
  public ProtocolMapperConfig userInfoTokenClaim(boolean userInfoTokenClaim) {
    this.userInfoTokenClaim = userInfoTokenClaim;
    return this;
  }

  /**
   * Sets userAttribute for {@link ProtocolMapperConfig} and returns {@link ProtocolMapperConfig}.
   *
   * @return this {@link ProtocolMapperConfig} with new userAttribute value
   */
  public ProtocolMapperConfig userAttribute(String userAttribute) {
    this.userAttribute = userAttribute;
    return this;
  }

  /**
   * Sets claimName for {@link ProtocolMapperConfig} and returns {@link ProtocolMapperConfig}.
   *
   * @return this {@link ProtocolMapperConfig} with new claimName value
   */
  public ProtocolMapperConfig claimName(String claimName) {
    this.claimName = claimName;
    return this;
  }

  /**
   * Sets jsonTypeLabel for {@link ProtocolMapperConfig} and returns {@link ProtocolMapperConfig}.
   *
   * @return this {@link ProtocolMapperConfig} with new jsonTypeLabel value
   */
  public ProtocolMapperConfig jsonTypeLabel(String jsonTypeLabel) {
    this.jsonTypeLabel = jsonTypeLabel;
    return this;
  }
}
