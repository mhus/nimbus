/*
 * Source TS: LoginMessage.ts
 * Original TS: 'interface LoginRequestData'
 */
package de.mhus.nimbus.generated.network.messages;

@lombok.Data
@lombok.Builder
public class LoginRequestData extends Object {
    private String username;
    private String password;
    private String token;
    private String worldId;
    private de.mhus.nimbus.generated.network.ClientType clientType;
    private String sessionId;
}
