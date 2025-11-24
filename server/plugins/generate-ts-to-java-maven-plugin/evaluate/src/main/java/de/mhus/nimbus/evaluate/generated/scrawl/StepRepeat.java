/*
 * Source TS: ScrawlStep.ts
 * Original TS: 'interface StepRepeat'
 */
package de.mhus.nimbus.evaluate.generated.scrawl;

@Deprecated
@lombok.Data
@lombok.experimental.SuperBuilder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class StepRepeat {
    @Deprecated
    @SuppressWarnings("required")
    private String kind;
    @Deprecated
    @SuppressWarnings("optional")
    private Object times;
    @Deprecated
    @SuppressWarnings("optional")
    private String untilEvent;
    @Deprecated
    @SuppressWarnings("required")
    private ScrawlStep step;
}
