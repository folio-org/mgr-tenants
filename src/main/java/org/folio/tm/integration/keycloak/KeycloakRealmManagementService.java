package org.folio.tm.integration.keycloak;

import static org.folio.tm.integration.keycloak.KeycloakUtils.SCOPES;
import static org.folio.tm.integration.keycloak.model.Client.CLIENT_SECRET_AUTH_TYPE;
import static org.folio.tm.integration.keycloak.model.Client.OPENID_CONNECT_PROTOCOL;
import static org.folio.tm.integration.keycloak.model.ProtocolMapper.STRING_TYPE_LABEL;
import static org.folio.tm.integration.keycloak.model.ProtocolMapper.USER_ATTRIBUTE_MAPPER_TYPE;
import static org.folio.tm.integration.keycloak.model.ProtocolMapper.USER_PROPERTY_MAPPER_TYPE;

import jakarta.persistence.EntityNotFoundException;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.tm.domain.dto.Tenant;
import org.folio.tm.integration.keycloak.configuration.KeycloakRealmSetupProperties;
import org.folio.tm.integration.keycloak.model.AuthorizationPermission;
import org.folio.tm.integration.keycloak.model.AuthorizationRolePolicy;
import org.folio.tm.integration.keycloak.model.AuthorizationRolePolicy.RolePolicy;
import org.folio.tm.integration.keycloak.model.AuthorizationScope;
import org.folio.tm.integration.keycloak.model.Client;
import org.folio.tm.integration.keycloak.model.ProtocolMapper;
import org.folio.tm.integration.keycloak.model.ProtocolMapperType;
import org.folio.tm.integration.keycloak.model.Realm;
import org.folio.tm.integration.keycloak.model.Role;
import org.folio.tm.service.listeners.TenantServiceListener;

@Log4j2
@RequiredArgsConstructor
public class KeycloakRealmManagementService {

  private static final String SYSTEM_ROLE_NAME = "System";
  private static final String SYSTEM_ROLE_DESC = "System role for module-to-module communication";
  private static final String M2M_CLIENT_DESC = "Client for module-to-module communication";
  private static final String SYSTEM_ROLE_POLICY = "System role policy";
  private static final String SYSTEM_ROLE_PERMISSION = "System role permission";
  private static final String PASSWORD_RESET_ACTION_MAPPER = "Password reset action mapper";
  private static final String PASSWORD_RESET_ROLE = "Password Reset";
  private static final String PASSWORD_RESET_POLICY = PASSWORD_RESET_ROLE + " policy";
  private static final String PASSWORD_RESET_CLIENT_DESC = "Client for password reset operations";
  private static final String PASSWORD_RESET_ROLE_DESC = "A role with access to password reset endpoints";
  private static final String PASSWORD_RESET_ACTION_ID_CLAIM = "passwordResetActionId";
  private static final String LOGIN_CLIENT_DESC = "Client for login operations";
  private static final String USERNAME_PROPERTY = "username";
  private static final String USER_ID_PROPERTY = "user_id";
  private static final String URIS = "/*";

  private final KeycloakRealmService realmService;
  private final KeycloakClientService clientService;
  private final KeycloakRoleService roleService;
  private final KeycloakPolicyService policyService;
  private final KeycloakPermissionService permissionService;
  private final KeycloakAuthScopeService authScopeService;
  private final ClientSecretService clientSecretService;
  private final KeycloakRealmSetupProperties setupProperties;
  private final KeycloakImpersonationService keycloakImpersonationService;

  public void setupRealm(Tenant tenant) {
    var realm = createRealm(tenant);
    setupClients(realm.getName());
  }

  public Realm createRealm(Tenant tenant) {
    return realmService.createRealm(tenant);
  }

  public Realm updateRealm(Tenant tenant) {
    return realmService.updateRealm(tenant);
  }

  public void destroyRealm(String tenantName) {
    realmService.deleteRealm(tenantName);
  }

  public TenantServiceListener tenantServiceListener() {
    return this.new TenantListener();
  }

  public void setupModuleClient(String realmName, Role sysRole) {
    var m2mClient = createModuleClientInRealm(realmName);

    var serviceAccount = clientService.getClientServiceAccountUser(m2mClient, realmName);
    roleService.assignRole(sysRole, serviceAccount, realmName);
  }

  public void setupImpersonationClient(String realmName) {
    keycloakImpersonationService.setupImpersonationClient(realmName);
  }

  public void setupLoginClient(String realmName, Role sysRole, Role passwordResetRole) {
    var loginClient = createLoginClientInRealm(realmName);
    var clientId = loginClient.getId();

    var policy = createSystemRolePolicy(realmName, clientId, sysRole.getId());
    createSystemRolePermission(realmName, clientId, policy.getId());

    createResetPasswordRolePolicy(realmName, clientId, passwordResetRole.getId());
  }

  public void setupPasswordResetClient(String realm, Role passwordResetRole) {
    var client = createPasswordResetClient(realm);
    var serviceAccount = clientService.getClientServiceAccountUser(client, realm);
    roleService.assignRole(passwordResetRole, serviceAccount, realm);
  }

  private void setupClients(String realmName) {
    var sysRole = createSystemRole(realmName);
    var passwordResetRole = createPasswordResetRole(realmName);

    setupModuleClient(realmName, sysRole);
    setupLoginClient(realmName, sysRole, passwordResetRole);
    setupImpersonationClient(realmName);
    setupPasswordResetClient(realmName, passwordResetRole);
  }

  private Client createPasswordResetClient(String realm) {
    var clientProperties = setupProperties.getPasswordResetClient();
    var clientId = clientProperties.getClientId();

    log.info("Creating Keycloak password reset client in realm: clientId = {}, realm = {}", clientId,
      realm);

    var passwordResetMapper = findProtocolMapper(PASSWORD_RESET_ACTION_MAPPER);

    var client = buildClient(realm, clientId, PASSWORD_RESET_CLIENT_DESC, List.of(passwordResetMapper), false, true);
    var attributes = client.getAttributes();
    attributes.setAccessTokenLifeSpan(clientProperties.getTokenLifespan());
    attributes.setUseRefreshTokens(false);
    return clientService.createClient(client, realm);
  }

  private Client createLoginClientInRealm(String realm) {
    var loginClient = setupProperties.getLoginClient();

    log.info("Creating Keycloak login client in realm: clientId = {}, realm = {}", loginClient,
      realm);

    var usernameMapper = protocolMapper(USER_PROPERTY_MAPPER_TYPE, USERNAME_PROPERTY, USERNAME_PROPERTY, "sub");
    var userIdMapper = protocolMapper(USER_ATTRIBUTE_MAPPER_TYPE, "user_id mapper", USER_ID_PROPERTY, USER_ID_PROPERTY);

    var clientId = realm + loginClient.getClientId();
    var client = buildClient(realm, clientId, LOGIN_CLIENT_DESC, List.of(usernameMapper, userIdMapper), true, true);
    return clientService.createClient(client, realm);
  }

  private Client buildClient(String realm, String clientId, String desc, List<ProtocolMapper> mappers,
    boolean authEnabled, boolean serviceAccountEnabled) {
    var attributes = getClientAttributes();

    return Client.builder()
      .clientId(clientId)
      .name(clientId)
      .description(desc)
      .secret(clientSecretService.getOrCreateClientSecret(realm, clientId))
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

  private static Client.Attribute getClientAttributes() {
    return Client.Attribute.builder()
      .oauth2DeviceAuthGrantEnabled(false)
      .oidcCibaGrantEnabled(false)
      .clientSecretCreationTime(Instant.now().getEpochSecond())
      .backChannelLogoutSessionRequired(true)
      .backChannelLogoutRevokeOfflineTokens(false)
      .build();
  }

  private ProtocolMapper protocolMapper(String mapperType, String mapperName, String userAttr, String claimName) {
    return ProtocolMapper.builder()
      .mapper(mapperType)
      .protocol(OPENID_CONNECT_PROTOCOL)
      .name(mapperName)
      .config(ProtocolMapper.Config.of(true, true, true, userAttr, claimName, STRING_TYPE_LABEL))
      .build();
  }

  private Client createModuleClientInRealm(String realm) {
    var m2mClient = setupProperties.getM2mClient();

    var clientId = m2mClient.getClientId();
    log.info("Creating Keycloak module-to-module client in realm: clientId = {}, realm = {}", clientId,
      realm);

    var client = buildClient(realm, clientId, M2M_CLIENT_DESC, null, true, true);
    client.setClientAuthenticatorType(CLIENT_SECRET_AUTH_TYPE);
    return clientService.createClient(client, realm);
  }

  private Role createSystemRole(String realm) {
    log.info("Creating System role in realm: {}", realm);
    return roleService.createRole(SYSTEM_ROLE_NAME, SYSTEM_ROLE_DESC, realm);
  }

  private AuthorizationRolePolicy createSystemRolePolicy(String realm, String client, String roleId) {
    log.info("Creating System role policy in client: {}", client);
    var policy = new AuthorizationRolePolicy();
    policy.setName(SYSTEM_ROLE_POLICY);
    policy.setRoles(List.of(RolePolicy.of(roleId, false)));
    policy.setType("role");
    return policyService.createRolePolicy(policy, realm, client);
  }

  private void createSystemRolePermission(String realm, String clientId, String policyId) {
    var scopeIds = createThenGetAuthScopeIds(realm, clientId);

    var authPermission = AuthorizationPermission.builder()
      .name(SYSTEM_ROLE_PERMISSION)
      .type("scope")
      .policies(List.of(policyId))
      .scopes(scopeIds)
      .build();
    permissionService.createRolePermission(authPermission, realm, clientId);
  }

  private Role createPasswordResetRole(String realm) {
    log.info("Creating Password Reset role in realm: {}", realm);
    return roleService.createRole(PASSWORD_RESET_ROLE, PASSWORD_RESET_ROLE_DESC, realm);
  }

  private void createResetPasswordRolePolicy(String realm, String client, String roleId) {
    log.info("Creating Reset Password role policy in client: {}", client);
    var policy = new AuthorizationRolePolicy();
    policy.setName(PASSWORD_RESET_POLICY);
    policy.setRoles(List.of(RolePolicy.of(roleId, false)));
    policy.setType("role");
    policyService.createRolePolicy(policy, realm, client);
  }

  private ProtocolMapper findProtocolMapper(String mapperName) {
    var mappers = realmService.getServerInfo().getProtocolMapperTypes().get(OPENID_CONNECT_PROTOCOL);
    return mappers.stream().filter(mapper -> mapper.getName().equals(mapperName))
      .findFirst()
      .map(this::mapProtocolMapper)
      .orElseThrow(() -> new EntityNotFoundException(
        "Mapper is not found by name: " + PASSWORD_RESET_ACTION_MAPPER));
  }

  private ProtocolMapper mapProtocolMapper(ProtocolMapperType mapperType) {
    var name = mapperType.getName();
    var id = mapperType.getId();
    return protocolMapper(id, name, null, PASSWORD_RESET_ACTION_ID_CLAIM);
  }

  private List<String> createThenGetAuthScopeIds(String realm, String clientId) {
    return authScopeService.createAuthScopes(realm, clientId, SCOPES)
      .stream().map(AuthorizationScope::getId).collect(Collectors.toList());
  }

  private final class TenantListener implements TenantServiceListener {

    @Override
    public void onTenantCreate(Tenant tenant) {
      log.debug("Running Keycloak event 'onTenantCreate' for tenant {}", tenant.getName());

      setupRealm(tenant);
    }

    @Override
    public void onTenantUpdate(Tenant tenant) {
      log.debug("Running Keycloak event 'onTenantUpdate' for tenant {}", tenant.getName());

      updateRealm(tenant);
    }

    @Override
    public void onTenantDelete(String tenantName) {
      log.debug("Running Keycloak event 'onTenantDelete' for tenant {}", tenantName);

      destroyRealm(tenantName);
    }
  }
}
