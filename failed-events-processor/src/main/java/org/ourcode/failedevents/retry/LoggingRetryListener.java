package org.ourcode.failedevents.retry;

import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryListener;

@Slf4j
public class LoggingRetryListener implements RetryListener {

    @Override
    public <T, E extends Throwable> void onError(RetryContext context, RetryCallback<T, E> callback, Throwable throwable) {
        log.warn("Retry attempt {}/{} failed due to: {}",
                context.getRetryCount(),
                context.getAttribute("context.max-attempts"),
                throwable.getMessage()
        );
    }

    @Override
    public <T, E extends Throwable> void close(RetryContext context, RetryCallback<T, E> callback, Throwable throwable) {
        if (throwable == null) {
            log.info("Operation succeeded after {} attempts", context.getRetryCount());
        } else {
            log.error("Operation failed after {} attempts due to: {}", context.getRetryCount(), throwable.getMessage());
        }
    }

}
