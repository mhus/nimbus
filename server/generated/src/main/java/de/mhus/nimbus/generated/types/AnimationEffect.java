package de.mhus.nimbus.generated.types;

public interface AnimationEffect {
    String getId();
    AnimationEffectType getType();
    java.util.List<PositionRef> getPositions();
    Object getFrom();
    Object getTo();
    EasingType getEasing();
    java.lang.Double getSpeed();
    String getProjectileModel();
    String getTrajectory();
    java.lang.Double getRadius();
    java.lang.Double getExplosionIntensity();
    String getColor();
    java.lang.Double getLightIntensity();
    String getSoundPath();
    java.lang.Double getVolume();
    double getStartTime();
    java.lang.Double getDuration();
    java.lang.Double getEndTime();
    java.lang.Boolean getBlocking();
}
