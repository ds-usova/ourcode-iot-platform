package common;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.minio.*;
import io.minio.messages.Item;
import lombok.extern.slf4j.Slf4j;
import org.ourcode.failedevents.minio.configuration.MinioProperties;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
public class TestMinioClient {

    private final MinioClient minioClient;
    private final MinioProperties minioProperties;
    private final ObjectMapper objectMapper;

    public TestMinioClient(MinioClient minioClient, MinioProperties minioProperties, ObjectMapper objectMapper) {
        this.minioClient = minioClient;
        this.minioProperties = minioProperties;
        this.objectMapper = objectMapper;
    }

    private interface MinioOperation<T> {
        T execute() throws Exception;
    }

    private <T> T withErrorHandling(MinioOperation<T> operation, String actionDescription) {
        try {
            return operation.execute();
        } catch (Exception e) {
            log.error("Failed to {} in MinIO", actionDescription, e);
            throw new RuntimeException("Failed to " + actionDescription + " in MinIO", e);
        }
    }

    public List<String> listAllObjects() {
        return listObjects(null);
    }

    /**
     * Lists objects in the configured bucket with optional prefix filtering.
     *
     * @param prefix Optional prefix to filter objects (e.g., "NullPointerException/")
     * @return List of object names (paths)
     */
    public List<String> listObjects(String prefix) {
        return withErrorHandling(() -> {
                    ListObjectsArgs.Builder builder = ListObjectsArgs.builder()
                            .bucket(minioProperties.bucket())
                            .recursive(true);

                    if (prefix != null && !prefix.isEmpty()) {
                        builder.prefix(prefix);
                    }

                    Iterable<Result<Item>> results = minioClient.listObjects(builder.build());

                    List<String> objectNames = new ArrayList<>();
                    for (Result<Item> result : results) {
                        Item item = result.get();
                        objectNames.add(item.objectName());
                    }

                    log.debug("Listed {} objects from bucket '{}' with prefix '{}'", objectNames.size(), minioProperties.bucket(), prefix);
                    return objectNames;
                }, "list objects with prefix '" + prefix + "'"
        );
    }

    public Map<String, Object> readObjectAsJson(String objectName) {
        return withErrorHandling(() -> {
            String content = readObjectAsString(objectName);
            Map<String, Object> json = objectMapper.readValue(content, new TypeReference<>() {});

            log.debug("Parsed object '{}' as JSON with {} keys {}", objectName, json.size(), System.lineSeparator() + json);
            return json;
        }, "parse object '" + objectName + "' as JSON");
    }

    public void deleteAllObjects() {
        withErrorHandling(() -> {
            List<String> allObjects = listAllObjects();

            if (allObjects.isEmpty()) {
                log.debug("No objects to delete from bucket '{}'", minioProperties.bucket());
                return null;
            }

            log.debug("Deleting {} objects from bucket '{}'", allObjects.size(), minioProperties.bucket());
            allObjects.forEach(this::deleteObject);
            return null;
        }, "delete objects");
    }

    private String readObjectAsString(String objectName) {
        return withErrorHandling(() -> {
            InputStream objectStream = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(minioProperties.bucket())
                            .object(objectName)
                            .build()
            );

            String content = new String(objectStream.readAllBytes());
            log.debug("Read object '{}' from bucket '{}', size: {} bytes",
                    objectName, minioProperties.bucket(), content.length());
            return content;
        }, "read object '" + objectName + "'");
    }

    private void deleteObject(String objectName) {
        withErrorHandling(() -> {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(minioProperties.bucket())
                            .object(objectName)
                            .build()
            );
            log.debug("Deleted object '{}' from bucket '{}'", objectName, minioProperties.bucket());
            return null;
        }, "delete object '" + objectName + "'");
    }

}
