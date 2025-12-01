/*
 * Source TS: ScrawlScript.ts
 * Original TS: 'interface ScrawlSequence'
 */
package de.mhus.nimbus.evaluate.generated.scrawl;

@lombok.Data
@lombok.experimental.SuperBuilder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class ScrawlSequence {
    private String name;
    private ScrawlStep step;
    private String description;
}
