package org.folio.tm.integration.kafka;

import static java.lang.Thread.currentThread;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang3.BooleanUtils.isNotTrue;
import static org.folio.common.configuration.properties.FolioEnvironment.getFolioEnvName;

import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.kafka.clients.admin.AdminClient;
import org.springframework.stereotype.Service;

@Log4j2
@Service
@RequiredArgsConstructor
public class KafkaService {

  private static final long TIMEOUT_SECONDS = 30;

  private final AdminClient adminClient;

  public void deleteTopics(String tenant, Boolean purgeKafkaTopics) {
    if (isNotTrue(purgeKafkaTopics)) {
      log.info("Purge Kafka topics is disabled, skipping topic deletion for tenant: tenant = {}", tenant);
      return;
    }

    try {
      var topicsToDelete = getTopicsToDelete(tenant);
      if (topicsToDelete.isEmpty()) {
        log.info("No topics to delete for tenant: tenant = {}", tenant);
        return;
      }
      log.debug("Deleting topics for tenant: tenant = {}, topics = {}", tenant, topicsToDelete);
      adminClient.deleteTopics(topicsToDelete).all().get(TIMEOUT_SECONDS, SECONDS);
      log.info("Deleted topics successfully: tenant = {}", tenant);
    } catch (InterruptedException e) {
      currentThread().interrupt();
      log.warn("Interrupted while deleting topics: tenant = {}", tenant, e);
    } catch (ExecutionException | TimeoutException e) {
      log.warn("Error deleting topics: tenant = {}", tenant, e);
    }
  }

  private Set<String> getTopicsToDelete(String tenant)
    throws InterruptedException, ExecutionException, TimeoutException {
    var allTopics = adminClient.listTopics().names().get(TIMEOUT_SECONDS, SECONDS);
    log.debug("All topics: {}", () -> allTopics);
    var topicPrefix = getTopicPrefix(tenant);
    return allTopics.stream()
      .filter(name -> name.contains(topicPrefix))
      .collect(toSet());
  }

  public static String getTopicPrefix(String tenant) {
    return String.format("%s.%s.", getFolioEnvName(), tenant);
  }
}
