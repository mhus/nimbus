/*
 * Source TS: ScrawlStep.ts
 * Original TS: 'interface StepUntil'
 */
package de.mhus.nimbus.evaluate.generated.scrawl;

@lombok.Data
@lombok.experimental.SuperBuilder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class StepUntil {
    private String kind;
    private String event;
    private ScrawlStep step;
    private java.lang.Double timeout;
}
