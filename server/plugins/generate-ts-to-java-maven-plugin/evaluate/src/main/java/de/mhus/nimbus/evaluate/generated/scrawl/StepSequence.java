/*
 * Source TS: ScrawlStep.ts
 * Original TS: 'interface StepSequence'
 */
package de.mhus.nimbus.evaluate.generated.scrawl;

@Deprecated
@lombok.Data
@lombok.experimental.SuperBuilder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class StepSequence {
    @Deprecated
    @SuppressWarnings("required")
    private String kind;
    @Deprecated
    @SuppressWarnings("required")
    private java.util.List<ScrawlStep> steps;
}
