/*
 * Source TS: ItemBlockRef.ts
 * Original TS: 'interface ItemBlockRef'
 */
package de.mhus.nimbus.evaluate.generated.types;

@Deprecated
@lombok.Data
@lombok.experimental.SuperBuilder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class ItemBlockRef {
    @Deprecated
    @SuppressWarnings("required")
    private String id;
    @Deprecated
    @SuppressWarnings("required")
    private Vector3 position;
    @Deprecated
    @SuppressWarnings("required")
    private String texture;
    @Deprecated
    @SuppressWarnings("optional")
    private java.lang.Double scaleX;
    @Deprecated
    @SuppressWarnings("optional")
    private java.lang.Double scaleY;
    @Deprecated
    @SuppressWarnings("optional")
    private java.util.List<Object> offset;
}
