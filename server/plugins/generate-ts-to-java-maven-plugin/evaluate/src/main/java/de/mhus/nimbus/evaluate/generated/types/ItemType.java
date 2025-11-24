/*
 * Source TS: ItemType.ts
 * Original TS: 'interface ItemType'
 */
package de.mhus.nimbus.evaluate.generated.types;

@Deprecated
@lombok.Data
@lombok.experimental.SuperBuilder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class ItemType {
    @Deprecated
    @SuppressWarnings("required")
    private String type;
    @Deprecated
    @SuppressWarnings("required")
    private String name;
    @Deprecated
    @SuppressWarnings("optional")
    private String description;
    @Deprecated
    @SuppressWarnings("required")
    private ItemModifier modifier;
    @Deprecated
    @SuppressWarnings("optional")
    private java.util.Map<String, Object> parameters;
}
