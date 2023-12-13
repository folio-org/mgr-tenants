package org.folio.tm.integration.keycloak.utils;

import static org.folio.tm.integration.keycloak.model.Client.OPENID_CONNECT_PROTOCOL;
import static org.folio.tm.integration.keycloak.model.ProtocolMapper.STRING_TYPE_LABEL;
import static org.folio.tm.integration.keycloak.model.ProtocolMapper.SUB_CLAIM;
import static org.folio.tm.integration.keycloak.model.ProtocolMapper.USER_ATTRIBUTE_MAPPER_TYPE;
import static org.folio.tm.integration.keycloak.model.ProtocolMapper.USER_PROPERTY_MAPPER_TYPE;

import java.time.Instant;
import java.util.List;
import lombok.experimental.UtilityClass;
import org.folio.tm.integration.keycloak.model.Client;
import org.folio.tm.integration.keycloak.model.ProtocolMapper;

@UtilityClass
public class KeycloakClientUtils {

  private static final String URIS = "/*";
  private static final String USERNAME_PROPERTY = "username";
  private static final String USER_ID_PROPERTY = "user_id";
  private static final String USER_ID_MAPPER_NAME = "user_id mapper";

  public static Client buildClient(String clientId, String clientSecret, String desc, List<ProtocolMapper> mappers,
                                   boolean authEnabled, boolean serviceAccountEnabled) {
    var attributes = buildClientAttributes();
    return Client.builder()
      .clientId(clientId)
      .name(clientId)
      .description(desc)
      .secret(clientSecret)
      .enabled(true)
      .authorizationServicesEnabled(authEnabled)
      .directAccessGrantsEnabled(true)
      .serviceAccountsEnabled(serviceAccountEnabled)
      .frontChannelLogout(true)
      .redirectUris(List.of(URIS))
      .webOrigins(List.of(URIS))
      .attributes(attributes)
      .protocolMappers(mappers)
      .build();
  }

  public static List<ProtocolMapper> folioUserTokenMappers() {
    var usernameMapper = protocolMapper(USER_PROPERTY_MAPPER_TYPE, USERNAME_PROPERTY, USERNAME_PROPERTY, SUB_CLAIM);
    var userIdMapper =
      protocolMapper(USER_ATTRIBUTE_MAPPER_TYPE, USER_ID_MAPPER_NAME, USER_ID_PROPERTY, USER_ID_PROPERTY);
    return List.of(usernameMapper, userIdMapper);
  }

  public static Client.Attribute buildClientAttributes() {
    return Client.Attribute.builder()
      .oauth2DeviceAuthGrantEnabled(false)
      .oidcCibaGrantEnabled(false)
      .clientSecretCreationTime(Instant.now().getEpochSecond())
      .backChannelLogoutSessionRequired(true)
      .backChannelLogoutRevokeOfflineTokens(false)
      .build();
  }

  public static ProtocolMapper protocolMapper(String mapperType, String mapperName, String userAttr, String claimName) {
    return ProtocolMapper.builder()
      .mapper(mapperType)
      .protocol(OPENID_CONNECT_PROTOCOL)
      .name(mapperName)
      .config(ProtocolMapper.Config.of(true, true, true, userAttr, claimName, STRING_TYPE_LABEL))
      .build();
  }
}
