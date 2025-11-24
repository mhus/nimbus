/*
 * Source TS: ScrawlStep.ts
 * Original TS: 'interface StepIf'
 */
package de.mhus.nimbus.evaluate.generated.scrawl;

@Deprecated
@lombok.Data
@lombok.experimental.SuperBuilder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class StepIf {
    @Deprecated
    @SuppressWarnings("required")
    private String kind;
    @Deprecated
    @SuppressWarnings("required")
    private ScrawlCondition cond;
    @Deprecated
    @SuppressWarnings("required")
    private ScrawlStep then;
}
