package org.ourcode.eventcollector.api.exception;

public class MessageNotPublishedException extends RuntimeException {

    public MessageNotPublishedException(String message, Throwable cause) {
        super(message, cause);
    }

}
