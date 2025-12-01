/*
 * Source TS: ScrawlStep.ts
 * Original TS: 'interface StepCall'
 */
package de.mhus.nimbus.evaluate.generated.scrawl;

@lombok.Data
@lombok.experimental.SuperBuilder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class StepCall {
    private String kind;
    private String scriptId;
    private java.util.Map<String, Object> args;
}
