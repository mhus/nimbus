/*
 * Source TS: ScrawlStep.ts
 * Original TS: 'interface StepCmd'
 */
package de.mhus.nimbus.evaluate.generated.scrawl;

@lombok.Data
@lombok.experimental.SuperBuilder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class StepCmd {
    private String kind;
    private String cmd;
    private java.util.List<Object> parameters;
}
