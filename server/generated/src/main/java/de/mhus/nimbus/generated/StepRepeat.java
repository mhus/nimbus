package de.mhus.nimbus.generated;

@lombok.Data
@lombok.Builder
public class StepRepeat extends Object {
    private String kind;
    private Object times;
    private Object untilEvent;
    private ScrawlStep step;
}
