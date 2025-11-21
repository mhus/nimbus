/*
 * Source TS: WorldDTO.ts
 * Original TS: 'interface UserDTO'
 */
package de.mhus.nimbus.generated.rest;

@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
@lombok.Data
@lombok.Builder
public class UserDTO extends Object {
    private String user;
    private String displayName;
    private String email;
}
