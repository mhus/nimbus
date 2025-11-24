/*
 * Source TS: BlockModifier.ts
 * Original TS: 'interface AudioDefinition'
 */
package de.mhus.nimbus.evaluate.generated.types;

@Deprecated
@lombok.Data
@lombok.experimental.SuperBuilder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class AudioDefinition {
    @Deprecated
    @SuppressWarnings("required")
    private Object type;
    @Deprecated
    @SuppressWarnings("required")
    private String path;
    @Deprecated
    @SuppressWarnings("required")
    private double volume;
    @Deprecated
    @SuppressWarnings("optional")
    private java.lang.Boolean loop;
    @Deprecated
    @SuppressWarnings("required")
    private boolean enabled;
    @Deprecated
    @SuppressWarnings("optional")
    private java.lang.Double maxDistance;
}
