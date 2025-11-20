/*
 * Source TS: ScrawlCondition.ts
 * Original TS: 'interface CondVarEquals'
 */
package de.mhus.nimbus.generated.scrawl;

@lombok.Data
@lombok.Builder
public class CondVarEquals extends Object {
    private String kind;
    private String name;
    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    private Object value;
}
