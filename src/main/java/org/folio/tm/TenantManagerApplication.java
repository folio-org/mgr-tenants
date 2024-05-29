package org.folio.tm;

import static org.folio.common.utils.tls.FipsChecker.getFipsChecksResultString;

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
    SpringApplication.run(TenantManagerApplication.class, args);
  }
}
