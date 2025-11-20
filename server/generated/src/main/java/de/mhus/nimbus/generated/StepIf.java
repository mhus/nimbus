package de.mhus.nimbus.generated;

@lombok.Data
@lombok.Builder
public class StepIf extends Object {
    private String kind;
    private ScrawlCondition cond;
    private ScrawlStep then;
}
