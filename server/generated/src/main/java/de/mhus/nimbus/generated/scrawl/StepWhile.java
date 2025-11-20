/*
 * Source TS: ScrawlStep.ts
 * Original TS: 'interface StepWhile'
 */
package de.mhus.nimbus.generated.scrawl;

@lombok.Data
@lombok.Builder
public class StepWhile extends Object {
    private String kind;
    private String taskId;
    private ScrawlStep step;
    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    private java.lang.Double timeout;
}
