package de.mhus.nimbus.generated.network;

/**
 * Generated from MessageTypes.ts
 * DO NOT EDIT MANUALLY - This file is auto-generated
 */
public enum ClientType {
    WEB("web"),
    XBOX("xbox"),
    MOBILE("mobile"),
    DESKTOP("desktop");

    private final String value;

    ClientType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
