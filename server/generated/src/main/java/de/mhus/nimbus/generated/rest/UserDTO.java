/*
 * Source TS: WorldDTO.ts
 * Original TS: 'interface UserDTO'
 */
package de.mhus.nimbus.generated.rest;

@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
@org.springframework.aot.hint.annotation.Reflective
@lombok.Data
@lombok.experimental.SuperBuilder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class UserDTO {
    private String user;
    @com.fasterxml.jackson.annotation.JsonProperty("displayName")
    private String displayName;
    private String email;
}
