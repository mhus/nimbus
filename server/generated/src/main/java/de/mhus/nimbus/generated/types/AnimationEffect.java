package de.mhus.nimbus.generated.types;

@lombok.Data
@lombok.Builder
public class AnimationEffect extends Object {
    private String id;
    private AnimationEffectType type;
    private java.util.List<PositionRef> positions;
    private Object from;
    private Object to;
    private EasingType easing;
    private java.lang.Double speed;
    private String projectileModel;
    private String trajectory;
    private java.lang.Double radius;
    private java.lang.Double explosionIntensity;
    private String color;
    private java.lang.Double lightIntensity;
    private String soundPath;
    private java.lang.Double volume;
    private double startTime;
    private java.lang.Double duration;
    private java.lang.Double endTime;
    private java.lang.Boolean blocking;
}
