/*
 * Source TS: ClientEntity.ts
 * Original TS: 'interface ClientEntity'
 */
package de.mhus.nimbus.evaluate.generated.types;

@Deprecated
@lombok.Data
@lombok.experimental.SuperBuilder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class ClientEntity {
    @Deprecated
    @SuppressWarnings("required")
    private String id;
    @Deprecated
    @SuppressWarnings("required")
    private EntityModel model;
    @Deprecated
    @SuppressWarnings("required")
    private Entity entity;
    @Deprecated
    @SuppressWarnings("required")
    private boolean visible;
    @Deprecated
    @SuppressWarnings("required")
    private java.util.List<Object> meshes;
    @Deprecated
    @SuppressWarnings("required")
    private Vector3 currentPosition;
    @Deprecated
    @SuppressWarnings("required")
    private Rotation currentRotation;
    @Deprecated
    @SuppressWarnings("required")
    private double currentWaypointIndex;
    @Deprecated
    @SuppressWarnings("required")
    private double currentPose;
    @Deprecated
    @SuppressWarnings("required")
    private java.util.List<Waypoint> currentWaypoints;
    @Deprecated
    @SuppressWarnings("required")
    private double lastAccess;
    @Deprecated
    @SuppressWarnings("optional")
    private java.lang.Double lastStepTime;
}
