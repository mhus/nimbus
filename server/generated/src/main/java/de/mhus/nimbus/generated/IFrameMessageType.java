package de.mhus.nimbus.generated;

/**
 * Generated from Modal.ts
 * DO NOT EDIT MANUALLY - This file is auto-generated
 */
public enum IFrameMessageType {
    IFRAME_READY("IFRAME_READY"),
    REQUEST_CLOSE("REQUEST_CLOSE"),
    REQUEST_POSITION_CHANGE("REQUEST_POSITION_CHANGE"),
    NOTIFICATION("NOTIFICATION");

    private final String value;

    IFrameMessageType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
