/*
 * Source TS: ScrawlStep.ts
 * Original TS: 'interface StepCall'
 */
package de.mhus.nimbus.generated.scrawl;

@lombok.Data
@lombok.Builder
public class StepCall extends Object {
    private String kind;
    private String scriptId;
    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    private java.util.Map<String, Object> args;
}
