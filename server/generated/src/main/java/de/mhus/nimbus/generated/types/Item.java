/*
 * Source TS: Item.ts
 * Original TS: 'interface Item'
 */
package de.mhus.nimbus.generated.types;

@lombok.Data
@lombok.Builder
public class Item extends Object {
    private String id;
    private String itemType;
    private String name;
    private String description;
    private ItemModifier modifier;
    private java.util.Map<String, Object> parameters;
}
