package de.mhus.nimbus.generated.scrawl;

@lombok.Data
@lombok.Builder
public class StepIf extends Object {
    private String kind;
    private ScrawlCondition cond;
    private ScrawlStep then;
}
