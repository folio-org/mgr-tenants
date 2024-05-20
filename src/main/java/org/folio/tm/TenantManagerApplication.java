package org.folio.tm;

import java.security.Security;
import org.bouncycastle.jcajce.provider.BouncyCastleFipsProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class TenantManagerApplication {

  /**
   * Runs spring application.
   *
   * @param args command line arguments.
   */
  public static void main(String[] args) {
    if (Security.getProvider("BCFIPS") == null) {
      Security.addProvider(new BouncyCastleFipsProvider());
    }
    SpringApplication.run(TenantManagerApplication.class, args);
  }
}
