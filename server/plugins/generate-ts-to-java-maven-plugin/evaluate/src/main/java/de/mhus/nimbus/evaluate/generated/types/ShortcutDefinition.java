/*
 * Source TS: ShortcutDefinition.ts
 * Original TS: 'interface ShortcutDefinition'
 */
package de.mhus.nimbus.evaluate.generated.types;

@lombok.Data
@lombok.experimental.SuperBuilder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class ShortcutDefinition {
    private ShortcutActionType type;
    private String itemId;
    private String name;
    private String description;
}
