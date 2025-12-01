/*
 * Source TS: ScrawlStep.ts
 * Original TS: 'interface StepRepeat'
 */
package de.mhus.nimbus.evaluate.generated.scrawl;

@lombok.Data
@lombok.experimental.SuperBuilder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class StepRepeat {
    private String kind;
    private Object times;
    private String untilEvent;
    private ScrawlStep step;
}
