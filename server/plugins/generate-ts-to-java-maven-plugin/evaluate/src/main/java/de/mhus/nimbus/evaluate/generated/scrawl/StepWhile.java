/*
 * Source TS: ScrawlStep.ts
 * Original TS: 'interface StepWhile'
 */
package de.mhus.nimbus.evaluate.generated.scrawl;

@Deprecated
@lombok.Data
@lombok.experimental.SuperBuilder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class StepWhile {
    @Deprecated
    @SuppressWarnings("required")
    private String kind;
    @Deprecated
    @SuppressWarnings("required")
    private String taskId;
    @Deprecated
    @SuppressWarnings("required")
    private ScrawlStep step;
    @Deprecated
    @SuppressWarnings("optional")
    private java.lang.Double timeout;
}
