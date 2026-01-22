package org.ourcode.failedevents.retry;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.RetryListener;

@Configuration
public class RetryConfiguration {

    @Bean
    public RetryListener loggingRetryListener() {
        return new LoggingRetryListener();
    }

}
