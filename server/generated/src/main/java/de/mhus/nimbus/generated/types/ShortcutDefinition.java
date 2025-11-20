/*
 * Source TS: ShortcutDefinition.ts
 * Original TS: 'interface ShortcutDefinition'
 */
package de.mhus.nimbus.generated.types;

@lombok.Data
@lombok.Builder
public class ShortcutDefinition extends Object {
    private ShortcutActionType type;
    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    private String itemId;
    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    private String name;
    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    private String description;
}
