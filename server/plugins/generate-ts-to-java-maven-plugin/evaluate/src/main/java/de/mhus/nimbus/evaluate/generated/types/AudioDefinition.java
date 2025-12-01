/*
 * Source TS: BlockModifier.ts
 * Original TS: 'interface AudioDefinition'
 */
package de.mhus.nimbus.evaluate.generated.types;

@lombok.Data
@lombok.experimental.SuperBuilder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class AudioDefinition {
    private Object type;
    private String path;
    private double volume;
    private java.lang.Boolean loop;
    private boolean enabled;
    private java.lang.Double maxDistance;
}
