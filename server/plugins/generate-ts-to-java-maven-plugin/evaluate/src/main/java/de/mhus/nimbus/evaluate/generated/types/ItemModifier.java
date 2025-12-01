/*
 * Source TS: ItemModifier.ts
 * Original TS: 'interface ItemModifier'
 */
package de.mhus.nimbus.evaluate.generated.types;

@lombok.Data
@lombok.experimental.SuperBuilder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class ItemModifier {
    private String texture;
    private java.lang.Double scaleX;
    private java.lang.Double scaleY;
    private java.util.List<Object> offset;
    private String color;
    private String pose;
    private de.mhus.nimbus.evaluate.generated.scrawl.ScriptActionDefinition onUseEffect;
    private java.lang.Boolean exclusive;
    private de.mhus.nimbus.evaluate.generated.scrawl.ScriptActionDefinition actionScript;
}
