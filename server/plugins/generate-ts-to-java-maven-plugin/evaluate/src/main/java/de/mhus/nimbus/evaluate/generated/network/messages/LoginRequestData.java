/*
 * Source TS: LoginMessage.ts
 * Original TS: 'interface LoginRequestData'
 */
package de.mhus.nimbus.evaluate.generated.network.messages;

@Deprecated
@lombok.Data
@lombok.experimental.SuperBuilder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class LoginRequestData {
    @Deprecated
    @SuppressWarnings("optional")
    private String username;
    @Deprecated
    @SuppressWarnings("optional")
    private String password;
    @Deprecated
    @SuppressWarnings("optional")
    private String token;
    @Deprecated
    @SuppressWarnings("required")
    private String worldId;
    @Deprecated
    @SuppressWarnings("required")
    private de.mhus.nimbus.evaluate.generated.network.ClientType clientType;
    @Deprecated
    @SuppressWarnings("optional")
    private String sessionId;
}
