/*
 * Source TS: ItemType.ts
 * Original TS: 'interface ItemType'
 */
package de.mhus.nimbus.evaluate.generated.types;

@lombok.Data
@lombok.experimental.SuperBuilder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class ItemType {
    private String type;
    private String name;
    private String description;
    private ItemModifier modifier;
    private java.util.Map<String, Object> parameters;
}
