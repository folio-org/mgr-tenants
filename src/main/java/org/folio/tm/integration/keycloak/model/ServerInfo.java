package org.folio.tm.integration.keycloak.model;

import java.util.List;
import java.util.Map;
import lombok.Data;

@Data
public class ServerInfo {
  private Map<String, List<ProtocolMapperType>> protocolMapperTypes;
}
