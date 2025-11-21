/*
 * Source TS: LoginMessage.ts
 * Original TS: 'interface LoginResponseData'
 */
package de.mhus.nimbus.generated.network.messages;

@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
@lombok.Data
@lombok.Builder
public class LoginResponseData extends Object {
    private boolean success;
    private String userId;
    private String displayName;
    private String sessionId;
}
