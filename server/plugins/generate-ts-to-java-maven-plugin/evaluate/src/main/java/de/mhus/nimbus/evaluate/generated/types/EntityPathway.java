/*
 * Source TS: EntityData.ts
 * Original TS: 'interface EntityPathway'
 */
package de.mhus.nimbus.evaluate.generated.types;

@Deprecated
@lombok.Data
@lombok.experimental.SuperBuilder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class EntityPathway {
    @Deprecated
    @SuppressWarnings("required")
    private String entityId;
    @Deprecated
    @SuppressWarnings("required")
    private double startAt;
    @Deprecated
    @SuppressWarnings("required")
    private java.util.List<Waypoint> waypoints;
    @Deprecated
    @SuppressWarnings("optional")
    private java.lang.Boolean isLooping;
    @Deprecated
    @SuppressWarnings("optional")
    private java.lang.Double queryAt;
    @Deprecated
    @SuppressWarnings("optional")
    private ENTITY_POSES idlePose;
    @Deprecated
    @SuppressWarnings("optional")
    private java.lang.Boolean physicsEnabled;
    @Deprecated
    @SuppressWarnings("optional")
    private Vector3 velocity;
    @Deprecated
    @SuppressWarnings("optional")
    private java.lang.Boolean grounded;
}
