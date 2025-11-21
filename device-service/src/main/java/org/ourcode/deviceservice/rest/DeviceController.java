package org.ourcode.deviceservice.rest;

import lombok.extern.slf4j.Slf4j;
import org.ourcode.deviceservice.api.exception.BadRequestException;
import org.ourcode.deviceservice.api.service.DeviceService;
import org.ourcode.rest.api.DeviceApi;
import org.ourcode.rest.model.Device;
import org.ourcode.rest.model.DeviceUpdate;
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
        if (device.getId().isBlank()) {
            throw new BadRequestException("Validation failed: id must not be blank");
        }

        if (device.getType().isBlank()) {
            throw new BadRequestException("Validation failed: type must not be blank");
        }

        log.debug("Adding device {}", device);
        var created = deviceService.addDevice( toApiModel(device) );

        return ResponseEntity.created( toUri(created.id()) )
                             .body( toRestModel(created) );
    }

    @Override
    public ResponseEntity<Void> deleteDevice(String deviceId) {
        return ResponseEntity.notFound().build();
    }

    @Override
    public ResponseEntity<Device> getDeviceById(String deviceId) {
        log.debug("Getting device by ID {}", deviceId);
        var device = deviceService.getDevice(deviceId);

        return ResponseEntity.ok( toRestModel(device) );
    }

    @Override
    public ResponseEntity<Device> updateDevice(String deviceId, DeviceUpdate deviceUpdate) {
        if (deviceUpdate.getMeta() == null && deviceUpdate.getType().isBlank()) {
            throw new BadRequestException("Validation failed: type must not be blank");
        }

        log.debug("Updating device by ID {}", deviceId);
        Device updated = toRestModel( deviceService.updateDevice(deviceId, deviceUpdate.getType(), deviceUpdate.getMeta()) );
        return ResponseEntity.ok(updated);
    }

}
