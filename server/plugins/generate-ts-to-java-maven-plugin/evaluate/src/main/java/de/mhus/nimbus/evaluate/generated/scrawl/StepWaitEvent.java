/*
 * Source TS: ScrawlStep.ts
 * Original TS: 'interface StepWaitEvent'
 */
package de.mhus.nimbus.evaluate.generated.scrawl;

@lombok.Data
@lombok.experimental.SuperBuilder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class StepWaitEvent {
    private String kind;
    private String name;
    private java.lang.Double timeout;
}
