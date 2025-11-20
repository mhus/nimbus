package de.mhus.nimbus.generated.types;

public interface WorldInfo {
    String getWorldId();
    String getName();
    String getDescription();
    Vector3 getStart();
    Vector3 getStop();
    java.lang.Double getChunkSize();
    String getAssetPath();
    java.lang.Double getAssetPort();
    String getWorldGroupId();
    java.lang.Double getStatus();
    String getCreatedAt();
    String getUpdatedAt();
    String getUser();
    String getDisplayName();
    String getEmail();
    double getMaxPlayers();
    boolean getAllowGuests();
    boolean getPvpEnabled();
    double getPingInterval();
    String getDeadAmbientAudio();
    String getSwimStepAudio();
    String getType();
    String getExpiresAt();
    double getX();
    double getY();
    double getZ();
    double getRadius();
    double getRotation();
    String getEditorUrl();
}
