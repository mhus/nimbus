/*
 * Source TS: LoginMessage.ts
 * Original TS: 'interface LoginRequestData'
 */
package de.mhus.nimbus.evaluate.generated.network.messages;

@lombok.Data
@lombok.experimental.SuperBuilder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class LoginRequestData {
    private String username;
    private String password;
    private String token;
    private String worldId;
    private de.mhus.nimbus.evaluate.generated.network.ClientType clientType;
    private String sessionId;
}
