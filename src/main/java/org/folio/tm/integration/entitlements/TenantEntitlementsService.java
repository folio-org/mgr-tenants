package org.folio.tm.integration.entitlements;

import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.removeStartIgnoreCase;
import static org.folio.tm.integration.okapi.OkapiHeaders.TOKEN;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;

import feign.FeignException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.folio.tm.exception.RequestValidationException;
import org.folio.tm.integration.entitlements.model.EntitlementsResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Service for checking tenant entitlements in mgr-tenant-entitlements.
 *
 * <p>
 * This service uses token from HTTP request for service-to-service communication with mgr-tenant-entitlements. Uses
 * RequestContextHolder for thread-safe access to request-scoped data.
 * </p>
 */
@Log4j2
@Service
@RequiredArgsConstructor
public class TenantEntitlementsService {

  private final TenantEntitlementsClient client;

  /**
   * Checks if tenant can be deleted by verifying it has no active entitlements.
   *
   * <p>
   * This method implements fail-close strategy: if mgr-tenant-entitlements service is unavailable or returns an error,
   * it will throw an exception to prevent tenant deletion.
   * </p>
   *
   * @param tenantName - tenant name for logging purposes
   * @param tenantId - tenant UUID from database
   * @throws RequestValidationException if tenant has entitlements or entitlements check failed
   */
  public void checkTenantCanBeDeleted(String tenantName, UUID tenantId) {
    requireNonNull(tenantId, "tenantId cannot be null");
    try {
      verifyNoActiveEntitlements(tenantName, tenantId);
    } catch (FeignException.NotFound e) {
      log.debug("No entitlements found for tenant '{}': {}", tenantName, e.getMessage());
    } catch (RequestValidationException e) {
      throw e;
    } catch (FeignException e) {
      handleServiceError(tenantName, e);
    } catch (Exception e) {
      handleUnexpectedError(tenantName, e);
    }
  }

  private void verifyNoActiveEntitlements(String tenantName, UUID tenantId) {
    log.debug("Checking entitlements for tenant '{}' [id: {}]", tenantName, tenantId);
    var token = getTokenFromRequest();
    var query = "tenantId==" + tenantId;
    var response = client.getEntitlements(1, query, token);

    if (hasEntitlements(response)) {
      log.warn("Cannot delete tenant '{}': tenant has active entitlements", tenantName);
      throw createValidationException("Please uninstall applications first");
    }

    log.debug("Tenant '{}' has no entitlements, deletion allowed", tenantName);
  }

  private void handleServiceError(String tenantName, FeignException e) {
    log.warn("Failed to check entitlements for tenant '{}' [status: {}, message: {}]. Deletion not allowed.",
      tenantName, e.status(), e.getMessage());
    throw createValidationException("Unable to verify tenant's entitlements state, try again");
  }

  private void handleUnexpectedError(String tenantName, Exception e) {
    log.warn("Unexpected error checking entitlements for tenant '{}'. Deletion not allowed.", tenantName, e);
    throw createValidationException("Unable to verify tenant's entitlements state, try again");
  }

  private static RequestValidationException createValidationException(String cause) {
    return new RequestValidationException("Cannot delete tenant", "cause", cause);
  }

  /**
   * Retrieves authentication token from the HTTP request in a thread-safe manner.
   *
   * <p>
   * Uses RequestContextHolder to safely access the current request context across threads. Returns null if token is
   * not present, allowing the service to work in environments where security is disabled. The mgr-tenant-entitlements
   * client accepts null tokens (required = false).
   * </p>
   *
   * @return authentication token, or null if not present or request context unavailable
   */
  private String getTokenFromRequest() {
    var requestAttributes = RequestContextHolder.getRequestAttributes();
    if (requestAttributes == null) {
      log.debug("No request context available - cannot retrieve authentication token");
      return null;
    }

    var httpRequest = ((ServletRequestAttributes) requestAttributes).getRequest();
    return extractToken(httpRequest);
  }

  /**
   * Extracts authentication token from HTTP request headers.
   *
   * <p>
   * Checks the following headers in order:
   * <ol>
   *   <li>X-Okapi-Token - FOLIO token header</li>
   *   <li>Authorization - Standard Bearer token (strips "Bearer " prefix)</li>
   * </ol>
   * </p>
   *
   * @param request - HTTP request
   * @return authentication token, or null if not found
   */
  private static String extractToken(HttpServletRequest request) {
    var okapiToken = request.getHeader(TOKEN);
    if (isNotBlank(okapiToken)) {
      return okapiToken;
    }

    var authHeader = request.getHeader(AUTHORIZATION);
    if (isNotBlank(authHeader)) {
      return trimTokenBearer(authHeader);
    }

    log.debug("No authentication token found in request headers");
    return null;
  }

  /**
   * Removes "Bearer " prefix from authorization header token (case-insensitive).
   *
   * @param token - authorization header value
   * @return token without Bearer prefix
   */
  private static String trimTokenBearer(String token) {
    return removeStartIgnoreCase(token, "Bearer ");
  }

  private static boolean hasEntitlements(EntitlementsResponse response) {
    return response != null && response.getEntitlements() != null && !response.getEntitlements().isEmpty();
  }
}
