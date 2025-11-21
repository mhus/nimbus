/*
 * Source TS: ScrawlStep.ts
 * Original TS: 'interface StepSequence'
 */
package de.mhus.nimbus.generated.scrawl;

@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
@lombok.Data
@lombok.Builder
public class StepSequence extends Object {
    private String kind;
    private java.util.List<ScrawlStep> steps;
}
