/*
 * Source TS: ScrawlScript.ts
 * Original TS: 'interface ScrawlSequence'
 */
package de.mhus.nimbus.evaluate.generated.scrawl;

@Deprecated
@lombok.Data
@lombok.experimental.SuperBuilder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class ScrawlSequence {
    @Deprecated
    @SuppressWarnings("required")
    private String name;
    @Deprecated
    @SuppressWarnings("required")
    private ScrawlStep step;
    @Deprecated
    @SuppressWarnings("optional")
    private String description;
}
