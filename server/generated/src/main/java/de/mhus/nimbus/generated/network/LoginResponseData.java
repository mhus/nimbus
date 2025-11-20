package de.mhus.nimbus.generated.network;

@lombok.Data
@lombok.Builder
public class LoginResponseData extends Object {
    private boolean success;
    private String userId;
    private String displayName;
    private de.mhus.nimbus.generated.types.WorldInfo worldInfo;
    private String sessionId;
}
