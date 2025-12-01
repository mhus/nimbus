/*
 * Source TS: Item.ts
 * Original TS: 'interface Item'
 */
package de.mhus.nimbus.evaluate.generated.types;

@lombok.Data
@lombok.experimental.SuperBuilder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class Item {
    private String id;
    private String itemType;
    private String name;
    private String description;
    private ItemModifier modifier;
    private java.util.Map<String, Object> parameters;
}
