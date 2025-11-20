/*
 * Source TS: ItemType.ts
 * Original TS: 'interface ItemType'
 */
package de.mhus.nimbus.generated.types;

@lombok.Data
@lombok.Builder
public class ItemType extends Object {
    private String type;
    private String name;
    private String description;
    private ItemModifier modifier;
    private java.util.Map<String, Object> parameters;
}
