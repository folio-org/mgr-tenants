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
public class ProtocolMapper implements Serializable {

  public static final String USER_PROPERTY_MAPPER_TYPE = "oidc-usermodel-property-mapper";
  public static final String USER_ATTRIBUTE_MAPPER_TYPE = "oidc-usermodel-attribute-mapper";
  public static final String SUB_MAPPER_TYPE = "oidc-sub-mapper";
  public static final String STRING_TYPE_LABEL = "String";
  public static final String SUB_CLAIM = "sub";

  @Serial
  private static final long serialVersionUID = -68733298302690087L;

  private String protocol;
  private Config config;
  private String name;

  @JsonProperty("protocolMapper")
  private String mapper;

  @Data
  @AllArgsConstructor(staticName = "of")
  public static class Config implements Serializable {

    @Serial
    private static final long serialVersionUID = 1485065406541950993L;
    @JsonProperty("id.token.claim")
    private boolean idTokenClaim;

    @JsonProperty("access.token.claim")
    private boolean accessTokenClaim;

    @JsonProperty("userinfo.token.claim")
    private boolean userinfoTokenClaim;

    @JsonProperty("user.attribute")
    private String userAttribute;

    @JsonProperty("claim.name")
    private String claimName;

    @JsonProperty("jsonType.label")
    private String jsonTypeLabel;
  }
}
