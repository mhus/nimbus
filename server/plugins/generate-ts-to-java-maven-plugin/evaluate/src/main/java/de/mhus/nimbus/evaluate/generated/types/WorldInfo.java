/*
 * Source TS: World.ts
 * Original TS: 'interface WorldInfo'
 */
package de.mhus.nimbus.evaluate.generated.types;

@Deprecated
@lombok.Data
@lombok.experimental.SuperBuilder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class WorldInfo {
    @Deprecated
    @SuppressWarnings("required")
    private String worldId;
    @Deprecated
    @SuppressWarnings("required")
    private String name;
    @Deprecated
    @SuppressWarnings("optional")
    private String description;
    @Deprecated
    @SuppressWarnings("optional")
    private Vector3 start;
    @Deprecated
    @SuppressWarnings("optional")
    private Vector3 stop;
    @Deprecated
    @SuppressWarnings("optional")
    private java.lang.Double chunkSize;
    @Deprecated
    @SuppressWarnings("optional")
    private String assetPath;
    @Deprecated
    @SuppressWarnings("optional")
    private java.lang.Double assetPort;
    @Deprecated
    @SuppressWarnings("optional")
    private String worldGroupId;
    @Deprecated
    @SuppressWarnings("optional")
    private java.lang.Double status;
    @Deprecated
    @SuppressWarnings("optional")
    private String seasonStatus;
    @Deprecated
    @SuppressWarnings("optional")
    private java.lang.Double seasonProgress;
    @Deprecated
    @SuppressWarnings("optional")
    private String createdAt;
    @Deprecated
    @SuppressWarnings("optional")
    private String updatedAt;
    @Deprecated
    @SuppressWarnings("optional")
    private java.util.Map<String, Object> owner;
    @Deprecated
    @SuppressWarnings("optional")
    private java.util.Map<String, Object> settings;
    @Deprecated
    @SuppressWarnings("optional")
    private java.util.Map<String, Object> license;
    @Deprecated
    @SuppressWarnings("optional")
    private java.util.Map<String, Object> startArea;
    @Deprecated
    @SuppressWarnings("optional")
    private String editorUrl;
}
