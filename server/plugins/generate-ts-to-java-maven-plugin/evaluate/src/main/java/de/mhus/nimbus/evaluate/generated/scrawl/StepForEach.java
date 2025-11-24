/*
 * Source TS: ScrawlStep.ts
 * Original TS: 'interface StepForEach'
 */
package de.mhus.nimbus.evaluate.generated.scrawl;

@Deprecated
@lombok.Data
@lombok.experimental.SuperBuilder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class StepForEach {
    @Deprecated
    @SuppressWarnings("required")
    private String kind;
    @Deprecated
    @SuppressWarnings("required")
    private String collection;
    @Deprecated
    @SuppressWarnings("required")
    private String itemVar;
    @Deprecated
    @SuppressWarnings("required")
    private ScrawlStep step;
}
