/*
 * Source TS: ShortcutDefinition.ts
 * Original TS: 'interface ShortcutDefinition'
 */
package de.mhus.nimbus.evaluate.generated.types;

@Deprecated
@lombok.Data
@lombok.experimental.SuperBuilder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class ShortcutDefinition {
    @Deprecated
    @SuppressWarnings("required")
    private ShortcutActionType type;
    @Deprecated
    @SuppressWarnings("optional")
    private String itemId;
    @Deprecated
    @SuppressWarnings("optional")
    private String name;
    @Deprecated
    @SuppressWarnings("optional")
    private String description;
}
