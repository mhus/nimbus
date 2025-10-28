/**
 * Entity serialization utilities
 * Convert between EntityData and JSON representations
 */

import type { EntityData } from '../types/EntityData';
import { isPlayer, isNPC, isItem, isMob } from '../types/EntityData';

/**
 * Entity serialization helpers
 */
export namespace EntitySerializer {
  /**
   * Serialize entity to JSON
   * @param entity Entity to serialize
   * @param _includeAllFields Include all fields (default: false, optimized)
   * @returns JSON string
   */
  export function toJSON(entity: EntityData, _includeAllFields: boolean = false): string {
    // Always use optimized version
    const obj = toObject(entity);
    return JSON.stringify(obj);
  }

  /**
   * Deserialize entity from JSON
   * @param json JSON string
   * @returns EntityData or null if invalid
   */
  export function fromJSON(json: string): EntityData | null {
    try {
      const data = JSON.parse(json);
      return fromObject(data);
    } catch (e) {
      console.error('Failed to parse entity JSON:', e);
      return null;
    }
  }

  /**
   * Convert EntityData to plain object (optimized for network)
   * Only includes relevant fields per entity type
   * @param entity Entity data
   * @returns Plain object
   */
  export function toObject(entity: EntityData): any {
    // Base fields (all entities)
    const obj: any = {
      id: entity.id,
      type: entity.type,
      position: entity.position,
      rotation: entity.rotation,
    };

    // Optional common fields
    if (entity.velocity) obj.velocity = entity.velocity;
    if (entity.walkToPosition) obj.walkToPosition = entity.walkToPosition;
    if (entity.visibility) obj.visibility = entity.visibility;

    // Type-specific fields
    if (isPlayer(entity)) {
      if (entity.username) obj.username = entity.username;
      if (entity.displayName) obj.displayName = entity.displayName;
      if (entity.userId) obj.userId = entity.userId;
      if (entity.health) obj.health = entity.health;
      if (entity.state) obj.state = entity.state;
      if (entity.isCrouching) obj.isCrouching = entity.isCrouching;
      if (entity.isSprinting) obj.isSprinting = entity.isSprinting;
      if (entity.role) obj.role = entity.role;
      if (entity.team) obj.team = entity.team;
      if (entity.heldItem !== undefined) obj.heldItem = entity.heldItem;
      // Don't send full inventory in every update (too large)
    } else if (isNPC(entity)) {
      if (entity.displayName) obj.displayName = entity.displayName;
      if (entity.dialogId) obj.dialogId = entity.dialogId;
      if (entity.aiState) obj.aiState = entity.aiState;
      if (entity.targetId) obj.targetId = entity.targetId;
      if (entity.team) obj.team = entity.team;
    } else if (isMob(entity)) {
      if (entity.displayName) obj.displayName = entity.displayName;
      if (entity.health) obj.health = entity.health;
      if (entity.aggression !== undefined) obj.aggression = entity.aggression;
      if (entity.aiState) obj.aiState = entity.aiState;
      if (entity.targetId) obj.targetId = entity.targetId;
    } else if (isItem(entity)) {
      if (entity.itemTypeId !== undefined) obj.itemTypeId = entity.itemTypeId;
      if (entity.stackCount !== undefined) obj.stackCount = entity.stackCount;
      if (entity.canPickup !== undefined) obj.canPickup = entity.canPickup;
    }

    // Metadata if present
    if (entity.metadata) obj.metadata = entity.metadata;

    return obj;
  }

  /**
   * Convert plain object to EntityData
   * @param obj Plain object
   * @returns EntityData or null if invalid
   */
  export function fromObject(obj: any): EntityData | null {
    if (!obj || typeof obj !== 'object') {
      return null;
    }

    if (!obj.id || !obj.type || !obj.position || !obj.rotation) {
      return null;
    }

    return obj as EntityData;
  }

  /**
   * Serialize entity array to JSON
   * @param entities Array of entities
   * @returns JSON string
   */
  export function arrayToJSON(entities: EntityData[]): string {
    const objects = entities.map((e) => toObject(e));
    return JSON.stringify(objects);
  }

  /**
   * Deserialize entity array from JSON
   * @param json JSON string
   * @returns Array of entities or null
   */
  export function arrayFromJSON(json: string): EntityData[] | null {
    try {
      const data = JSON.parse(json);

      if (!Array.isArray(data)) {
        return null;
      }

      const entities = data
        .map(fromObject)
        .filter((e): e is EntityData => e !== null);

      return entities;
    } catch (e) {
      console.error('Failed to parse entity array JSON:', e);
      return null;
    }
  }

  /**
   * Create minimal update object (only position and rotation)
   * @param entity Entity
   * @returns Minimal update object
   */
  export function toMinimalUpdate(entity: EntityData): any {
    return {
      id: entity.id,
      position: entity.position,
      rotation: entity.rotation,
      velocity: entity.velocity,
    };
  }

  /**
   * Create full snapshot (all fields)
   * @param entity Entity
   * @returns Full snapshot object
   */
  export function toFullSnapshot(entity: EntityData): any {
    return JSON.parse(JSON.stringify(entity));
  }
}
