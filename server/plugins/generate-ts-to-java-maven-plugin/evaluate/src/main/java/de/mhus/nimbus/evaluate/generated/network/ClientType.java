/*
 * Source TS: MessageTypes.ts
 * Original TS: 'enum ClientType'
 */
package de.mhus.nimbus.evaluate.generated.network;

public enum ClientType {
    WEB("web"),
    XBOX("xbox"),
    MOBILE("mobile"),
    DESKTOP("desktop");

    @lombok.Getter
    private final String tsIndex;
    ClientType(String tsIndex) { this.tsIndex = tsIndex; }
}
