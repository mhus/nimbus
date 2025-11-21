/*
 * Source TS: ClientEntity.ts
 * Original TS: 'interface ClientEntity'
 */
package de.mhus.nimbus.generated.types;

@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
@lombok.Data
@lombok.Builder
public class ClientEntity extends Object {
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
    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    private java.lang.Double lastStepTime;
}
