/*
 * Source TS: ScrawlStep.ts
 * Original TS: 'interface StepForEach'
 */
package de.mhus.nimbus.evaluate.generated.scrawl;

@lombok.Data
@lombok.experimental.SuperBuilder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class StepForEach {
    private String kind;
    private String collection;
    private String itemVar;
    private ScrawlStep step;
}
