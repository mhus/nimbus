/*
 * Source TS: ScrawlStep.ts
 * Original TS: 'interface StepLodSwitch'
 */
package de.mhus.nimbus.evaluate.generated.scrawl;

@Deprecated
@lombok.Data
@lombok.experimental.SuperBuilder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class StepLodSwitch {
    @Deprecated
    @SuppressWarnings("required")
    private String kind;
    @Deprecated
    @SuppressWarnings("required")
    private java.util.Map<ScrawlLodLevel, ScrawlStep> levels;
}
