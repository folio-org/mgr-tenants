package org.folio.tm;

import static org.folio.common.utils.tls.FipsChecker.getFipsChecksResultString;

import java.util.Map;
import java.util.Properties;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@Log4j2
@SpringBootApplication
public class TenantManagerApplication {

  /**
   * Runs spring application.
   *
   * @param args command line arguments.
   */
  public static void main(String[] args) {
    log.info(getFipsChecksResultString());
    log.info(printSystemInfo());
    SpringApplication.run(TenantManagerApplication.class, args);
  }

  public static String printSystemInfo() {
    StringBuilder sb = new StringBuilder();

    // Append all environment variables
    Map<String, String> env = System.getenv();
    sb.append("\nEnvironment Variables:\n");
    for (String envName : env.keySet()) {
      sb.append(String.format("%s=%s%n", envName, env.get(envName)));
    }

    // Append all system properties
    Properties props = System.getProperties();
    sb.append("\nSystem Properties:\n");
    for (String propName : props.stringPropertyNames()) {
      sb.append(String.format("%s=%s%n", propName, props.getProperty(propName)));
    }

    return sb.toString();
  }
}
