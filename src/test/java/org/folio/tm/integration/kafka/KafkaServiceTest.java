package org.folio.tm.integration.kafka;

import static java.lang.Boolean.TRUE;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Set;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.DeleteTopicsResult;
import org.apache.kafka.clients.admin.ListTopicsResult;
import org.apache.kafka.common.KafkaFuture;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class KafkaServiceTest {

  @InjectMocks private KafkaService kafkaService;

  @Mock(answer = RETURNS_DEEP_STUBS) private AdminClient adminClient;

  @Test
  void deleteTopics_skip_purgeKafkaTopicsIsFalse() {
    kafkaService.deleteTopics("tenant", false);

    verifyNoInteractions(adminClient);
  }

  @Test
  void deleteTopics_skip_purgeKafkaTopicsIsNull() {
    kafkaService.deleteTopics("tenant", null);

    verifyNoInteractions(adminClient);
  }

  @ParameterizedTest
  @MethodSource("positiveTopicsProvider")
  void deleteTopics_positive(String tenant, Set<String> allTopics, Set<String> topicsToDelete) throws Exception {
    when(adminClient.listTopics().names().get(anyLong(), eq(SECONDS)))
      .thenReturn(allTopics);

    var deleteResult = mock(DeleteTopicsResult.class, RETURNS_DEEP_STUBS);
    var fakeFuture = mock(KafkaFuture.class);

    when(adminClient.deleteTopics(topicsToDelete)).thenReturn(deleteResult);
    when(deleteResult.all()).thenReturn(fakeFuture);
    when(fakeFuture.get(anyLong(), eq(SECONDS))).thenReturn(null);

    kafkaService.deleteTopics(tenant, TRUE);

    verify(adminClient).deleteTopics(topicsToDelete);
    verify(fakeFuture).get(anyLong(), eq(SECONDS));
  }

  @Test
  void deleteTopics_positive_noTopicsToDelete() throws Exception {
    var allTopics = Set.of("folio.tenant1.topic1", "folio.tenant2.topic1", "env.tenant.topic");

    var listTopicsResult = mock(ListTopicsResult.class);
    var names = mock(KafkaFuture.class);
    doReturn(listTopicsResult).when(adminClient).listTopics();
    when(listTopicsResult.names()).thenReturn(names);
    when(names.get(anyLong(), eq(SECONDS))).thenReturn(allTopics);

    kafkaService.deleteTopics("tenant", TRUE);

    verify(adminClient).listTopics();
    verifyNoInteractions(adminClient.deleteTopics(anySet()));
  }

  @Test
  void deleteTopics_positive_exception() throws Exception {
    var listTopicsResult = mock(ListTopicsResult.class);
    var names = mock(KafkaFuture.class);
    doReturn(listTopicsResult).when(adminClient).listTopics();
    when(listTopicsResult.names()).thenReturn(names);
    when(names.get(anyLong(), eq(SECONDS))).thenThrow(new TimeoutException("Test exception"));

    kafkaService.deleteTopics("tenant", TRUE);

    verify(adminClient).listTopics();
    verifyNoInteractions(adminClient.deleteTopics(anySet()));
  }

  @Test
  void deleteTopics_positive_interruptedException() throws Exception {
    var listTopicsResult = mock(ListTopicsResult.class);
    var names = mock(KafkaFuture.class);
    doReturn(listTopicsResult).when(adminClient).listTopics();
    when(listTopicsResult.names()).thenReturn(names);
    when(names.get(anyLong(), eq(SECONDS))).thenThrow(new InterruptedException("Test exception"));

    kafkaService.deleteTopics("tenant", TRUE);

    verify(adminClient).listTopics();
    verifyNoInteractions(adminClient.deleteTopics(anySet()));
  }

  static Stream<Arguments> positiveTopicsProvider() {
    return Stream.of(
      Arguments.of("tenant", Set.of("folio.tenant.topic1", "folio.tenant1.topic1", "env.tenant.topic"),
        Set.of("folio.tenant.topic1")),
      Arguments.of("tenant", Set.of("folio.tenant.topic1", "tenant.topic"), Set.of("folio.tenant.topic1"))
    );
  }
}
