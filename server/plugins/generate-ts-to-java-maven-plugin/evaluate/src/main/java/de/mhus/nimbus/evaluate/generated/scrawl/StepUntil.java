/*
 * Source TS: ScrawlStep.ts
 * Original TS: 'interface StepUntil'
 */
package de.mhus.nimbus.evaluate.generated.scrawl;

@Deprecated
@lombok.Data
@lombok.experimental.SuperBuilder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class StepUntil {
    @Deprecated
    @SuppressWarnings("required")
    private String kind;
    @Deprecated
    @SuppressWarnings("required")
    private String event;
    @Deprecated
    @SuppressWarnings("required")
    private ScrawlStep step;
    @Deprecated
    @SuppressWarnings("optional")
    private java.lang.Double timeout;
}
