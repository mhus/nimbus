/**
 * Entity System - Dynamic objects in the game world
 *
 * Entities sind dynamische Objekte, die in der Spielwelt existieren.
 * Sie wechseln oft ihre Position, Ausrichtung oder ihren Zustand.
 * Beispiele: Spieler, NPCs, Fahrzeuge, bewegliche Gegenstände.
 */

import type { Vector3 } from './Vector3';
import type { Rotation } from './Rotation';

/**
 * PoseType - Art der Bewegungsanimation
 */
export type PoseType =
  | '2-Legs'    // Zweibeinig (Menschen, Vögel)
  | '4-Legs'    // Vierbeinig (Hunde, Pferde)
  | '6-Legs'    // Sechsbeinig (Insekten)
  | 'Wings'     // Fliegend (Vögel, Drachen)
  | 'Fish'      // Schwimmend (Fische)
  | 'Snake'     // Kriechend (Schlangen)
  | 'Humanoid'  // Menschenähnlich
  | 'Slime';    // Gleitend/formlos

/**
 * Dimensions for different movement states
 * Collision box dimensions for each pose/movement type
 */
export interface EntityDimensions {
  walk?: { height: number; width: number; footprint: number };
  sprint?: { height: number; width: number; footprint: number };
  crouch?: { height: number; width: number; footprint: number };
  swim?: { height: number; width: number; footprint: number };
  climb?: { height: number; width: number; footprint: number };
  fly?: { height: number; width: number; footprint: number };
  teleport?: { height: number; width: number; footprint: number };
}

/**
 * EntityModel - Template/Definition für Entity-Typen
 *
 * Analog zu BlockType: Definiert die Eigenschaften eines Entity-Typs.
 * Wird im Registry gespeichert und von Entity-Instanzen referenziert.
 */
export interface EntityModel {
  /** Unique identifier for this entity model */
  id: string;

  /** Type/category of entity */
  type: string;

  /** Path to 3D model file */
  modelPath: string;

  /** Position offset from entity position */
  positionOffset: Vector3;

  /** Rotation offset from entity rotation */
  rotationOffset: Vector3;

  /** Scale of the model */
  scale: Vector3;

  /** Mapping of pose IDs to animation names */
  poseMapping: Map<number, string>;

  /** Type of movement/animation system */
  poseType: PoseType;

  /** Mapping of modifier keys to visual modifications (e.g., skin colors, equipment) */
  modelModifierMapping: Map<string, string>;

  /** Collision dimensions for different movement states */
  dimensions: EntityDimensions;
}

/**
 * MovementType - Defines how dynamic an entity's movement is
 */
export type MovementType =
  | 'static'   // Statisch (bewegt sich nicht)
  | 'passive'  // Passiv (langsame, vorhersagbare Bewegung)
  | 'slow'     // Langsam (moderate Bewegung)
  | 'dynamic'; // Dynamisch (schnelle, unvorhersagbare Bewegung)

/**
 * Entity - Konkrete Entity-Instanz in der Welt
 *
 * Analog zu BlockInstance: Eine konkrete Entity an einer bestimmten Position.
 * Referenziert ein EntityModel über die ID.
 */
export interface Entity {
  /** Unique identifier for this entity instance */
  id: string;

  /** Display name of the entity */
  name: string;

  /** Reference to EntityModel (by ID) */
  model: string; // EntityModel ID

  /** Custom modifiers for this instance (overrides/extends model defaults) */
  modelModifier: Record<string, any>;

  /** Movement behavior type */
  movementType: MovementType;

  /** Is this entity solid (blocking)? */
  solid?: boolean;

  /** Is this entity interactive (can be clicked/used)? */
  interactive?: boolean;
}

/**
 * Waypoint - Single point in an entity's path
 */
export interface Waypoint {
  /** Target timestamp when entity should reach this waypoint */
  timestamp: number;

  /** Target position (world coordinates) */
  target: Vector3;

  /** Target rotation (direction, pitch) */
  rotation: Rotation;

  /** Pose/animation ID at this waypoint */
  pose: number;
}

/**
 * EntityPathway - Movement path for an entity
 *
 * Defines a sequence of waypoints that the entity follows.
 * Can be used for NPCs, moving platforms, or other scripted movements.
 */
export interface EntityPathway {
  /** Entity ID this pathway belongs to */
  entityId: string;

  /** Start timestamp for the pathway */
  startAt: number;

  /** Sequence of waypoints */
  waypoints: Waypoint[];

  /** Should the pathway loop? If true, recalculate waypoints with startAt */
  isLooping?: boolean;

  /** Timestamp for querying current position (interpolation) */
  queryAt?: number;

  /** Idle pose when not moving */
  idlePose?: number;
}

/**
 * Helper: Create a basic EntityModel
 */
export function createEntityModel(
  id: string,
  type: string,
  modelPath: string,
  poseType: PoseType,
  dimensions: EntityDimensions
): EntityModel {
  return {
    id,
    type,
    modelPath,
    positionOffset: { x: 0, y: 0, z: 0 },
    rotationOffset: { x: 0, y: 0, z: 0 },
    scale: { x: 1.0, y: 1.0, z: 1.0 },
    poseMapping: new Map(),
    poseType,
    modelModifierMapping: new Map(),
    dimensions,
  };
}

/**
 * Helper: Create a basic Entity instance
 */
export function createEntity(
  id: string,
  name: string,
  modelId: string,
  movementType: MovementType = 'static'
): Entity {
  return {
    id,
    name,
    model: modelId,
    modelModifier: {},
    movementType,
    solid: true,
    interactive: false,
  };
}

/**
 * Helper: Create a pathway for an entity
 */
export function createEntityPathway(
  entityId: string,
  startAt: number,
  waypoints: Waypoint[],
  isLooping: boolean = false
): EntityPathway {
  return {
    entityId,
    startAt,
    waypoints,
    isLooping,
  };
}

/**
 * Helper: Interpolate entity position at a given timestamp
 *
 * Calculates the current position/rotation/pose based on waypoints and timestamp.
 */
export function interpolateEntityPosition(
  pathway: EntityPathway,
  queryTimestamp: number
): { position: Vector3; rotation: Rotation; pose: number } | null {
  if (pathway.waypoints.length === 0) {
    return null;
  }

  // Find the two waypoints to interpolate between
  let prevWaypoint: Waypoint | null = null;
  let nextWaypoint: Waypoint | null = null;

  for (let i = 0; i < pathway.waypoints.length; i++) {
    const wp = pathway.waypoints[i];
    if (wp.timestamp <= queryTimestamp) {
      prevWaypoint = wp;
      nextWaypoint = pathway.waypoints[i + 1] || null;
    } else {
      break;
    }
  }

  // If before first waypoint
  if (!prevWaypoint) {
    const first = pathway.waypoints[0];
    return {
      position: first.target,
      rotation: first.rotation,
      pose: first.pose,
    };
  }

  // If after last waypoint
  if (!nextWaypoint) {
    return {
      position: prevWaypoint.target,
      rotation: prevWaypoint.rotation,
      pose: pathway.idlePose ?? prevWaypoint.pose,
    };
  }

  // Interpolate between waypoints
  const t = (queryTimestamp - prevWaypoint.timestamp) / (nextWaypoint.timestamp - prevWaypoint.timestamp);
  const clampedT = Math.max(0, Math.min(1, t));

  return {
    position: {
      x: prevWaypoint.target.x + (nextWaypoint.target.x - prevWaypoint.target.x) * clampedT,
      y: prevWaypoint.target.y + (nextWaypoint.target.y - prevWaypoint.target.y) * clampedT,
      z: prevWaypoint.target.z + (nextWaypoint.target.z - prevWaypoint.target.z) * clampedT,
    },
    rotation: {
      y: prevWaypoint.rotation.y + (nextWaypoint.rotation.y - prevWaypoint.rotation.y) * clampedT,
      p: prevWaypoint.rotation.p + (nextWaypoint.rotation.p - prevWaypoint.rotation.p) * clampedT,
    },
    pose: clampedT < 0.5 ? prevWaypoint.pose : nextWaypoint.pose,
  };
}
