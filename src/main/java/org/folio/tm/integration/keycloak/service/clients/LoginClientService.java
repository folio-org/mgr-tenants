package org.folio.tm.integration.keycloak.service.clients;

import static jakarta.ws.rs.HttpMethod.DELETE;
import static jakarta.ws.rs.HttpMethod.GET;
import static jakarta.ws.rs.HttpMethod.OPTIONS;
import static jakarta.ws.rs.HttpMethod.PATCH;
import static jakarta.ws.rs.HttpMethod.POST;
import static jakarta.ws.rs.HttpMethod.PUT;
import static java.util.Map.entry;
import static org.folio.common.utils.CollectionUtils.mapItems;
import static org.folio.tm.integration.keycloak.service.roles.PasswordResetRoleService.PASSWORD_RESET_ROLE_NAME;
import static org.folio.tm.integration.keycloak.service.roles.SystemRoleService.SYSTEM_ROLE_NAME;
import static org.folio.tm.integration.keycloak.utils.KeycloakClientUtils.getFolioUserTokenMappers;
import static org.keycloak.representations.idm.authorization.DecisionStrategy.AFFIRMATIVE;
import static org.keycloak.representations.idm.authorization.DecisionStrategy.UNANIMOUS;
import static org.keycloak.representations.idm.authorization.Logic.POSITIVE;
import static org.keycloak.representations.idm.authorization.PolicyEnforcementMode.ENFORCING;

import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.folio.tm.integration.keycloak.configuration.KeycloakRealmSetupProperties;
import org.folio.tm.integration.keycloak.model.ClientAttributes;
import org.folio.tm.utils.JsonHelper;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.authorization.PolicyRepresentation;
import org.keycloak.representations.idm.authorization.ResourceServerRepresentation;
import org.keycloak.representations.idm.authorization.RolePolicyRepresentation.RoleDefinition;
import org.keycloak.representations.idm.authorization.ScopeRepresentation;

@RequiredArgsConstructor
public class LoginClientService extends AbstractKeycloakClientService {

  public static final String SYSTEM_ROLE_POLICY_NAME = "System role policy";
  public static final String PASSWORD_RESET_ROLE_POLICY_NAME = PASSWORD_RESET_ROLE_NAME + " policy";
  public static final List<String> SCOPES = List.of(GET, POST, PUT, DELETE, PATCH, OPTIONS);

  private static final String USERNAME_PROPERTY = "username";
  private static final String USER_ID_PROPERTY = "user_id";
  private static final String USER_ID_MAPPER_NAME = "user_id mapper";
  private static final String SYSTEM_ROLE_PERMISSION = "System role permission";

  private final JsonHelper jsonHelper;
  private final KeycloakRealmSetupProperties keycloakRealmSetupProperties;

  @Override
  public ClientRepresentation setupClient(String realm) {
    return setupKeycloakClient(realm);
  }

  @Override
  protected String getClientId(String realm) {
    var loginClientConfiguration = keycloakRealmSetupProperties.getLoginClient();
    var loginClientSuffix = loginClientConfiguration.getClientId();
    return realm + loginClientSuffix;
  }

  @Override
  protected String getClientDescription() {
    return "Client for login operations";
  }

  @Override
  protected Boolean isServiceAccountEnabled() {
    return true;
  }

  @Override
  protected Boolean isAuthorizationServicesEnabled() {
    return true;
  }

  @Override
  protected Map<String, String> getAttributes() {
    return ClientAttributes.defaultValue().asMap();
  }

  @Override
  protected ResourceServerRepresentation getAuthorizationSettings() {
    var resourceServer = new ResourceServerRepresentation();
    resourceServer.setDecisionStrategy(AFFIRMATIVE);
    resourceServer.setPolicyEnforcementMode(ENFORCING);
    resourceServer.setAllowRemoteResourceManagement(true);
    resourceServer.setScopes(mapItems(SCOPES, LoginClientService::createScope));
    resourceServer.setPolicies(List.of(
      buildRolePolicy(SYSTEM_ROLE_POLICY_NAME, SYSTEM_ROLE_NAME),
      buildRolePolicy(PASSWORD_RESET_ROLE_POLICY_NAME, PASSWORD_RESET_ROLE_NAME),
      buildSystemUserPermission()));

    return resourceServer;
  }

  private PolicyRepresentation buildRolePolicy(String name, String roleName) {
    var policyRepresentation = new PolicyRepresentation();
    policyRepresentation.setType("role");
    policyRepresentation.setName(name);
    policyRepresentation.setLogic(POSITIVE);
    policyRepresentation.setDecisionStrategy(UNANIMOUS);

    var roleDefinitions = List.of(new RoleDefinition(roleName, false));
    policyRepresentation.setConfig(Map.of("roles", jsonHelper.asJsonString(roleDefinitions),
      "fetchRoles", "true"));

    return policyRepresentation;
  }

  private PolicyRepresentation buildSystemUserPermission() {
    var policyRepresentation = new PolicyRepresentation();
    policyRepresentation.setType("scope");
    policyRepresentation.setName(SYSTEM_ROLE_PERMISSION);
    policyRepresentation.setLogic(POSITIVE);
    policyRepresentation.setDecisionStrategy(UNANIMOUS);
    policyRepresentation.setConfig(Map.ofEntries(
      entry("scopes", jsonHelper.asJsonString(SCOPES)),
      entry("applyPolicies", jsonHelper.asJsonString(List.of(SYSTEM_ROLE_POLICY_NAME)))));
    return policyRepresentation;
  }

  @Override
  protected List<ProtocolMapperRepresentation> getProtocolMappers() {
    return getFolioUserTokenMappers();
  }

  private static ScopeRepresentation createScope(String name) {
    var scopeRepresentation = new ScopeRepresentation();
    scopeRepresentation.setName(name);
    scopeRepresentation.setIconUri(name);
    scopeRepresentation.setDisplayName(name);
    return scopeRepresentation;
  }
}
