package org.folio.tm.mapper;

import java.time.OffsetDateTime;
import java.util.Date;
import org.springframework.stereotype.Component;

@Component
public class MappingMethods {

  public Date offsetDateTimeAsDate(OffsetDateTime offsetDateTime) {
    return offsetDateTime == null ? null : Date.from(offsetDateTime.toInstant());
  }
}
