/*
 * Source TS: ScrawlStep.ts
 * Original TS: 'interface StepSequence'
 */
package de.mhus.nimbus.generated.scrawl;

@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
@lombok.Data
@lombok.experimental.SuperBuilder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class StepSequence {
    private String kind;
    private java.util.List<ScrawlStep> steps;
}
