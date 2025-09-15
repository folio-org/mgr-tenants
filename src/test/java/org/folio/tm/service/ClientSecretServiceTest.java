package org.folio.tm.service;

import static org.folio.tm.support.TestConstants.CLIENT_ID;
import static org.folio.tm.support.TestConstants.REALM_NAME;
import static org.folio.tm.support.TestConstants.SECRET;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.folio.security.integration.keycloak.service.SecureStoreKeyProvider;
import org.folio.test.types.UnitTest;
import org.folio.tm.integration.keycloak.ClientSecretService;
import org.folio.tm.integration.keycloak.configuration.KeycloakRealmSetupProperties;
import org.folio.tools.store.SecureStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class ClientSecretServiceTest {

  @Mock private SecureStoreKeyProvider secureStoreKeyProvider;
  @Mock private SecureStore secureStore;
  @Mock private KeycloakRealmSetupProperties keycloakRealmSetupProperties;

  @InjectMocks private ClientSecretService clientSecretService;

  @Test
  void createClientSecret_positive() {
    when(secureStoreKeyProvider.tenantStoreKey(REALM_NAME, CLIENT_ID)).thenReturn("x_y_z");
    var secretCaptor = ArgumentCaptor.forClass(String.class);
    when(secureStore.lookup("x_y_z")).thenReturn(Optional.empty());
    var secret = clientSecretService.getOrCreateClientSecret(REALM_NAME, CLIENT_ID);

    verify(secureStore).set(eq("x_y_z"), secretCaptor.capture());
    assertNotNull(secret);
    assertEquals(secret, secretCaptor.getValue());
  }

  @Test
  void getClientSecret_positive() {
    when(secureStoreKeyProvider.tenantStoreKey(REALM_NAME, CLIENT_ID)).thenReturn("x_y_z");
    when(secureStore.lookup("x_y_z")).thenReturn(Optional.of(SECRET));
    var secret = clientSecretService.getOrCreateClientSecret(REALM_NAME, CLIENT_ID);
    assertEquals(SECRET, secret);
  }
}
