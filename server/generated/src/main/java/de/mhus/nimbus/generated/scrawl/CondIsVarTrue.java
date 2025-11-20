/*
 * Source TS: ScrawlCondition.ts
 * Original TS: 'interface CondIsVarTrue'
 */
package de.mhus.nimbus.generated.scrawl;

@lombok.Data
@lombok.Builder
public class CondIsVarTrue extends Object {
    private String kind;
    private String name;
    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    private java.lang.Boolean defaultValue;
}
