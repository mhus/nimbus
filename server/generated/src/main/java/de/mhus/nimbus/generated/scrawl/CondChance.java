/*
 * Source TS: ScrawlCondition.ts
 * Original TS: 'interface CondChance'
 */
package de.mhus.nimbus.generated.scrawl;

@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
@lombok.Data
@lombok.Builder
public class CondChance extends Object {
    private String kind;
    private double p;
}
