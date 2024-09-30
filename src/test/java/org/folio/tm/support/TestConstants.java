package org.folio.tm.support;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static org.folio.tm.domain.dto.TenantType.DEFAULT;
import static org.folio.tm.integration.keycloak.model.Client.OPENID_CONNECT_PROTOCOL;
import static org.folio.tm.integration.keycloak.model.ProtocolMapper.USER_ATTRIBUTE_MAPPER_TYPE;
import static org.folio.tm.integration.keycloak.model.ProtocolMapperConfig.forUserAttribute;

import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.folio.tm.domain.dto.Tenant;
import org.folio.tm.domain.dto.TenantAttribute;
import org.folio.tm.domain.dto.TenantAttributes;
import org.folio.tm.integration.keycloak.model.ProtocolMapperConfig;
import org.folio.tm.integration.keycloak.model.Realm;
import org.folio.tm.integration.okapi.model.TenantDescriptor;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class TestConstants {

  /**
   * Sample JWT that will expire in 2030 year for test_tenant with randomly generated user id.
   */
  public static final String AUTH_TOKEN = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJmb2xpbyIsInVzZXJfaWQiOiJlNmQyODVlOS03M"
    + "mVkLTQxYTQtOGIzYi01Y2VlNGNiYzg0MjUiLCJ0eXBlIjoiYWNjZXNzIiwiZXhwIjoxODkzNTAyODAwLCJpYXQiOjE3MjUzMDM2ODgsInRlbmFud"
    + "CI6InRlc3RfdGVuYW50In0.SdtIQTrn7_XPnyi75Ai9bBkCWa8eQ69U6VAidCCRFFQ";

  public static final UUID TENANT_ID = UUID.fromString("4b83457f-5309-4648-a89a-62917ac3c63d");
  public static final String TENANT_NAME = "tenant1";
  public static final String TENANT_DESCRIPTION = "test tenant1";
  public static final String REALM_NAME = TENANT_NAME;
  public static final String SECRET = "secret";
  public static final String CLIENT_ID = "test client id";

  public static Tenant tenant() {
    return new Tenant().id(TENANT_ID).name(TENANT_NAME).description(TENANT_DESCRIPTION).type(DEFAULT);
  }

  public static TenantAttributes tenantAttributes(TenantAttribute ... tenantAttributes) {
    return new TenantAttributes().tenantAttributes(List.of(tenantAttributes));
  }

  public static TenantDescriptor tenantDescriptor() {
    return tenantDescriptor(TENANT_NAME);
  }

  public static TenantDescriptor tenantDescriptor(String id) {
    return new TenantDescriptor(id, TENANT_NAME, TENANT_DESCRIPTION);
  }

  public static Realm realmDescriptor() {
    return new Realm(TENANT_ID.toString(), true, TENANT_NAME, TRUE, FALSE, emptyList(), emptyMap());
  }

  public static ProtocolMapperRepresentation usernameProtocolMapper() {
    var usernameMapper = new ProtocolMapperRepresentation();
    usernameMapper.setProtocolMapper("oidc-usermodel-property-mapper");
    usernameMapper.setProtocol("openid-connect");
    usernameMapper.setName("username");
    usernameMapper.setConfig(forUserAttribute("username", "sub").asMap());

    return usernameMapper;
  }

  public static ProtocolMapperRepresentation userIdProtocolMapper() {
    var usernameMapper = new ProtocolMapperRepresentation();
    usernameMapper.setName("user_id mapper");
    usernameMapper.setProtocolMapper(USER_ATTRIBUTE_MAPPER_TYPE);
    usernameMapper.setProtocol(OPENID_CONNECT_PROTOCOL);
    usernameMapper.setConfig(forUserAttribute("user_id", "user_id").asMap());

    return usernameMapper;
  }

  public static ProtocolMapperRepresentation protocolMapper(String mapperType,
    String mapperName, String userAttr, String claimName) {
    var protocolMapper = new ProtocolMapperRepresentation();
    protocolMapper.setProtocolMapper(mapperType);
    protocolMapper.setProtocol(OPENID_CONNECT_PROTOCOL);
    protocolMapper.setName(mapperName);
    protocolMapper.setConfig(ProtocolMapperConfig.forUserAttribute(userAttr, claimName).asMap());
    return protocolMapper;
  }
}
