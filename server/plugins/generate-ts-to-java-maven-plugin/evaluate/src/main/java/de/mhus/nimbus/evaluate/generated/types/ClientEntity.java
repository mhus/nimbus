/*
 * Source TS: ClientEntity.ts
 * Original TS: 'interface ClientEntity'
 */
package de.mhus.nimbus.evaluate.generated.types;

@lombok.Data
@lombok.experimental.SuperBuilder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class ClientEntity {
    private String id;
    private EntityModel model;
    private Entity entity;
    private boolean visible;
    private java.util.List<Object> meshes;
    private Vector3 currentPosition;
    private Rotation currentRotation;
    private double currentWaypointIndex;
    private double currentPose;
    private java.util.List<Waypoint> currentWaypoints;
    private double lastAccess;
    private java.lang.Double lastStepTime;
}
