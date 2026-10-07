package com.kamsarobar.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/** Background work (sending notifications) runs on Spring Boot's managed task executor. */
@Configuration
@EnableAsync
public class AsyncConfig {
}
