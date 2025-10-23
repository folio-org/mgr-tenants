package org.folio.tm.integration.entitlements;

import static java.util.Objects.requireNonNull;
import static org.folio.tm.integration.okapi.OkapiHeaders.TOKEN;

import feign.FeignException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.tm.exception.RequestValidationException;
import org.folio.tm.integration.entitlements.model.EntitlementsResponse;
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
   * Uses RequestContextHolder to safely access the current request context across threads. This method implements
   * fail-close behavior: if the token is missing, it throws an exception which will be caught by the caller and result
   * in blocking the deletion operation.
   * </p>
   *
   * @return authentication token
   * @throws IllegalStateException if token is missing, empty, or request context is unavailable
   */
  private String getTokenFromRequest() {
    var requestAttributes = RequestContextHolder.getRequestAttributes();
    requireNonNull(requestAttributes, "No request context available - cannot retrieve authentication token");

    var httpRequest = ((ServletRequestAttributes) requestAttributes).getRequest();
    var token = httpRequest.getHeader(TOKEN);

    if (token == null || token.isEmpty()) {
      throw new IllegalStateException("Missing authentication token in request header: " + TOKEN);
    }
    return token;
  }

  private static boolean hasEntitlements(EntitlementsResponse response) {
    return response != null && response.getEntitlements() != null && !response.getEntitlements().isEmpty();
  }
}
