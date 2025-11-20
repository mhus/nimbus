/*
 * Source TS: ScrawlStep.ts
 * Original TS: 'interface StepUntil'
 */
package de.mhus.nimbus.generated.scrawl;

@lombok.Data
@lombok.Builder
public class StepUntil extends Object {
    private String kind;
    private String event;
    private ScrawlStep step;
    private java.lang.Double timeout;
}
