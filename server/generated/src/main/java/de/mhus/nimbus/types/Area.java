/*
 * Source TS: Area.ts
 * Original TS: 'interface Area'
 */
package de.mhus.nimbus.types;

@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
@org.springframework.aot.hint.annotation.Reflective
@lombok.Data
@lombok.experimental.SuperBuilder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class Area {
    private Vector3 a;
    private Vector3 b;
}
