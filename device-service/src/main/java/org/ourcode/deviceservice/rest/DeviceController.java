package org.ourcode.deviceservice.rest;

import org.ourcode.device.api.*;
import org.ourcode.device.model.Device;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class DeviceController implements DeviceApi {

    @Override
    public ResponseEntity<Device> addDevice(Device device) {
        return ResponseEntity.notFound().build();
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
    public ResponseEntity<Device> updateDevice(Long deviceId, String type, Map<String, Object> meta) {
        return ResponseEntity.notFound().build();
    }

}
