package org.folio.tm.config;

import org.folio.security.EnableMgrSecurity;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableFeignClients
@EnableCaching
@EnableMgrSecurity
public class AppConfiguration {
}
