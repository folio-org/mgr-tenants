package org.folio.tm.extension;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.folio.tm.extension.impl.PostgresContainerExtension;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Starts PostgreSQL server on a random port in docker and sets all required Spring properties.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(PostgresContainerExtension.class)
public @interface EnablePostgres {
}
