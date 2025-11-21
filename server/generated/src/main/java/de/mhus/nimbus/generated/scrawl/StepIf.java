/*
 * Source TS: ScrawlStep.ts
 * Original TS: 'interface StepIf'
 */
package de.mhus.nimbus.generated.scrawl;

@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
@lombok.Data
@lombok.Builder
public class StepIf extends Object {
    private String kind;
    private ScrawlCondition cond;
    private ScrawlStep then;
}
