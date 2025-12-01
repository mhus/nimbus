/*
 * Source TS: ScrawlStep.ts
 * Original TS: 'interface StepLodSwitch'
 */
package de.mhus.nimbus.evaluate.generated.scrawl;

@lombok.Data
@lombok.experimental.SuperBuilder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class StepLodSwitch {
    private String kind;
    private java.util.Map<ScrawlLodLevel, ScrawlStep> levels;
}
