package com.dealer.dealer_inventory.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Enables Spring's {@code @Async} support so that audit log writes
 * execute on a separate thread pool and never block the request thread.
 * <p>
 * Pool tuning is done via {@code spring.task.execution.*} properties in
 * {@code application.properties}.
 * </p>
 */
@Configuration
@EnableAsync
public class AsyncConfig {
    // Uses Spring Boot's auto-configured TaskExecutor from application.properties:
    //   spring.task.execution.pool.core-size=10
    //   spring.task.execution.pool.max-size=50
    //   spring.task.execution.pool.queue-capacity=500
    //   spring.task.execution.thread-name-prefix=audit-
}

