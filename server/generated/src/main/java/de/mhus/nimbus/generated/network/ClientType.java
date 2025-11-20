/*
 * Source TS: MessageTypes.ts
 * Original TS: 'enum ClientType'
 */
package de.mhus.nimbus.generated.network;

public enum ClientType {
    WEB(1),
    XBOX(2),
    MOBILE(3),
    DESKTOP(4);

    @lombok.Getter
    private final int tsIndex;
    ClientType(int tsIndex) { this.tsIndex = tsIndex; }
}
