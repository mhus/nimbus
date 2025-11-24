/*
 * Source TS: LoginMessage.ts
 * Original TS: 'interface LoginResponseData'
 */
package de.mhus.nimbus.evaluate.generated.network.messages;

@Deprecated
@lombok.Data
@lombok.experimental.SuperBuilder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class LoginResponseData {
    @Deprecated
    @SuppressWarnings("required")
    private boolean success;
    @Deprecated
    @SuppressWarnings("required")
    private String userId;
    @Deprecated
    @SuppressWarnings("required")
    private String displayName;
    @Deprecated
    @SuppressWarnings("required")
    private String sessionId;
}
