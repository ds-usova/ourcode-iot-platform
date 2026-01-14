package org.ourcode.failedevents.minio;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.extern.slf4j.Slf4j;
import org.ourcode.failedevents.api.exception.FailedEventStorageException;
import org.ourcode.failedevents.api.gateway.FailedEventGateway;
import org.ourcode.failedevents.api.model.FailedEvent;
import org.ourcode.failedevents.minio.configuration.MinioProperties;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;

@Slf4j
@Component
public class MinioFailedEventGateway implements FailedEventGateway {

    private static final String JSON_CONTENT_TYPE = "application/json";
    private static final String TEXT_CONTENT_TYPE = "text/plain";

    private final MinioClient minioClient;
    private final MinioProperties minioProperties;
    private final ObjectNameGenerator objectNameGenerator;
    private final ObjectMapper objectMapper;

    public MinioFailedEventGateway(
            MinioClient minioClient,
            MinioProperties minioProperties,
            ObjectNameGenerator objectNameGenerator,
            ObjectMapper objectMapper
    ) {
        this.minioClient = minioClient;
        this.minioProperties = minioProperties;
        this.objectNameGenerator = objectNameGenerator;
        this.objectMapper = objectMapper;
    }

    @Override
    public void save(FailedEvent failedEvent) {
        boolean isValidJson = isValidJson(failedEvent.payload());
        String contentType = isValidJson(failedEvent.payload()) ? JSON_CONTENT_TYPE : TEXT_CONTENT_TYPE;
        String extension = isValidJson ? "json" : "txt";
        byte[] data = failedEvent.payload().getBytes();

        String objectName = objectNameGenerator.toObjectName(failedEvent, extension);

        try {
            log.debug("Uploading object to MinIO: {}", objectName);

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioProperties.bucket())
                            .object(objectName)
                            .stream(new ByteArrayInputStream(data), data.length, -1)
                            .contentType(contentType)
                            .build()
            );

            log.debug("Successfully uploaded object to MinIO: {}", objectName);
        } catch (Exception e) {
            log.error("Failed to upload object to MinIO: {}", objectName, e);
            throw new FailedEventStorageException("Failed to store failed event", e);
        }
    }

    private boolean isValidJson(String payload) {
        if (payload == null || payload.isBlank()) {
            return false;
        }

        try {
            objectMapper.readTree(payload);
            return true;
        } catch (Exception e) {
            log.debug("Payload is not valid JSON: {}", e.getMessage());
            return false;
        }
    }

}
