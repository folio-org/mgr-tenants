package org.folio.tm.support;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.util.Collections.emptyMap;
import static org.folio.tm.domain.dto.TenantType.DEFAULT;
import static org.folio.tm.integration.keycloak.model.AuthorizationRolePolicy.RolePolicy;
import static org.folio.tm.integration.keycloak.model.Client.OPENID_CONNECT_PROTOCOL;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.folio.security.integration.keycloak.model.TokenResponse;
import org.folio.tm.domain.dto.Tenant;
import org.folio.tm.integration.keycloak.model.AuthorizationClientPolicy;
import org.folio.tm.integration.keycloak.model.AuthorizationPermission;
import org.folio.tm.integration.keycloak.model.AuthorizationRolePolicy;
import org.folio.tm.integration.keycloak.model.AuthorizationScope;
import org.folio.tm.integration.keycloak.model.Client;
import org.folio.tm.integration.keycloak.model.ProtocolMapper;
import org.folio.tm.integration.keycloak.model.ProtocolMapperType;
import org.folio.tm.integration.keycloak.model.Realm;
import org.folio.tm.integration.keycloak.model.Role;
import org.folio.tm.integration.keycloak.model.ServerInfo;
import org.folio.tm.integration.keycloak.model.User;
import org.folio.tm.integration.keycloak.model.UserManagementPermission;
import org.folio.tm.integration.keycloak.model.UserManagementPermission.ScopePermission;
import org.folio.tm.integration.okapi.model.TenantDescriptor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class TestConstants {

  public static final String OKAPI_AUTH_TOKEN = "X-Okapi-Token test value";
  public static final String AUTH_TOKEN = "dGVzdC1hdXRoLnRva2Vu";
  public static final UUID TENANT_ID = UUID.fromString("4b83457f-5309-4648-a89a-62917ac3c63d");
  public static final String TENANT_NAME = "tenant1";
  public static final String TENANT_DESCRIPTION = "test tenant1";
  public static final String TOKEN_CACHE = "token";
  public static final String ROLE_NAME = "System";
  public static final String REALM_NAME = TENANT_NAME;
  public static final String ROLE_ID = "e6a9d6e7-f9cd-4a7b-8cdf-f06c6f0f14e2";
  public static final String USER_ID = "4606c293-a61a-453e-a995-a0d2c1f9556d";
  public static final String USERNAME = "test username";
  public static final String POLICY_ID = "policy id";
  public static final String POLICY_NAME = "System role policy";
  public static final String ROLE_POLICY_TYPE = "role";
  public static final String SECRET = "secret";
  public static final String PERMISSION_NAME = "System role permission";
  public static final String CLIENT_ID = "test client id";
  public static final String SCOPE_GET = "GET";
  public static final String M2M_DESC = "Client for module-to-module communication";
  public static final String LOGIN_DESC = "Client for login operations";
  public static final String ROLE_DESC = "System role for module-to-module communication";
  public static final String ROLE_USER = "USER";
  public static final String IMPERSONATE_CLIENT = "impersonation-client";
  public static final String IMPERSONATE_POLICY = "impersonation-policy";
  public static final String IMPERSONATE_POLICY_ID = "impersonation-policy id";
  public static final String IMPERSONATE_SCOPE_ID = "fac5bcac-3345-46cb-bf65-432e67c18c17";
  public static final String CLIENT_POLICY_TYPE = "client";
  public static final String REALM_MANAGEMENT_CLIENT = "realm-management";
  public static final String REALM_MANAGEMENT_CLIENT_ID = "realm-management id";
  public static final String ADMIN_IMPERSONATING_PERMISSION = "admin-impersonating.permission.users";
  public static final String PASSWORD_RESET_ACTION_MAPPER = "Password reset action mapper";
  public static final String PASSWORD_RESET_CLIENT = "password-reset-client";
  private static final String BEARER = "Bearer";
  private static final String URIS = "/*";

  public static Tenant tenant() {
    return new Tenant().id(TENANT_ID).name(TENANT_NAME).description(TENANT_DESCRIPTION).type(DEFAULT);
  }

  public static TenantDescriptor tenantDescriptor() {
    return tenantDescriptor(TENANT_NAME);
  }

  public static TenantDescriptor tenantDescriptor(String id) {
    return new TenantDescriptor(id, TENANT_NAME, TENANT_DESCRIPTION);
  }

  public static Realm realmDescriptor() {
    return new Realm(TENANT_ID.toString(), true, TENANT_NAME, TRUE, FALSE, emptyMap());
  }

  public static TokenResponse tokenResponse() {
    var token = new TokenResponse();
    token.setAccessToken(AUTH_TOKEN);
    token.setExpiresIn(800L);
    token.setTokenType(BEARER);
    return token;
  }

  public static String getAccessToken() {
    var token = tokenResponse();
    return token.getTokenType() + " " + token.getAccessToken();
  }

  public static Role roleDescriptor() {
    return Role.builder()
      .name(ROLE_NAME)
      .description(ROLE_DESC)
      .build();
  }

  public static Role roleDescriptor(String id) {
    var r = roleDescriptor();
    r.setId(id);
    return r;
  }

  public static User userDescriptor() {
    return User.builder()
      .id(USER_ID)
      .userName(USERNAME)
      .enabled(true)
      .build();
  }

  public static AuthorizationRolePolicy authorizationPolicy() {
    var rolePolicy = new AuthorizationRolePolicy();
    rolePolicy.setName(POLICY_NAME);
    rolePolicy.setRoles(List.of(RolePolicy.of(ROLE_ID, false)));
    rolePolicy.setType("role");
    return rolePolicy;
  }

  public static AuthorizationRolePolicy authorizationPolicy(String id) {
    var policy = authorizationPolicy();
    policy.setId(id);
    return policy;
  }

  public static AuthorizationClientPolicy authorizationClientPolicy() {
    var policy = new AuthorizationClientPolicy();
    policy.setName(IMPERSONATE_POLICY);
    policy.setClients(List.of(IMPERSONATE_CLIENT));
    policy.setType(CLIENT_POLICY_TYPE);
    return policy;
  }

  public static AuthorizationPermission authorizationPermission() {
    return AuthorizationPermission.builder()
      .name(PERMISSION_NAME)
      .policies(List.of(POLICY_ID))
      .type("scope")
      .build();
  }

  public static AuthorizationPermission authorizationPermission(String id) {
    var permission = authorizationPermission();
    permission.setId(id);
    return permission;
  }

  public static AuthorizationPermission authorizationImpersonationPermission() {
    return AuthorizationPermission.builder()
      .name(ADMIN_IMPERSONATING_PERMISSION)
      .policies(List.of(IMPERSONATE_POLICY_ID))
      .build();
  }

  public static AuthorizationScope authorizationScope(String scope) {
    return AuthorizationScope.builder()
      .iconUri(scope)
      .displayName(scope)
      .name(scope)
      .id(UUID.randomUUID().toString())
      .build();
  }

  public static AuthorizationScope authorizationScope() {
    return authorizationScope(SCOPE_GET);
  }

  public static ServerInfo serverInfo() {
    var serverInfo = new ServerInfo();
    var mapper = new ProtocolMapperType();
    mapper.setId(PASSWORD_RESET_ACTION_MAPPER);
    mapper.setName(PASSWORD_RESET_ACTION_MAPPER);
    serverInfo.setProtocolMapperTypes(Map.of(OPENID_CONNECT_PROTOCOL, List.of(mapper)));
    return serverInfo;
  }

  public static Client clientDescriptor(String id, String name, String clientId, String clientSecret, String desc) {
    var client = clientDescriptor(clientId, clientSecret, desc);
    client.setId(id);
    client.setName(name);
    return client;
  }

  public static Client clientDescriptor(String name, String clientId, String clientSecret, String desc) {
    var client = clientDescriptor(clientId, clientSecret, desc);
    client.setName(name);
    return client;
  }

  public static Client clientDescriptor(String clientId, String clientSecret, String desc) {
    return Client.builder()
      .name(clientId)
      .clientId(clientId)
      .description(desc)
      .secret(clientSecret)
      .enabled(true)
      .authorizationServicesEnabled(true)
      .directAccessGrantsEnabled(true)
      .serviceAccountsEnabled(true)
      .frontChannelLogout(true)
      .webOrigins(List.of(URIS))
      .redirectUris(List.of(URIS))
      .attributes(getClientAttributes())
      .build();
  }

  public static ProtocolMapper protocolMapper(String mapperType, String mapperName, String userAttr, String claimName) {
    return ProtocolMapper.builder()
      .mapper(mapperType)
      .protocol(OPENID_CONNECT_PROTOCOL)
      .name(mapperName)
      .config(ProtocolMapper.Config.of(true, true, true, userAttr, claimName, "String"))
      .build();
  }

  private static Client.Attribute getClientAttributes() {
    return Client.Attribute.builder()
      .oauth2DeviceAuthGrantEnabled(false)
      .oidcCibaGrantEnabled(false)
      .clientSecretCreationTime(Instant.now().getEpochSecond())
      .backChannelLogoutSessionRequired(true)
      .backChannelLogoutRevokeOfflineTokens(false)
      .build();
  }

  public static UserManagementPermission userManagementPermission() {
    return UserManagementPermission.builder()
      .enabled(true)
      .build();
  }

  public static UserManagementPermission userManagementPermission(ScopePermission scopePermission) {
    return UserManagementPermission.builder()
      .enabled(true)
      .scopePermissions(scopePermission)
      .build();
  }

  public static ScopePermission scopePermission() {
    return UserManagementPermission.ScopePermission.builder()
      .impersonate(IMPERSONATE_SCOPE_ID)
      .build();
  }
}
