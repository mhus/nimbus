/**
 * Tests for EntityValidator
 */

import { EntityValidator } from './EntityValidator';
import { EntityType, createPlayer, createNPC, createItem, type EntityData } from '../types/EntityData';

describe('EntityValidator', () => {
  describe('isValidEntityId', () => {
    it('should accept valid entity IDs', () => {
      expect(EntityValidator.isValidEntityId('entity123')).toBe(true);
      expect(EntityValidator.isValidEntityId('p')).toBe(true);
      expect(EntityValidator.isValidEntityId('a'.repeat(100))).toBe(true);
    });

    it('should reject invalid entity IDs', () => {
      expect(EntityValidator.isValidEntityId('')).toBe(false);
      expect(EntityValidator.isValidEntityId('a'.repeat(101))).toBe(false);
      expect(EntityValidator.isValidEntityId(123 as any)).toBe(false);
    });
  });

  describe('isValidEntityType', () => {
    it('should accept valid entity types', () => {
      expect(EntityValidator.isValidEntityType(EntityType.PLAYER)).toBe(true);
      expect(EntityValidator.isValidEntityType(EntityType.NPC)).toBe(true);
      expect(EntityValidator.isValidEntityType(EntityType.ITEM)).toBe(true);
      expect(EntityValidator.isValidEntityType(EntityType.MOB)).toBe(true);
    });

    it('should reject invalid entity types', () => {
      expect(EntityValidator.isValidEntityType('invalid')).toBe(false);
      expect(EntityValidator.isValidEntityType('')).toBe(false);
    });
  });

  describe('isValidPosition', () => {
    it('should accept valid positions', () => {
      expect(EntityValidator.isValidPosition({ x: 0, y: 0, z: 0 })).toBe(true);
      expect(EntityValidator.isValidPosition({ x: 10.5, y: 64.2, z: -20.7 })).toBe(true);
    });

    it('should reject invalid positions', () => {
      expect(EntityValidator.isValidPosition(null)).toBeFalsy();
      expect(EntityValidator.isValidPosition({ x: NaN, y: 0, z: 0 })).toBe(false);
      expect(EntityValidator.isValidPosition({ x: 0, y: Infinity, z: 0 })).toBe(false);
      expect(EntityValidator.isValidPosition({ x: 0, y: 0 })).toBe(false); // Missing z
    });
  });

  describe('isValidRotation', () => {
    it('should accept valid rotations', () => {
      expect(EntityValidator.isValidRotation({ y: 0, p: 0 })).toBe(true);
      expect(EntityValidator.isValidRotation({ y: 90.5, p: 15.3 })).toBe(true);
    });

    it('should reject invalid rotations', () => {
      expect(EntityValidator.isValidRotation(null)).toBeFalsy();
      expect(EntityValidator.isValidRotation({ y: NaN, p: 0 })).toBe(false);
      expect(EntityValidator.isValidRotation({ y: 0 })).toBe(false); // Missing p
    });
  });

  describe('validateEntity', () => {
    it('should validate correct player entity', () => {
      const player = createPlayer('p1', 'u1', 'username', 'Display Name', {
        x: 0,
        y: 64,
        z: 0,
      });
      const result = EntityValidator.validateEntity(player);
      expect(result.valid).toBe(true);
      expect(result.errors).toHaveLength(0);
    });

    it('should validate correct NPC entity', () => {
      const npc = createNPC('npc1', 'Merchant', { x: 0, y: 64, z: 0 }, 'models/merchant.glb');
      const result = EntityValidator.validateEntity(npc);
      expect(result.valid).toBe(true);
    });

    it('should validate correct item entity', () => {
      const item = createItem('i1', 42, { x: 0, y: 0, z: 0 });
      const result = EntityValidator.validateEntity(item);
      expect(result.valid).toBe(true);
    });

    it('should reject invalid entity ID', () => {
      const entity: EntityData = {
        id: '',
        type: EntityType.PLAYER,
        position: { x: 0, y: 0, z: 0 },
        rotation: { y: 0, p: 0 },
      };
      const result = EntityValidator.validateEntity(entity);
      expect(result.valid).toBe(false);
      expect(result.errors.some((e) => e.includes('Invalid entity ID'))).toBe(true);
    });

    it('should reject invalid entity type', () => {
      const entity: any = {
        id: 'e1',
        type: 'invalid',
        position: { x: 0, y: 0, z: 0 },
        rotation: { y: 0, p: 0 },
      };
      const result = EntityValidator.validateEntity(entity);
      expect(result.valid).toBe(false);
      expect(result.errors.some((e) => e.includes('Invalid entity type'))).toBe(true);
    });

    it('should reject invalid position', () => {
      const entity: EntityData = {
        id: 'e1',
        type: EntityType.PLAYER,
        position: { x: NaN, y: 0, z: 0 },
        rotation: { y: 0, p: 0 },
      };
      const result = EntityValidator.validateEntity(entity);
      expect(result.valid).toBe(false);
      expect(result.errors).toContain('Invalid position');
    });

    it('should reject invalid rotation', () => {
      const entity: EntityData = {
        id: 'e1',
        type: EntityType.PLAYER,
        position: { x: 0, y: 0, z: 0 },
        rotation: { y: NaN, p: 0 },
      };
      const result = EntityValidator.validateEntity(entity);
      expect(result.valid).toBe(false);
      expect(result.errors).toContain('Invalid rotation');
    });

    it('should warn about extreme Y position', () => {
      const entity: EntityData = {
        id: 'e1',
        type: EntityType.PLAYER,
        position: { x: 0, y: 1000, z: 0 },
        rotation: { y: 0, p: 0 },
      };
      const result = EntityValidator.validateEntity(entity);
      expect(result.warnings).toBeDefined();
      expect(result.warnings!.some((w) => w.includes('Unusual Y position'))).toBe(true);
    });

    it('should warn about player missing username', () => {
      const player: EntityData = {
        id: 'p1',
        type: EntityType.PLAYER,
        position: { x: 0, y: 0, z: 0 },
        rotation: { y: 0, p: 0 },
      };
      const result = EntityValidator.validateEntity(player);
      expect(result.warnings).toBeDefined();
      expect(result.warnings!.some((w) => w.includes('missing username'))).toBe(true);
    });

    it('should reject player with invalid health', () => {
      const player = createPlayer('p1', 'u1', 'user', 'User', { x: 0, y: 0, z: 0 });
      player.health = { current: -10, max: 20, alive: true };
      const result = EntityValidator.validateEntity(player);
      expect(result.valid).toBe(false);
      expect(result.errors.some((e) => e.includes('Invalid health current'))).toBe(true);
    });

    it('should reject item missing itemTypeId', () => {
      const item: EntityData = {
        id: 'i1',
        type: EntityType.ITEM,
        position: { x: 0, y: 0, z: 0 },
        rotation: { y: 0, p: 0 },
      };
      const result = EntityValidator.validateEntity(item);
      expect(result.valid).toBe(false);
      expect(result.errors.some((e) => e.includes('Item missing itemTypeId'))).toBe(true);
    });

    it('should reject invalid velocity', () => {
      const entity: EntityData = {
        id: 'e1',
        type: EntityType.PLAYER,
        position: { x: 0, y: 0, z: 0 },
        rotation: { y: 0, p: 0 },
        velocity: { x: NaN, y: 0, z: 0 },
      };
      const result = EntityValidator.validateEntity(entity);
      expect(result.valid).toBe(false);
      expect(result.errors).toContain('Invalid velocity');
    });
  });

  describe('validateEntityArray', () => {
    it('should validate empty array', () => {
      const result = EntityValidator.validateEntityArray([]);
      expect(result.valid).toBe(true);
    });

    it('should validate correct entity array', () => {
      const entities: EntityData[] = [
        createPlayer('p1', 'u1', 'user1', 'User 1', { x: 0, y: 0, z: 0 }),
        createNPC('npc1', 'NPC', { x: 1, y: 0, z: 0 }, 'model.glb'),
      ];
      const result = EntityValidator.validateEntityArray(entities);
      expect(result.valid).toBe(true);
    });

    it('should reject non-array', () => {
      const result = EntityValidator.validateEntityArray('invalid' as any);
      expect(result.valid).toBe(false);
      expect(result.errors).toContain('Entities must be an array');
    });

    it('should reject too many entities', () => {
      const entities = Array(1001)
        .fill(null)
        .map(() =>
          createPlayer('p', 'u', 'user', 'User', { x: 0, y: 0, z: 0 })
        );
      const result = EntityValidator.validateEntityArray(entities);
      expect(result.valid).toBe(false);
      expect(result.errors.some((e) => e.includes('Too many entities'))).toBe(true);
    });

    it('should reject duplicate entity IDs', () => {
      const entities: EntityData[] = [
        createPlayer('p1', 'u1', 'user1', 'User 1', { x: 0, y: 0, z: 0 }),
        createPlayer('p1', 'u2', 'user2', 'User 2', { x: 1, y: 0, z: 0 }),
      ];
      const result = EntityValidator.validateEntityArray(entities);
      expect(result.valid).toBe(false);
      expect(result.errors.some((e) => e.includes('Duplicate entity ID'))).toBe(true);
    });
  });

  describe('isValid', () => {
    it('should return true for valid entity', () => {
      const entity = createPlayer('p1', 'u1', 'user', 'User', { x: 0, y: 0, z: 0 });
      expect(EntityValidator.isValid(entity)).toBe(true);
    });

    it('should return false for invalid entity', () => {
      const entity: any = {
        id: '',
        type: 'invalid',
        position: { x: NaN, y: 0, z: 0 },
        rotation: { y: 0, p: 0 },
      };
      expect(EntityValidator.isValid(entity)).toBe(false);
    });
  });
});
