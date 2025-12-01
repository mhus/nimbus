/*
 * Source TS: World.ts
 * Original TS: 'interface WorldInfo'
 */
package de.mhus.nimbus.evaluate.generated.types;

@lombok.Data
@lombok.experimental.SuperBuilder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class WorldInfo {
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
    private java.util.Map<String, Object> owner;
    private java.util.Map<String, Object> settings;
    private java.util.Map<String, Object> license;
    private java.util.Map<String, Object> startArea;
    private String editorUrl;
}
