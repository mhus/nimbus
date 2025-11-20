package de.mhus.nimbus.generated.network;

@lombok.Data
@lombok.Builder
public class LoginRequestData extends Object {
    private String username;
    private String password;
    private String token;
    private String worldId;
    private ClientType clientType;
    private String sessionId;
}
