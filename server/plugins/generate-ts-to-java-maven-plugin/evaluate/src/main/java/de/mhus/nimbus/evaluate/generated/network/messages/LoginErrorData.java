/*
 * Source TS: LoginMessage.ts
 * Original TS: 'interface LoginErrorData'
 */
package de.mhus.nimbus.evaluate.generated.network.messages;

@Deprecated
@lombok.Data
@lombok.experimental.SuperBuilder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class LoginErrorData {
    @Deprecated
    @SuppressWarnings("required")
    private boolean success;
    @Deprecated
    @SuppressWarnings("required")
    private double errorCode;
    @Deprecated
    @SuppressWarnings("required")
    private String errorMessage;
}
