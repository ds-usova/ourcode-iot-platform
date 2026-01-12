package common.containers;

import org.testcontainers.containers.MinIOContainer;

public class MinioContainer {

    private static final String MINIO_IMAGE = "minio/minio:RELEASE.2025-07-23T15-54-02Z";
    public static final org.testcontainers.containers.MinIOContainer CONTAINER;

    static {
        CONTAINER = new MinIOContainer(MINIO_IMAGE);

        CONTAINER.start();
    }

    private MinioContainer() {}

}

