/*
 * Source TS: ScrawlStep.ts
 * Original TS: 'interface StepEmitEvent'
 */
package de.mhus.nimbus.evaluate.generated.scrawl;

@lombok.Data
@lombok.experimental.SuperBuilder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class StepEmitEvent {
    private String kind;
    private String name;
    private Object payload;
}
