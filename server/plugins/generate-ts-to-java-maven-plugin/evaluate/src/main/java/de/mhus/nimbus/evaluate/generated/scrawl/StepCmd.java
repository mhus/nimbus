/*
 * Source TS: ScrawlStep.ts
 * Original TS: 'interface StepCmd'
 */
package de.mhus.nimbus.evaluate.generated.scrawl;

@Deprecated
@lombok.Data
@lombok.experimental.SuperBuilder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class StepCmd {
    @Deprecated
    @SuppressWarnings("required")
    private String kind;
    @Deprecated
    @SuppressWarnings("required")
    private String cmd;
    @Deprecated
    @SuppressWarnings("optional")
    private java.util.List<Object> parameters;
}
