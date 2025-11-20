/*
 * Source TS: ScrawlStep.ts
 * Original TS: 'interface StepForEach'
 */
package de.mhus.nimbus.generated.scrawl;

@lombok.Data
@lombok.Builder
public class StepForEach extends Object {
    private String kind;
    private String collection;
    private String itemVar;
    private ScrawlStep step;
}
