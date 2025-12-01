/*
 * Source TS: TargetingTypes.ts
 * Original TS: 'interface ClientBlock'
 */
package de.mhus.nimbus.types;

@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
@org.springframework.aot.hint.annotation.Reflective
@lombok.Data
@lombok.experimental.SuperBuilder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class ClientBlock {
    private java.util.Map<String, Object> block;
    private Object blockType;
}
