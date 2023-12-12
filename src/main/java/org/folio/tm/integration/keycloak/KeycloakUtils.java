package org.folio.tm.integration.keycloak;

import static jakarta.ws.rs.HttpMethod.DELETE;
import static jakarta.ws.rs.HttpMethod.GET;
import static jakarta.ws.rs.HttpMethod.OPTIONS;
import static jakarta.ws.rs.HttpMethod.PATCH;
import static jakarta.ws.rs.HttpMethod.POST;
import static jakarta.ws.rs.HttpMethod.PUT;

import java.util.List;
import java.util.Optional;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.ResponseEntity;

@UtilityClass
public class KeycloakUtils {

  public static final List<String> SCOPES = List.of(GET, POST, PUT, DELETE, PATCH, OPTIONS);

  public static Optional<String> extractResourceId(ResponseEntity<Void> res) {
    if (res.getStatusCode().is2xxSuccessful() && res.getHeaders().getLocation() != null) {
      var path = res.getHeaders().getLocation().getPath();

      var resourceId = StringUtils.stripToNull(StringUtils.substringAfterLast(path, "/"));
      return Optional.ofNullable(resourceId);
    }

    return Optional.empty();
  }
}
