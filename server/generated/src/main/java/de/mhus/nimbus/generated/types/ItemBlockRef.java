/*
 * Source TS: ItemBlockRef.ts
 * Original TS: 'interface ItemBlockRef'
 */
package de.mhus.nimbus.generated.types;

@lombok.Data
@lombok.Builder
public class ItemBlockRef extends Object {
    private String id;
    private Vector3 position;
    private String texture;
    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    private java.lang.Double scaleX;
    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    private java.lang.Double scaleY;
    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    private java.util.List<Object> offset;
}
