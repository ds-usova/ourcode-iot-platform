package org.ourcode.deviceservice.rest.config;

public enum Roles {

    DEVICE_READ("device-reader"),
    DEVICE_WRITE("device-writer");

    private final String roleName;

    Roles(String roleName) {
        this.roleName = roleName;
    }

    public String getRoleName() {
        return roleName;
    }

}
