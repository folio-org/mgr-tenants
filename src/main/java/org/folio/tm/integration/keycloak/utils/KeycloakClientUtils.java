package org.folio.tm.integration.keycloak.utils;

import static org.folio.tm.integration.keycloak.model.Client.OPENID_CONNECT_PROTOCOL;
import static org.folio.tm.integration.keycloak.model.ProtocolMapper.AUDIENCE_MAPPER_TYPE;
import static org.folio.tm.integration.keycloak.model.ProtocolMapper.SUB_CLAIM;
import static org.folio.tm.integration.keycloak.model.ProtocolMapper.SUB_MAPPER_TYPE;
import static org.folio.tm.integration.keycloak.model.ProtocolMapper.USER_ATTRIBUTE_MAPPER_TYPE;
import static org.folio.tm.integration.keycloak.model.ProtocolMapper.USER_PROPERTY_MAPPER_TYPE;
import static org.folio.tm.integration.keycloak.model.ProtocolMapperConfig.ACCESS_TOKEN_CLAIM;
import static org.folio.tm.integration.keycloak.model.ProtocolMapperConfig.ID_TOKEN_CLAIM;
import static org.folio.tm.integration.keycloak.model.ProtocolMapperConfig.INCLUDED_CLIENT_AUDIENCE;
import static org.folio.tm.integration.keycloak.model.ProtocolMapperConfig.INTROSPECTION_TOKEN_CLAIM;
import static org.folio.tm.integration.keycloak.model.ProtocolMapperConfig.LIGHTWEIGHT_CLAIM;
import static org.folio.tm.integration.keycloak.model.ProtocolMapperConfig.USERINFO_TOKEN_CLAIM;
import static org.folio.tm.integration.keycloak.model.ProtocolMapperConfig.forUserAttribute;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import lombok.experimental.UtilityClass;
import org.folio.tm.integration.keycloak.model.ProtocolMapperConfig;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;

@UtilityClass
public class KeycloakClientUtils {

  private static final String USERNAME_PROPERTY = "username";
  private static final String USER_ID_PROPERTY = "user_id";
  private static final String USER_ID_MAPPER_NAME = "user_id mapper";
  private static final String SUBJECT_MAPPER_NAME = "Subject (sub)";
  private static final String AUDIENCE_MAPPER_NAME = "audience mapper";

  public static <T> void applyIfNotNull(T value, Consumer<T> valueConsumer) {
    if (value != null) {
      valueConsumer.accept(value);
    }
  }

  public static List<ProtocolMapperRepresentation> getFolioUserTokenMappers() {
    return List.of(getUsernameProtocolMapper(), getUserIdProtocolMapper(), getSubjectProtocolMapper());
  }

  public static ProtocolMapperRepresentation getSubjectProtocolMapper() {
    var subjectMapper = new ProtocolMapperRepresentation();
    subjectMapper.setName(SUBJECT_MAPPER_NAME);
    subjectMapper.setProtocolMapper(SUB_MAPPER_TYPE);
    subjectMapper.setProtocol(OPENID_CONNECT_PROTOCOL);
    subjectMapper.setConfig(ProtocolMapperConfig.defaultValue().asMap());

    return subjectMapper;
  }

  /**
   * Builds an audience protocol mapper adding the given client id to the 'aud' claim of issued tokens.
   *
   * <p>Required by Keycloak 26.6+ token introspection, which validates that the introspecting client is present
   * in the token audience. The 'lightweight.claim' option keeps the claim in lightweight access tokens.</p>
   *
   * @param clientId - client identifier to include as audience
   * @return audience {@link ProtocolMapperRepresentation} for the given client id
   */
  public static ProtocolMapperRepresentation getAudienceProtocolMapper(String clientId) {
    var audienceMapper = new ProtocolMapperRepresentation();
    audienceMapper.setName(AUDIENCE_MAPPER_NAME);
    audienceMapper.setProtocolMapper(AUDIENCE_MAPPER_TYPE);
    audienceMapper.setProtocol(OPENID_CONNECT_PROTOCOL);
    audienceMapper.setConfig(Map.of(
      INCLUDED_CLIENT_AUDIENCE, clientId,
      ID_TOKEN_CLAIM, "false",
      ACCESS_TOKEN_CLAIM, "true",
      USERINFO_TOKEN_CLAIM, "false",
      LIGHTWEIGHT_CLAIM, "true",
      INTROSPECTION_TOKEN_CLAIM, "true"));

    return audienceMapper;
  }

  private static ProtocolMapperRepresentation getUsernameProtocolMapper() {
    var usernameMapper = new ProtocolMapperRepresentation();
    usernameMapper.setProtocolMapper(USER_PROPERTY_MAPPER_TYPE);
    usernameMapper.setProtocol(OPENID_CONNECT_PROTOCOL);
    usernameMapper.setName(USERNAME_PROPERTY);
    usernameMapper.setConfig(forUserAttribute(USERNAME_PROPERTY, SUB_CLAIM).asMap());

    return usernameMapper;
  }

  private static ProtocolMapperRepresentation getUserIdProtocolMapper() {
    var usernameMapper = new ProtocolMapperRepresentation();
    usernameMapper.setName(USER_ID_MAPPER_NAME);
    usernameMapper.setProtocolMapper(USER_ATTRIBUTE_MAPPER_TYPE);
    usernameMapper.setProtocol(OPENID_CONNECT_PROTOCOL);
    usernameMapper.setConfig(forUserAttribute(USER_ID_PROPERTY, USER_ID_PROPERTY).asMap());

    return usernameMapper;
  }
}
