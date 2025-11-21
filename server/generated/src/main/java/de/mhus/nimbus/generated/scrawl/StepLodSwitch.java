/*
 * Source TS: ScrawlStep.ts
 * Original TS: 'interface StepLodSwitch'
 */
package de.mhus.nimbus.generated.scrawl;

@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
@lombok.Data
@lombok.Builder
public class StepLodSwitch extends Object {
    private String kind;
    private java.util.Map<ScrawlLodLevel, ScrawlStep> levels;
}
