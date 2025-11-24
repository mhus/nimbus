/*
 * Source TS: ScrawlStep.ts
 * Original TS: 'interface StepWaitEvent'
 */
package de.mhus.nimbus.evaluate.generated.scrawl;

@Deprecated
@lombok.Data
@lombok.experimental.SuperBuilder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class StepWaitEvent {
    @Deprecated
    @SuppressWarnings("required")
    private String kind;
    @Deprecated
    @SuppressWarnings("required")
    private String name;
    @Deprecated
    @SuppressWarnings("optional")
    private java.lang.Double timeout;
}
