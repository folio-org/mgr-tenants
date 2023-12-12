package org.folio.tm.integration.keycloak.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor(staticName = "of")
public class Strategy {

  private DecisionStrategy decisionStrategy;

  public enum DecisionStrategy {
    AFFIRMATIVE, UNANIMOUS
  }
}
