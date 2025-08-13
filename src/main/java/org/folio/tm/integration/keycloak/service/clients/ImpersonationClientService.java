package org.folio.tm.integration.keycloak.service.clients;

import static jakarta.ws.rs.core.Response.Status.Family.SUCCESSFUL;
import static java.lang.String.format;
import static java.util.Collections.singleton;
import static org.folio.tm.integration.keycloak.utils.KeycloakClientUtils.getFolioUserTokenMappers;

import feign.FeignException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.tm.integration.keycloak.KeycloakClient;
import org.folio.tm.integration.keycloak.configuration.KeycloakRealmSetupProperties;
import org.folio.tm.integration.keycloak.exception.KeycloakException;
import org.folio.tm.integration.keycloak.model.ClientAttributes;
import org.folio.tm.integration.keycloak.model.UserManagementPermission;
import org.keycloak.admin.client.resource.AuthorizationResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.authorization.ClientPolicyRepresentation;
import org.keycloak.representations.idm.authorization.ScopePermissionRepresentation;

@Log4j2
@RequiredArgsConstructor
public class ImpersonationClientService extends AbstractKeycloakClientService {

  private static final String CLIENT_POLICY_TYPE = "client";
  private static final String REALM_MANAGEMENT_CLIENT = "realm-management";
  private static final String IMPERSONATION_POLICY_NAME = "impersonation-policy";
  private static final String ADMIN_IMPERSONATING_PERMISSION = "admin-impersonating.permission.users";

  private final KeycloakClient keycloakClient;
  private final KeycloakRealmSetupProperties keycloakRealmSetupProperties;

  @Override
  public ClientRepresentation setupClient(String realm) {
    var userManagementPermission = enableUserManagement(realm);
    var impersonationClient = setupKeycloakClient(realm);
    var impersonationClientId = impersonationClient.getId();

    var realmMgmtClientId = getRealmMgmtClientId(realm);
    var authorizationResource = getRealmMgmtAuthorizationResource(realm, realmMgmtClientId);

    createImpersonationPolicy(impersonationClientId, authorizationResource, realmMgmtClientId);
    updateImpersonationPermissions(userManagementPermission, authorizationResource);

    return impersonationClient;
  }

  @Override
  protected String getClientId(String realm) {
    return keycloakRealmSetupProperties.getImpersonationClient();
  }

  @Override
  protected List<ProtocolMapperRepresentation> getProtocolMappers() {
    return getFolioUserTokenMappers();
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
  protected String getClientDescription() {
    return "Client for impersonating user (used for scheduling jobs and in consortia)";
  }

  @Override
  protected Map<String, String> getAttributes() {
    return ClientAttributes.defaultValue().asMap();
  }

  private UserManagementPermission enableUserManagement(String realm) {
    var userManagementPermission = new UserManagementPermission(true);
    var authorizationHeader = "Bearer " + keycloak.tokenManager().getAccessTokenString();
    try {
      return keycloakClient.updateRealmUserManagementPermission(realm, userManagementPermission, authorizationHeader);
    } catch (FeignException exception) {
      throw new KeycloakException("Failed to enable user management in Keycloak for realm: " + realm, exception);
    }
  }

  private ClientRepresentation getRealmManagementClient(String realm) {
    var foundClients = keycloak.realm(realm).clients().findByClientId(REALM_MANAGEMENT_CLIENT);
    return foundClients.stream()
      .findFirst()
      .orElseThrow(() -> new KeycloakException("Failed to find realm management client for realm: " + realm));
  }

  private AuthorizationResource getRealmMgmtAuthorizationResource(String realm, String realmMgmtClientId) {
    var realmResource = keycloak.realm(realm);
    return realmResource.clients().get(realmMgmtClientId).authorization();
  }

  private String getRealmMgmtClientId(String realm) {
    var realmManagementClient = getRealmManagementClient(realm);
    return realmManagementClient.getId();
  }

  private static void processKeycloakResponse(Response response, ClientPolicyRepresentation policy) {
    var statusInfo = response.getStatusInfo();
    if (statusInfo.getFamily() == SUCCESSFUL) {
      log.debug("Keycloak impersonation policy created: id = {}, name = {}", REALM_MANAGEMENT_CLIENT, policy.getName());
      return;
    }

    throw new KeycloakException(format(
      "Failed to create impersonation policy. Details: clientId = %s, name = %s, status = %s, message = %s",
      REALM_MANAGEMENT_CLIENT, policy.getName(), statusInfo.getStatusCode(), statusInfo.getReasonPhrase()));
  }

  private static ClientPolicyRepresentation buildImpersonationPolicy(String impersonationClientId) {
    var policy = new ClientPolicyRepresentation();
    policy.setName(IMPERSONATION_POLICY_NAME);
    policy.setClients(singleton(impersonationClientId));
    policy.setType(CLIENT_POLICY_TYPE);
    return policy;
  }

  private static ScopePermissionRepresentation buildNewImpersonationPermission() {
    var scopePermission = new ScopePermissionRepresentation();
    scopePermission.setName(ADMIN_IMPERSONATING_PERMISSION);
    scopePermission.setPolicies(Set.of(IMPERSONATION_POLICY_NAME));
    return scopePermission;
  }

  private static void createImpersonationPolicy(String impersonationClientId,
    AuthorizationResource authorizationResource, String realmMgmtClientId) {
    var impersonationPolicy = buildImpersonationPolicy(impersonationClientId);

    var clientPoliciesResource = authorizationResource.policies().client();
    try (var response = clientPoliciesResource.create(impersonationPolicy)) {
      processKeycloakResponse(response, impersonationPolicy);
    } catch (WebApplicationException exception) {
      throw new KeycloakException("Failed to create impersonation policy for client: " + realmMgmtClientId, exception);
    }
  }

  private static void updateImpersonationPermissions(UserManagementPermission userManagementPermission,
    AuthorizationResource authorizationResource) {
    var scopePermissionId = userManagementPermission.getScopePermissions().getImpersonate();
    try {
      var newScopePermission = buildNewImpersonationPermission();
      authorizationResource.permissions().scope().findById(scopePermissionId).update(newScopePermission);
    } catch (Exception exception) {
      throw new KeycloakException("Failed to update impersonation permission: " + scopePermissionId, exception);
    }
  }
}
