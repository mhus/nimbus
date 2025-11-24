/*
 * Source TS: ScrawlStep.ts
 * Original TS: 'interface StepCall'
 */
package de.mhus.nimbus.evaluate.generated.scrawl;

@Deprecated
@lombok.Data
@lombok.experimental.SuperBuilder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class StepCall {
    @Deprecated
    @SuppressWarnings("required")
    private String kind;
    @Deprecated
    @SuppressWarnings("required")
    private String scriptId;
    @Deprecated
    @SuppressWarnings("optional")
    private java.util.Map<String, Object> args;
}
