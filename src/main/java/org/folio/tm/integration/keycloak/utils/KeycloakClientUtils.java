package org.folio.tm.integration.keycloak.utils;

import static org.folio.tm.integration.keycloak.model.Client.OPENID_CONNECT_PROTOCOL;
import static org.folio.tm.integration.keycloak.model.ProtocolMapper.SUB_CLAIM;
import static org.folio.tm.integration.keycloak.model.ProtocolMapper.USER_ATTRIBUTE_MAPPER_TYPE;
import static org.folio.tm.integration.keycloak.model.ProtocolMapper.USER_PROPERTY_MAPPER_TYPE;
import static org.folio.tm.integration.keycloak.model.ProtocolMapperConfig.forUserAttribute;

import java.util.List;
import java.util.function.Consumer;
import lombok.experimental.UtilityClass;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;

@UtilityClass
public class KeycloakClientUtils {

  private static final String USERNAME_PROPERTY = "username";
  private static final String USER_ID_PROPERTY = "user_id";
  private static final String USER_ID_MAPPER_NAME = "user_id mapper";

  public static <T> void applyIfNotNull(T value, Consumer<T> valueConsumer) {
    if (value != null) {
      valueConsumer.accept(value);
    }
  }

  public static List<ProtocolMapperRepresentation> getFolioUserTokenMappers() {
    return List.of(buildUsernameProtocolMapper(), buildUserIdProtocolMapper());
  }

  private static ProtocolMapperRepresentation buildUsernameProtocolMapper() {
    var usernameMapper = new ProtocolMapperRepresentation();
    usernameMapper.setProtocolMapper(USER_PROPERTY_MAPPER_TYPE);
    usernameMapper.setProtocol(OPENID_CONNECT_PROTOCOL);
    usernameMapper.setName(USERNAME_PROPERTY);
    usernameMapper.setConfig(forUserAttribute(USERNAME_PROPERTY, SUB_CLAIM).asMap());

    return usernameMapper;
  }

  private static ProtocolMapperRepresentation buildUserIdProtocolMapper() {
    var usernameMapper = new ProtocolMapperRepresentation();
    usernameMapper.setName(USER_ID_MAPPER_NAME);
    usernameMapper.setProtocolMapper(USER_ATTRIBUTE_MAPPER_TYPE);
    usernameMapper.setProtocol(OPENID_CONNECT_PROTOCOL);
    usernameMapper.setConfig(forUserAttribute(USER_ID_PROPERTY, USER_ID_PROPERTY).asMap());

    return usernameMapper;
  }
}
