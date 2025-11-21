/*
 * Source TS: ScrawlStep.ts
 * Original TS: 'interface StepCmd'
 */
package de.mhus.nimbus.generated.scrawl;

@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
@lombok.Data
@lombok.Builder
public class StepCmd extends Object {
    private String kind;
    private String cmd;
    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    private java.util.List<Object> parameters;
}
