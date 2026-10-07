package com.kamsarobar.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

/**
 * Local Caffeine cache (see spring.cache.* in application.yml). For a multi-instance deployment,
 * switch spring.cache.type to "redis" and add spring-boot-starter-data-redis - no code changes needed.
 */
@Configuration
@EnableCaching
public class CacheConfig {
}
