package org.folio.tm.integration.keycloak.model;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuthorizationClientPolicy extends AuthorizationPolicy {

  private List<String> clients;
}
