/*
 * Source TS: ScrawlScript.ts
 * Original TS: 'interface ScrawlSequence'
 */
package de.mhus.nimbus.generated.scrawl;

@lombok.Data
@lombok.Builder
public class ScrawlSequence extends Object {
    private String name;
    private ScrawlStep step;
    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    private String description;
}
