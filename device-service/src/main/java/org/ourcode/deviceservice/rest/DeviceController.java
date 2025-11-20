package org.ourcode.deviceservice.rest;

import lombok.extern.slf4j.Slf4j;
import org.ourcode.deviceservice.api.service.DeviceService;
import org.ourcode.rest.api.DeviceApi;
import org.ourcode.rest.model.Device;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import static org.ourcode.deviceservice.rest.DeviceUtils.*;

@Slf4j
@RestController
public class DeviceController implements DeviceApi {

    private final DeviceService deviceService;

    public DeviceController(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    @Override
    public ResponseEntity<Device> addDevice(Device device) {
        log.debug("Adding device {}", device);
        var created = deviceService.addDevice( toApiModel(device) );

        return ResponseEntity.created( toUri(created.id()) )
                             .body( toRestModel(created) );
    }

    @Override
    public ResponseEntity<Void> deleteDevice(Long deviceId) {
        return ResponseEntity.notFound().build();
    }

    @Override
    public ResponseEntity<Device> getDeviceById(Long deviceId) {
        return ResponseEntity.notFound().build();
    }

    @Override
    public ResponseEntity<Device> updateDevice(Long deviceId, String type, String meta) {
        return ResponseEntity.notFound().build();
    }

}
