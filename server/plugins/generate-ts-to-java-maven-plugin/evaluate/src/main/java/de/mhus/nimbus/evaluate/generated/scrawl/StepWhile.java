/*
 * Source TS: ScrawlStep.ts
 * Original TS: 'interface StepWhile'
 */
package de.mhus.nimbus.evaluate.generated.scrawl;

@lombok.Data
@lombok.experimental.SuperBuilder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class StepWhile {
    private String kind;
    private String taskId;
    private ScrawlStep step;
    private java.lang.Double timeout;
}
