/*
 * Source TS: Modal.ts
 * Original TS: 'enum IFrameMessageType'
 */
package de.mhus.nimbus.evaluate.generated.types;

public enum IFrameMessageType {
    IFRAME_READY(1),
    REQUEST_CLOSE(2),
    REQUEST_POSITION_CHANGE(3),
    NOTIFICATION(4);

    @lombok.Getter
    private final int tsIndex;
    IFrameMessageType(int tsIndex) { this.tsIndex = tsIndex; }
}
