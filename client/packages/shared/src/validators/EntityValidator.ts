/**
 * Entity validation functions
 */

import type { EntityData } from '../types/EntityData';
import { EntityType } from '../types/EntityData';
import { EntityConstants, LimitConstants } from '../constants/NimbusConstants';

/**
 * Validation result
 */
export interface ValidationResult {
  valid: boolean;
  errors: string[];
  warnings?: string[];
}

/**
 * Entity validators
 */
export namespace EntityValidator {
  /**
   * Validate entity ID format
   * @param id Entity ID
   * @returns True if valid
   */
  export function isValidEntityId(id: string): boolean {
    return (
      typeof id === 'string' &&
      id.length > 0 &&
      id.length <= EntityConstants.ENTITY_ID_MAX_LENGTH
    );
  }

  /**
   * Validate entity type
   * @param type Entity type
   * @returns True if valid
   */
  export function isValidEntityType(type: string): boolean {
    return Object.values(EntityType).includes(type as EntityType);
  }

  /**
   * Validate rotation
   * @param rotation Rotation object
   * @returns True if valid
   */
  export function isValidRotation(rotation: any): boolean {
    return (
      rotation &&
      typeof rotation === 'object' &&
      Number.isFinite(rotation.y) &&
      Number.isFinite(rotation.p)
    );
  }

  /**
   * Validate position
   * @param position Position vector
   * @returns True if valid
   */
  export function isValidPosition(position: any): boolean {
    return (
      position &&
      typeof position === 'object' &&
      Number.isFinite(position.x) &&
      Number.isFinite(position.y) &&
      Number.isFinite(position.z)
    );
  }

  /**
   * Validate entity data
   * @param entity Entity to validate
   * @returns Validation result
   */
  export function validateEntity(entity: EntityData): ValidationResult {
    const errors: string[] = [];
    const warnings: string[] = [];

    // Validate ID
    if (!isValidEntityId(entity.id)) {
      errors.push(`Invalid entity ID: ${entity.id}`);
    }

    // Validate type
    if (!isValidEntityType(entity.type)) {
      errors.push(`Invalid entity type: ${entity.type}`);
    }

    // Validate position
    if (!isValidPosition(entity.position)) {
      errors.push('Invalid position');
    } else {
      // Warn about extreme positions
      if (Math.abs(entity.position.y) > 512) {
        warnings.push(`Unusual Y position: ${entity.position.y}`);
      }
    }

    // Validate rotation
    if (!isValidRotation(entity.rotation)) {
      errors.push('Invalid rotation');
    }

    // Type-specific validation
    if (entity.type === EntityType.PLAYER) {
      validatePlayerFields(entity, errors, warnings);
    } else if (entity.type === EntityType.NPC) {
      validateNPCFields(entity, errors, warnings);
    } else if (entity.type === EntityType.ITEM) {
      validateItemFields(entity, errors, warnings);
    }

    // Validate optional fields
    if (entity.velocity && !isValidPosition(entity.velocity)) {
      errors.push('Invalid velocity');
    }

    if (entity.walkToPosition && !isValidPosition(entity.walkToPosition)) {
      errors.push('Invalid walkToPosition');
    }

    return { valid: errors.length === 0, errors, warnings };
  }

  /**
   * Validate player-specific fields
   */
  function validatePlayerFields(
    entity: EntityData,
    errors: string[],
    warnings: string[]
  ): void {
    if (!entity.username) {
      warnings.push('Player missing username');
    }

    if (!entity.userId) {
      warnings.push('Player missing userId');
    }

    if (
      entity.username &&
      entity.username.length > EntityConstants.USERNAME_MAX_LENGTH
    ) {
      warnings.push(
        `Username too long: ${entity.username.length} chars (max: ${EntityConstants.USERNAME_MAX_LENGTH})`
      );
    }

    if (entity.health) {
      if (entity.health.current < 0) {
        errors.push(`Invalid health current: ${entity.health.current}`);
      }
      if (entity.health.max <= 0) {
        errors.push(`Invalid health max: ${entity.health.max}`);
      }
      if (entity.health.current > entity.health.max) {
        warnings.push('Health current exceeds max');
      }
    }
  }

  /**
   * Validate NPC-specific fields
   */
  function validateNPCFields(
    entity: EntityData,
    _errors: string[],
    warnings: string[]
  ): void {
    if (!entity.displayName) {
      warnings.push('NPC missing displayName');
    }

    if (!entity.visibility?.modelPath) {
      warnings.push('NPC missing model path');
    }
  }

  /**
   * Validate item-specific fields
   */
  function validateItemFields(
    entity: EntityData,
    errors: string[],
    _warnings: string[]
  ): void {
    if (entity.itemTypeId === undefined) {
      errors.push('Item missing itemTypeId');
    } else if (entity.itemTypeId < 0 || entity.itemTypeId > 65535) {
      errors.push(`Invalid itemTypeId: ${entity.itemTypeId}`);
    }

    if (entity.stackCount !== undefined && entity.stackCount <= 0) {
      errors.push(`Invalid stackCount: ${entity.stackCount}`);
    }
  }

  /**
   * Validate entity array
   * @param entities Array of entities
   * @param maxCount Maximum allowed entities
   * @returns Validation result
   */
  export function validateEntityArray(
    entities: EntityData[],
    maxCount: number = LimitConstants.MAX_ENTITIES_PER_MESSAGE
  ): ValidationResult {
    const errors: string[] = [];
    const warnings: string[] = [];

    if (!Array.isArray(entities)) {
      errors.push('Entities must be an array');
      return { valid: false, errors, warnings };
    }

    if (entities.length > maxCount) {
      errors.push(`Too many entities: ${entities.length} (max: ${maxCount})`);
    }

    // Validate each entity
    entities.forEach((entity, index) => {
      const result = validateEntity(entity);
      if (!result.valid) {
        errors.push(`Entity[${index}]: ${result.errors.join(', ')}`);
      }
    });

    // Check for duplicate IDs
    const ids = new Set<string>();
    entities.forEach((entity, index) => {
      if (ids.has(entity.id)) {
        errors.push(`Duplicate entity ID at index ${index}: ${entity.id}`);
      }
      ids.add(entity.id);
    });

    return { valid: errors.length === 0, errors, warnings };
  }

  /**
   * Quick validation (only critical checks)
   * @param entity Entity to validate
   * @returns True if valid
   */
  export function isValid(entity: EntityData): boolean {
    return (
      isValidEntityId(entity.id) &&
      isValidEntityType(entity.type) &&
      isValidPosition(entity.position) &&
      isValidRotation(entity.rotation)
    );
  }
}
