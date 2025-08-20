package org.folio.tm.integration.keycloak.model;

import static java.lang.String.valueOf;
import static org.folio.tm.integration.keycloak.utils.KeycloakClientUtils.applyIfNotNull;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientAttributes {

  public static final String OIDC_CIBA_GRANT_ENABLED = "oidc.ciba.grant.enabled";
  public static final String OAUTH_2_DEVICE_AUTHORIZATION_GRANT_ENABLED = "oauth2.device.authorization.grant.enabled";
  public static final String CLIENT_SECRET_CREATION_TIME = "client.secret.creation.time";
  public static final String BACKCHANNEL_LOGOUT_SESSION_REQUIRED = "backchannel.logout.session.required";
  public static final String BACKCHANNEL_LOGOUT_REVOKE_OFFLINE_TOKENS = "backchannel.logout.revoke.offline.tokens";
  public static final String CLIENT_USE_LIGHTWEIGHT_ACCESS_TOKEN_ENABLED =
    "client.use.lightweight.access.token.enabled";
  public static final String ACCESS_TOKEN_LIFESPAN = "access.token.lifespan";
  public static final String USE_REFRESH_TOKENS = "use.refresh.tokens";

  @JsonProperty(OIDC_CIBA_GRANT_ENABLED)
  private boolean oidcCibaGrantEnabled;

  @JsonProperty(OAUTH_2_DEVICE_AUTHORIZATION_GRANT_ENABLED)
  private boolean oauth2DeviceAuthGrantEnabled;

  @JsonProperty(CLIENT_SECRET_CREATION_TIME)
  private long clientSecretCreationTime;

  @JsonProperty(BACKCHANNEL_LOGOUT_SESSION_REQUIRED)
  private boolean backChannelLogoutSessionRequired;

  @JsonProperty(BACKCHANNEL_LOGOUT_REVOKE_OFFLINE_TOKENS)
  private boolean backChannelLogoutRevokeOfflineTokens;

  @JsonProperty(CLIENT_USE_LIGHTWEIGHT_ACCESS_TOKEN_ENABLED)
  private boolean clientUseLightweightAccessTokenEnabled;

  @JsonProperty(ACCESS_TOKEN_LIFESPAN)
  private Long accessTokenLifeSpan;

  @JsonProperty(USE_REFRESH_TOKENS)
  private Boolean useRefreshTokens;

  public static ClientAttributes defaultValue() {
    var secretTimestamp = Instant.now().getEpochSecond();
    return new ClientAttributes(false, false, secretTimestamp, true, false, true, null, null);
  }

  public static ClientAttributes of(Long accessTokenLifespan, Boolean useRefreshTokens) {
    var secretTimestamp = Instant.now().getEpochSecond();
    return new ClientAttributes(false, false, secretTimestamp,
      true, false, true,  accessTokenLifespan, useRefreshTokens);
  }

  public Map<String, String> asMap() {
    var clientAttributes = new HashMap<String, String>();
    clientAttributes.put(OIDC_CIBA_GRANT_ENABLED, valueOf(oidcCibaGrantEnabled));
    clientAttributes.put(OAUTH_2_DEVICE_AUTHORIZATION_GRANT_ENABLED, valueOf(oauth2DeviceAuthGrantEnabled));
    clientAttributes.put(CLIENT_SECRET_CREATION_TIME, valueOf(clientSecretCreationTime));
    clientAttributes.put(BACKCHANNEL_LOGOUT_SESSION_REQUIRED, valueOf(backChannelLogoutSessionRequired));
    clientAttributes.put(BACKCHANNEL_LOGOUT_REVOKE_OFFLINE_TOKENS, valueOf(backChannelLogoutRevokeOfflineTokens));
    clientAttributes.put(CLIENT_USE_LIGHTWEIGHT_ACCESS_TOKEN_ENABLED, valueOf(clientUseLightweightAccessTokenEnabled));

    applyIfNotNull(accessTokenLifeSpan, value -> clientAttributes.put(ACCESS_TOKEN_LIFESPAN, valueOf(value)));
    applyIfNotNull(useRefreshTokens, value -> clientAttributes.put(USE_REFRESH_TOKENS, valueOf(value)));

    return clientAttributes;
  }
}
