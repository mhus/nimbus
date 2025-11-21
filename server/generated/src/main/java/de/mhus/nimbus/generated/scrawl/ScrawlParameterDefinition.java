/*
 * Source TS: ScrawlScript.ts
 * Original TS: 'interface ScrawlParameterDefinition'
 */
package de.mhus.nimbus.generated.scrawl;

@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
@lombok.Data
@lombok.Builder
public class ScrawlParameterDefinition extends Object {
    private String name;
    private ScrawlParameterType type;
    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    private String description;
}
