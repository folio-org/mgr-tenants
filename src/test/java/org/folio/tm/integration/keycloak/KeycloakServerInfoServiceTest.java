package org.folio.tm.integration.keycloak;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.InternalServerErrorException;
import org.folio.test.types.UnitTest;
import org.folio.tm.integration.keycloak.exception.KeycloakException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.ServerInfoResource;
import org.keycloak.representations.info.ServerInfoRepresentation;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class KeycloakServerInfoServiceTest {

  @InjectMocks private KeycloakServerInfoService keycloakServerInfoService;
  @Mock private Keycloak keycloak;
  @Mock private ServerInfoResource serverInfoResource;

  @Test
  void getServerInfo_positive() {
    var serverInfo = serverInfo();
    when(keycloak.serverInfo()).thenReturn(serverInfoResource);
    when(serverInfoResource.getInfo()).thenReturn(serverInfo);

    var result = keycloakServerInfoService.getServerInfo();
    assertThat(result).isEqualTo(serverInfo);
  }

  @Test
  void getServerInfo_negative() {
    when(keycloak.serverInfo()).thenReturn(serverInfoResource);
    when(serverInfoResource.getInfo()).thenThrow(InternalServerErrorException.class);

    assertThatThrownBy(() -> keycloakServerInfoService.getServerInfo())
      .isInstanceOf(KeycloakException.class)
      .hasMessage("Failed to get server info")
      .hasCauseInstanceOf(InternalServerErrorException.class);
  }

  private static ServerInfoRepresentation serverInfo() {
    return new ServerInfoRepresentation();
  }
}
