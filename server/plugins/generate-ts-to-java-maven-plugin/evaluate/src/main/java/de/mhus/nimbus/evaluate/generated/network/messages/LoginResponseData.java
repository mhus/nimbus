/*
 * Source TS: LoginMessage.ts
 * Original TS: 'interface LoginResponseData'
 */
package de.mhus.nimbus.evaluate.generated.network.messages;

@lombok.Data
@lombok.experimental.SuperBuilder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class LoginResponseData {
    private boolean success;
    private String userId;
    private String displayName;
    private String sessionId;
}
