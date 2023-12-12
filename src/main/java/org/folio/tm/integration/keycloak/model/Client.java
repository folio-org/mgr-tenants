package org.folio.tm.integration.keycloak.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Client implements Serializable {

  public static final String CLIENT_SECRET_AUTH_TYPE = "client-secret";
  public static final String OPENID_CONNECT_PROTOCOL = "openid-connect";

  @Serial
  private static final long serialVersionUID = -5450019006221767712L;

  private String id;
  private String clientId;
  private String name;
  private String description;
  private boolean enabled;
  private String clientAuthenticatorType;
  private String secret;

  private boolean serviceAccountsEnabled;
  private boolean authorizationServicesEnabled;
  private boolean directAccessGrantsEnabled;
  private String protocol;
  @JsonProperty("frontchannelLogout")
  private boolean frontChannelLogout;

  private Attribute attributes;
  private List<String> redirectUris;
  private List<String> webOrigins;
  private List<ProtocolMapper> protocolMappers;

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class Attribute implements Serializable {

    @Serial
    private static final long serialVersionUID = -4112795806790832702L;
    @JsonProperty("oidc.ciba.grant.enabled")
    private boolean oidcCibaGrantEnabled;
    @JsonProperty("oauth2.device.authorization.grant.enabled")
    private boolean oauth2DeviceAuthGrantEnabled;
    @JsonProperty("client.secret.creation.time")
    private long clientSecretCreationTime;
    @JsonProperty("backchannel.logout.session.required")
    private boolean backChannelLogoutSessionRequired;
    @JsonProperty("backchannel.logout.revoke.offline.tokens")
    private boolean backChannelLogoutRevokeOfflineTokens;
    @JsonProperty("access.token.lifespan")
    private Long accessTokenLifeSpan;
    @JsonProperty("use.refresh.tokens")
    private Boolean useRefreshTokens;
  }
}
