/*
 * Source TS: LoginMessage.ts
 * Original TS: 'interface LoginErrorData'
 */
package de.mhus.nimbus.generated.network.messages;

@lombok.Data
@lombok.Builder
public class LoginErrorData extends Object {
    private boolean success;
    private double errorCode;
    private String errorMessage;
}
