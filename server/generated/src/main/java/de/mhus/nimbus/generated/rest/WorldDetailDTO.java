package de.mhus.nimbus.generated.rest;

public interface WorldDetailDTO {
    String getWorldId();
    String getName();
    String getDescription();
    Position3D getStart();
    Position3D getStop();
    double getChunkSize();
    String getAssetPath();
    java.lang.Double getAssetPort();
    String getWorldGroupId();
    String getCreatedAt();
    String getUpdatedAt();
    UserDTO getOwner();
    WorldSettingsDTO getSettings();
}
