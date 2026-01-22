package org.ourcode.failedevents.minio;

import org.ourcode.failedevents.api.model.FailedEvent;
import org.springframework.stereotype.Component;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

@Component
public class ObjectNameGenerator {

    private static final String NOT_ALLOWED_CHARACTERS = "[^a-zA-Z0-9._-]";
    private static final String REPLACEMENT_CHAR = "_";
    private static final String HIERARCHY_SEPARATOR = "/";

    public String toObjectName(FailedEvent failedEvent, String extension) {
        ZonedDateTime date = failedEvent.timestamp().atZone(ZoneOffset.UTC);
        String time = String.format("%02d-%02d-%02d",
                date.getHour(),
                date.getMinute(),
                date.getSecond()
        );

        String origin = failedEvent.origin().replaceAll(NOT_ALLOWED_CHARACTERS, REPLACEMENT_CHAR);
        String type = failedEvent.type().replaceAll(NOT_ALLOWED_CHARACTERS, REPLACEMENT_CHAR);
        String id = failedEvent.id().replaceAll(NOT_ALLOWED_CHARACTERS, REPLACEMENT_CHAR);

        return String.join(HIERARCHY_SEPARATOR,
                origin,
                type,
                "" + date.getYear(),
                "" + date.getMonthValue(),
                "" + date.getDayOfMonth(),
                time + "-" + id + "." + extension
        );
    }

}
