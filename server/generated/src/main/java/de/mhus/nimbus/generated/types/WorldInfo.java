package de.mhus.nimbus.generated.types;

@lombok.Data
@lombok.Builder
public class WorldInfo extends Object {
    private String worldId;
    private String name;
    private String description;
    private Vector3 start;
    private Vector3 stop;
    private java.lang.Double chunkSize;
    private String assetPath;
    private java.lang.Double assetPort;
    private String worldGroupId;
    private java.lang.Double status;
    private String seasonStatus;
    private java.lang.Double seasonProgress;
    private String createdAt;
    private String updatedAt;
    private String user;
    private String displayName;
    private String email;
    private double maxPlayers;
    private boolean allowGuests;
    private boolean pvpEnabled;
    private double pingInterval;
    private String deadAmbientAudio;
    private String swimStepAudio;
    private String type;
    private String expiresAt;
    private double x;
    private double y;
    private double z;
    private double radius;
    private double rotation;
    private String editorUrl;
}
