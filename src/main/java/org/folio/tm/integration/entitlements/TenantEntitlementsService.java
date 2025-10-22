package org.folio.tm.integration.entitlements;

import static java.util.Objects.requireNonNull;
import static org.folio.tm.integration.okapi.OkapiHeaders.TOKEN;

import feign.FeignException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.tm.integration.entitlements.model.EntitlementsResponse;
import org.springframework.stereotype.Service;

/**
 * Service for checking tenant entitlements in mgr-tenant-entitlements.
 *
 * <p>
 * This service uses token from HTTP request for service-to-service communication with mgr-tenant-entitlements.
 * </p>
 */
@Log4j2
@Service
@RequiredArgsConstructor
public class TenantEntitlementsService {

  private final TenantEntitlementsClient client;
  private final HttpServletRequest request;

  /**
   * Checks if tenant has any entitlements.
   *
   * <p>
   * This method implements fail-close strategy: if mgr-tenant-entitlements service is unavailable or returns an error,
   * it will return {@code true} to not allow tenant deletion.
   * </p>
   *
   * @param tenantName - tenant name for logging purposes
   * @param tenantId - tenant UUID from database
   * @return {@code true} if tenant has at least one entitlement, {@code false} otherwise
   */
  public boolean hasTenantEntitlements(String tenantName, UUID tenantId) {
    requireNonNull(tenantId, "tenantId cannot be null");
    try {
      log.debug("Checking entitlements for tenant '{}' [id: {}]", tenantName, tenantId);
      var token = getTokenFromRequest();

      var query = "tenantId==" + tenantId;
      var response = client.getEntitlements(1, query, token);

      return hasEntitlements(response);
    } catch (FeignException.NotFound e) {
      log.debug("No entitlements found for tenant '{}': {}", tenantName, e.getMessage());
      return false;
    } catch (FeignException e) {
      logServiceUnavailable(tenantName, e);
      return true;
    } catch (Exception e) {
      logUnexpectedError(tenantName, e);
      return true;
    }
  }

  /**
   * Retrieves authentication token from the HTTP request.
   *
   * @return authentication token
   * @throws IllegalStateException if token is missing or empty
   */
  private String getTokenFromRequest() {
    var token = request.getHeader(TOKEN);
    if (token == null || token.isEmpty()) {
      throw new IllegalStateException("Missing authentication token in request header: " + TOKEN);
    }
    return token;
  }

  private static boolean hasEntitlements(EntitlementsResponse response) {
    return response != null
      && response.getEntitlements() != null
      && !response.getEntitlements().isEmpty();
  }

  private static void logServiceUnavailable(String tenantName, FeignException e) {
    log.warn("Failed to check entitlements for tenant '{}' [status: {}, message: {}]. Deletion not allowed.",
      tenantName, e.status(), e.getMessage());
  }

  private static void logUnexpectedError(String tenantName, Exception e) {
    log.warn("Unexpected error checking entitlements for tenant '{}'. Deletion not allowed.", tenantName, e);
  }
}
