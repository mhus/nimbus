package de.mhus.nimbus.generated.network.messages;

@lombok.Data
@lombok.Builder
public class LoginResponseData extends Object {
    private boolean success;
    private String userId;
    private String displayName;
    private String sessionId;
}
