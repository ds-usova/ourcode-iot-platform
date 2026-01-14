package org.ourcode.failedevents.minio;

import org.ourcode.failedevents.api.model.FailedEvent;
import org.ourcode.failedevents.api.service.Constants;
import org.springframework.stereotype.Component;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

@Component
public class ObjectNameGenerator {

    public String toObjectName(FailedEvent failedEvent, String extension) {
        ZonedDateTime date = failedEvent.timestamp().atZone(ZoneOffset.UTC);
        String time = String.format("%02d-%02d-%02d",
                date.getHour(),
                date.getMinute(),
                date.getSecond()
        );

        return String.join(Constants.HIERARCHY_SEPARATOR,
                failedEvent.origin(),
                failedEvent.type(),
                "" + date.getYear(),
                "" + date.getMonthValue(),
                "" + date.getDayOfMonth(),
                time + "-" + failedEvent.id() + "." + extension
        );
    }

}
