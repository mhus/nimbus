/*
 * Source TS: ScrawlCondition.ts
 * Original TS: 'interface CondIsVarFalse'
 */
package de.mhus.nimbus.generated.scrawl;

@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
@lombok.Data
@lombok.Builder
public class CondIsVarFalse extends Object {
    private String kind;
    private String name;
    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    private java.lang.Boolean defaultValue;
}
