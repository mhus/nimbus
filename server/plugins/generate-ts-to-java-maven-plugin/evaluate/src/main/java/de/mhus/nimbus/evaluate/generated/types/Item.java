/*
 * Source TS: Item.ts
 * Original TS: 'interface Item'
 */
package de.mhus.nimbus.evaluate.generated.types;

@Deprecated
@lombok.Data
@lombok.experimental.SuperBuilder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class Item {
    @Deprecated
    @SuppressWarnings("required")
    private String id;
    @Deprecated
    @SuppressWarnings("required")
    private String itemType;
    @Deprecated
    @SuppressWarnings("optional")
    private String name;
    @Deprecated
    @SuppressWarnings("optional")
    private String description;
    @Deprecated
    @SuppressWarnings("optional")
    private ItemModifier modifier;
    @Deprecated
    @SuppressWarnings("optional")
    private java.util.Map<String, Object> parameters;
}
