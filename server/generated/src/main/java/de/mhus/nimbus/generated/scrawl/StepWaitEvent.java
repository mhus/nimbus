/*
 * Source TS: ScrawlStep.ts
 * Original TS: 'interface StepWaitEvent'
 */
package de.mhus.nimbus.generated.scrawl;

@lombok.Data
@lombok.Builder
public class StepWaitEvent extends Object {
    private String kind;
    private String name;
    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    private java.lang.Double timeout;
}
