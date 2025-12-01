/*
 * Source TS: ItemBlockRef.ts
 * Original TS: 'interface ItemBlockRef'
 */
package de.mhus.nimbus.evaluate.generated.types;

@lombok.Data
@lombok.experimental.SuperBuilder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class ItemBlockRef {
    private String id;
    private Vector3 position;
    private String texture;
    private java.lang.Double scaleX;
    private java.lang.Double scaleY;
    private java.util.List<Object> offset;
}
