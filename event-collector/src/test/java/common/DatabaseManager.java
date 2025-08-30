package common;

import org.ourcode.eventcollector.cassandra.DeviceEventRepository;

public class DatabaseManager {

    private final DeviceEventRepository deviceEventRepository;

    public DatabaseManager(DeviceEventRepository deviceEventRepository) {
        this.deviceEventRepository = deviceEventRepository;
    }

    public void cleanUp() {
        deviceEventRepository.deleteAll();
    }

}
