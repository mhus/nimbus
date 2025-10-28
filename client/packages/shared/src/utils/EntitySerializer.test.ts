/**
 * Tests for EntitySerializer
 */

import { EntitySerializer } from './EntitySerializer';
import {
  EntityType,
  type EntityData,
  createPlayer,
  createNPC,
  createItem,
} from '../types/EntityData';

describe('EntitySerializer', () => {
  describe('toJSON and fromJSON', () => {
    it('should serialize player entity to JSON', () => {
      const player = createPlayer('p1', 'user123', 'testuser', 'Test User', {
        x: 10,
        y: 64,
        z: 20,
      });

      const json = EntitySerializer.toJSON(player);
      expect(json).toBeTruthy();
      expect(typeof json).toBe('string');

      const parsed = JSON.parse(json);
      expect(parsed.id).toBe('p1');
      expect(parsed.type).toBe(EntityType.PLAYER);
      expect(parsed.username).toBe('testuser');
      expect(parsed.displayName).toBe('Test User');
    });

    it('should serialize NPC entity to JSON', () => {
      const npc = createNPC('npc1', 'Village Elder', { x: 5, y: 64, z: 10 }, 'models/elder.glb', 'dialog_elder');

      const json = EntitySerializer.toJSON(npc);
      const parsed = JSON.parse(json);

      expect(parsed.id).toBe('npc1');
      expect(parsed.type).toBe(EntityType.NPC);
      expect(parsed.displayName).toBe('Village Elder');
      expect(parsed.dialogId).toBe('dialog_elder');
    });

    it('should serialize item entity to JSON', () => {
      const item = createItem('item1', 42, { x: 1, y: 2, z: 3 }, 5);

      const json = EntitySerializer.toJSON(item);
      const parsed = JSON.parse(json);

      expect(parsed.id).toBe('item1');
      expect(parsed.type).toBe(EntityType.ITEM);
      expect(parsed.itemTypeId).toBe(42);
      expect(parsed.stackCount).toBe(5);
    });

    it('should deserialize player entity from JSON', () => {
      const player = createPlayer('p1', 'user123', 'testuser', 'Test User', {
        x: 10,
        y: 64,
        z: 20,
      });

      const json = EntitySerializer.toJSON(player);
      const deserialized = EntitySerializer.fromJSON(json);

      expect(deserialized).not.toBeNull();
      expect(deserialized!.id).toBe('p1');
      expect(deserialized!.type).toBe(EntityType.PLAYER);
      expect(deserialized!.username).toBe('testuser');
    });

    it('should return null for invalid JSON', () => {
      const entity = EntitySerializer.fromJSON('invalid json');
      expect(entity).toBeNull();
    });

    it('should return null for empty string', () => {
      const entity = EntitySerializer.fromJSON('');
      expect(entity).toBeNull();
    });

    it('should serialize entity with velocity', () => {
      const entity: EntityData = {
        id: 'e1',
        type: EntityType.PLAYER,
        position: { x: 1, y: 2, z: 3 },
        rotation: { y: 0, p: 0 },
        velocity: { x: 0.1, y: 0, z: 0.1 },
      };

      const json = EntitySerializer.toJSON(entity);
      const parsed = JSON.parse(json);

      expect(parsed.velocity).toEqual({ x: 0.1, y: 0, z: 0.1 });
    });

    it('should serialize entity with walkToPosition', () => {
      const entity: EntityData = {
        id: 'e1',
        type: EntityType.NPC,
        position: { x: 1, y: 2, z: 3 },
        rotation: { y: 0, p: 0 },
        walkToPosition: { x: 10, y: 2, z: 15 },
      };

      const json = EntitySerializer.toJSON(entity);
      const parsed = JSON.parse(json);

      expect(parsed.walkToPosition).toEqual({ x: 10, y: 2, z: 15 });
    });

    it('should serialize entity with visibility', () => {
      const entity: EntityData = {
        id: 'e1',
        type: EntityType.NPC,
        position: { x: 1, y: 2, z: 3 },
        rotation: { y: 0, p: 0 },
        visibility: {
          modelPath: 'models/npc.glb',
          visible: true,
          scale: { x: 1.2, y: 1.2, z: 1.2 },
        },
      };

      const json = EntitySerializer.toJSON(entity);
      const parsed = JSON.parse(json);

      expect(parsed.visibility).toBeDefined();
      expect(parsed.visibility.modelPath).toBe('models/npc.glb');
      expect(parsed.visibility.scale).toEqual({ x: 1.2, y: 1.2, z: 1.2 });
    });

    it('should serialize mob entity with aggression and target', () => {
      const mob: EntityData = {
        id: 'mob1',
        type: EntityType.MOB,
        position: { x: 5, y: 64, z: 10 },
        rotation: { y: 90, p: 0 },
        displayName: 'Angry Zombie',
        health: { current: 15, max: 20, alive: true },
        aggression: 0.8,
        aiState: 'attacking',
        targetId: 'p1',
      };

      const json = EntitySerializer.toJSON(mob);
      const parsed = JSON.parse(json);

      expect(parsed.type).toBe(EntityType.MOB);
      expect(parsed.aggression).toBe(0.8);
      expect(parsed.aiState).toBe('attacking');
      expect(parsed.targetId).toBe('p1');
    });

    it('should serialize player with state flags', () => {
      const player: EntityData = {
        id: 'p1',
        type: EntityType.PLAYER,
        position: { x: 1, y: 2, z: 3 },
        rotation: { y: 0, p: 0 },
        username: 'sneakyplayer',
        isCrouching: true,
        isSprinting: true, // Only included when true (network optimization)
        state: 'running',
        role: 'admin',
        team: 'red',
        heldItem: 5,
      };

      const json = EntitySerializer.toJSON(player);
      const parsed = JSON.parse(json);

      expect(parsed.isCrouching).toBe(true);
      expect(parsed.isSprinting).toBe(true);
      expect(parsed.state).toBe('running');
      expect(parsed.role).toBe('admin');
      expect(parsed.team).toBe('red');
      expect(parsed.heldItem).toBe(5);
    });

    it('should not include false boolean flags (network optimization)', () => {
      const player: EntityData = {
        id: 'p1',
        type: EntityType.PLAYER,
        position: { x: 1, y: 2, z: 3 },
        rotation: { y: 0, p: 0 },
        username: 'player',
        isCrouching: false,
        isSprinting: false,
      };

      const json = EntitySerializer.toJSON(player);
      const parsed = JSON.parse(json);

      // False values are not included (saves bandwidth)
      expect(parsed.isCrouching).toBeUndefined();
      expect(parsed.isSprinting).toBeUndefined();
    });

    it('should serialize entity with metadata', () => {
      const entity: EntityData = {
        id: 'e1',
        type: EntityType.NPC,
        position: { x: 1, y: 2, z: 3 },
        rotation: { y: 0, p: 0 },
        metadata: {
          questGiver: true,
          questId: 'quest_001',
          customData: { foo: 'bar' },
        },
      };

      const json = EntitySerializer.toJSON(entity);
      const parsed = JSON.parse(json);

      expect(parsed.metadata).toBeDefined();
      expect(parsed.metadata.questGiver).toBe(true);
      expect(parsed.metadata.questId).toBe('quest_001');
    });
  });

  describe('fromObject', () => {
    it('should convert valid object to EntityData', () => {
      const obj = {
        id: 'e1',
        type: EntityType.PLAYER,
        position: { x: 1, y: 2, z: 3 },
        rotation: { y: 0, p: 0 },
        username: 'testuser',
      };

      const entity = EntitySerializer.fromObject(obj);

      expect(entity).not.toBeNull();
      expect(entity!.id).toBe('e1');
      expect(entity!.type).toBe(EntityType.PLAYER);
      expect(entity!.username).toBe('testuser');
    });

    it('should return null for null input', () => {
      const entity = EntitySerializer.fromObject(null);
      expect(entity).toBeNull();
    });

    it('should return null for non-object input', () => {
      expect(EntitySerializer.fromObject('string')).toBeNull();
      expect(EntitySerializer.fromObject(123)).toBeNull();
      expect(EntitySerializer.fromObject(true)).toBeNull();
    });

    it('should return null for object without id', () => {
      const obj = {
        type: EntityType.PLAYER,
        position: { x: 1, y: 2, z: 3 },
        rotation: { y: 0, p: 0 },
      };

      const entity = EntitySerializer.fromObject(obj);
      expect(entity).toBeNull();
    });

    it('should return null for object without type', () => {
      const obj = {
        id: 'e1',
        position: { x: 1, y: 2, z: 3 },
        rotation: { y: 0, p: 0 },
      };

      const entity = EntitySerializer.fromObject(obj);
      expect(entity).toBeNull();
    });

    it('should return null for object without position', () => {
      const obj = {
        id: 'e1',
        type: EntityType.PLAYER,
        rotation: { y: 0, p: 0 },
      };

      const entity = EntitySerializer.fromObject(obj);
      expect(entity).toBeNull();
    });

    it('should return null for object without rotation', () => {
      const obj = {
        id: 'e1',
        type: EntityType.PLAYER,
        position: { x: 1, y: 2, z: 3 },
      };

      const entity = EntitySerializer.fromObject(obj);
      expect(entity).toBeNull();
    });
  });

  describe('toObject', () => {
    it('should convert EntityData to plain object', () => {
      const entity: EntityData = {
        id: 'e1',
        type: EntityType.PLAYER,
        position: { x: 1, y: 2, z: 3 },
        rotation: { y: 45, p: 10 },
      };

      const obj = EntitySerializer.toObject(entity);

      expect(obj.id).toBe('e1');
      expect(obj.type).toBe(EntityType.PLAYER);
      expect(obj.position).toEqual({ x: 1, y: 2, z: 3 });
      expect(obj.rotation).toEqual({ y: 45, p: 10 });
    });

    it('should include optional common fields if present', () => {
      const entity: EntityData = {
        id: 'e1',
        type: EntityType.NPC,
        position: { x: 1, y: 2, z: 3 },
        rotation: { y: 0, p: 0 },
        velocity: { x: 0.5, y: 0, z: 0.5 },
        walkToPosition: { x: 10, y: 2, z: 10 },
        visibility: { visible: true },
      };

      const obj = EntitySerializer.toObject(entity);

      expect(obj.velocity).toEqual({ x: 0.5, y: 0, z: 0.5 });
      expect(obj.walkToPosition).toEqual({ x: 10, y: 2, z: 10 });
      expect(obj.visibility).toEqual({ visible: true });
    });

    it('should not include optional fields if not present', () => {
      const entity: EntityData = {
        id: 'e1',
        type: EntityType.PLAYER,
        position: { x: 1, y: 2, z: 3 },
        rotation: { y: 0, p: 0 },
      };

      const obj = EntitySerializer.toObject(entity);

      expect(obj.velocity).toBeUndefined();
      expect(obj.walkToPosition).toBeUndefined();
      expect(obj.visibility).toBeUndefined();
    });

    it('should include player-specific fields', () => {
      const player = createPlayer('p1', 'user1', 'username', 'Display Name', {
        x: 1,
        y: 2,
        z: 3,
      });
      player.heldItem = 3;
      player.role = 'moderator';

      const obj = EntitySerializer.toObject(player);

      expect(obj.username).toBe('username');
      expect(obj.displayName).toBe('Display Name');
      expect(obj.userId).toBe('user1');
      expect(obj.health).toBeDefined();
      expect(obj.heldItem).toBe(3);
      expect(obj.role).toBe('moderator');
    });

    it('should include NPC-specific fields', () => {
      const npc = createNPC('npc1', 'Merchant', { x: 5, y: 64, z: 10 }, 'models/merchant.glb', 'dialog_merchant');
      npc.aiState = 'trading';
      npc.targetId = 'p1';
      npc.team = 'neutral';

      const obj = EntitySerializer.toObject(npc);

      expect(obj.displayName).toBe('Merchant');
      expect(obj.dialogId).toBe('dialog_merchant');
      expect(obj.aiState).toBe('trading');
      expect(obj.targetId).toBe('p1');
      expect(obj.team).toBe('neutral');
    });

    it('should include mob-specific fields', () => {
      const mob: EntityData = {
        id: 'mob1',
        type: EntityType.MOB,
        position: { x: 1, y: 2, z: 3 },
        rotation: { y: 0, p: 0 },
        displayName: 'Zombie',
        health: { current: 10, max: 20, alive: true },
        aggression: 0.9,
        aiState: 'hunting',
        targetId: 'p1',
      };

      const obj = EntitySerializer.toObject(mob);

      expect(obj.displayName).toBe('Zombie');
      expect(obj.health).toBeDefined();
      expect(obj.aggression).toBe(0.9);
      expect(obj.aiState).toBe('hunting');
      expect(obj.targetId).toBe('p1');
    });

    it('should include item-specific fields', () => {
      const item = createItem('item1', 42, { x: 1, y: 2, z: 3 }, 10);

      const obj = EntitySerializer.toObject(item);

      expect(obj.itemTypeId).toBe(42);
      expect(obj.stackCount).toBe(10);
      expect(obj.canPickup).toBe(true);
    });
  });

  describe('arrayToJSON and arrayFromJSON', () => {
    it('should serialize empty array', () => {
      const json = EntitySerializer.arrayToJSON([]);
      expect(json).toBe('[]');
    });

    it('should serialize array of entities', () => {
      const entities: EntityData[] = [
        createPlayer('p1', 'u1', 'player1', 'Player 1', { x: 1, y: 2, z: 3 }),
        createNPC('npc1', 'NPC', { x: 4, y: 5, z: 6 }, 'model.glb'),
        createItem('i1', 10, { x: 7, y: 8, z: 9 }),
      ];

      const json = EntitySerializer.arrayToJSON(entities);
      const parsed = JSON.parse(json);

      expect(Array.isArray(parsed)).toBe(true);
      expect(parsed).toHaveLength(3);
      expect(parsed[0].type).toBe(EntityType.PLAYER);
      expect(parsed[1].type).toBe(EntityType.NPC);
      expect(parsed[2].type).toBe(EntityType.ITEM);
    });

    it('should deserialize empty array', () => {
      const entities = EntitySerializer.arrayFromJSON('[]');
      expect(entities).toEqual([]);
    });

    it('should deserialize array of entities', () => {
      const original: EntityData[] = [
        createPlayer('p1', 'u1', 'player1', 'Player 1', { x: 1, y: 2, z: 3 }),
        createNPC('npc1', 'NPC', { x: 4, y: 5, z: 6 }, 'model.glb'),
      ];

      const json = EntitySerializer.arrayToJSON(original);
      const entities = EntitySerializer.arrayFromJSON(json);

      expect(entities).not.toBeNull();
      expect(entities).toHaveLength(2);
      expect(entities![0].id).toBe('p1');
      expect(entities![0].type).toBe(EntityType.PLAYER);
      expect(entities![1].id).toBe('npc1');
      expect(entities![1].type).toBe(EntityType.NPC);
    });

    it('should return null for invalid JSON', () => {
      const entities = EntitySerializer.arrayFromJSON('invalid');
      expect(entities).toBeNull();
    });

    it('should return null for non-array JSON', () => {
      const entities = EntitySerializer.arrayFromJSON('{"foo":"bar"}');
      expect(entities).toBeNull();
    });

    it('should filter out invalid entities', () => {
      const json =
        '[{"id":"e1","type":"player","position":{"x":1,"y":2,"z":3},"rotation":{"y":0,"p":0}},{"invalid":"data"}]';
      const entities = EntitySerializer.arrayFromJSON(json);

      expect(entities).not.toBeNull();
      expect(entities).toHaveLength(1);
      expect(entities![0].id).toBe('e1');
    });
  });

  describe('toMinimalUpdate', () => {
    it('should create minimal update with position, rotation, velocity', () => {
      const entity: EntityData = {
        id: 'e1',
        type: EntityType.PLAYER,
        position: { x: 10, y: 64, z: 20 },
        rotation: { y: 90, p: 15 },
        velocity: { x: 0.5, y: 0, z: 0.5 },
        username: 'player',
        health: { current: 20, max: 20, alive: true },
        state: 'running',
      };

      const update = EntitySerializer.toMinimalUpdate(entity);

      expect(update.id).toBe('e1');
      expect(update.position).toEqual({ x: 10, y: 64, z: 20 });
      expect(update.rotation).toEqual({ y: 90, p: 15 });
      expect(update.velocity).toEqual({ x: 0.5, y: 0, z: 0.5 });

      // Should not include other fields
      expect(update.username).toBeUndefined();
      expect(update.health).toBeUndefined();
      expect(update.state).toBeUndefined();
    });

    it('should handle entity without velocity', () => {
      const entity: EntityData = {
        id: 'e1',
        type: EntityType.NPC,
        position: { x: 1, y: 2, z: 3 },
        rotation: { y: 0, p: 0 },
      };

      const update = EntitySerializer.toMinimalUpdate(entity);

      expect(update.id).toBe('e1');
      expect(update.position).toBeDefined();
      expect(update.rotation).toBeDefined();
      expect(update.velocity).toBeUndefined();
    });
  });

  describe('toFullSnapshot', () => {
    it('should create deep copy of entity', () => {
      const entity = createPlayer('p1', 'u1', 'player', 'Player', { x: 1, y: 2, z: 3 });
      entity.metadata = { custom: 'data' };

      const snapshot = EntitySerializer.toFullSnapshot(entity);

      expect(snapshot).toEqual(entity);
      expect(snapshot).not.toBe(entity); // Different object reference

      // Modify original
      entity.position.x = 999;
      entity.metadata!.custom = 'changed';

      // Snapshot should be unchanged
      expect(snapshot.position.x).toBe(1);
      expect(snapshot.metadata.custom).toBe('data');
    });
  });

  describe('round-trip serialization', () => {
    it('should preserve player data through serialization cycle', () => {
      const original = createPlayer('p1', 'u123', 'testuser', 'Test User', {
        x: 10,
        y: 64,
        z: 20,
      });
      original.velocity = { x: 0.1, y: 0, z: 0.1 };
      original.isCrouching = true;
      original.heldItem = 5;

      const json = EntitySerializer.toJSON(original);
      const deserialized = EntitySerializer.fromJSON(json);

      expect(deserialized).not.toBeNull();
      expect(deserialized!.id).toBe(original.id);
      expect(deserialized!.type).toBe(original.type);
      expect(deserialized!.username).toBe(original.username);
      expect(deserialized!.velocity).toEqual(original.velocity);
      expect(deserialized!.isCrouching).toBe(original.isCrouching);
      expect(deserialized!.heldItem).toBe(original.heldItem);
    });

    it('should preserve NPC data through serialization cycle', () => {
      const original = createNPC('npc1', 'Elder', { x: 5, y: 64, z: 10 }, 'models/elder.glb', 'dialog_1');
      original.aiState = 'idle';
      original.team = 'friendly';

      const json = EntitySerializer.toJSON(original);
      const deserialized = EntitySerializer.fromJSON(json);

      expect(deserialized).not.toBeNull();
      expect(deserialized!.id).toBe(original.id);
      expect(deserialized!.type).toBe(original.type);
      expect(deserialized!.displayName).toBe(original.displayName);
      expect(deserialized!.dialogId).toBe(original.dialogId);
      expect(deserialized!.aiState).toBe(original.aiState);
      expect(deserialized!.team).toBe(original.team);
    });

    it('should preserve item data through serialization cycle', () => {
      const original = createItem('i1', 42, { x: 1, y: 2, z: 3 }, 10);

      const json = EntitySerializer.toJSON(original);
      const deserialized = EntitySerializer.fromJSON(json);

      expect(deserialized).not.toBeNull();
      expect(deserialized!.id).toBe(original.id);
      expect(deserialized!.type).toBe(original.type);
      expect(deserialized!.itemTypeId).toBe(original.itemTypeId);
      expect(deserialized!.stackCount).toBe(original.stackCount);
    });

    it('should preserve array data through serialization cycle', () => {
      const original: EntityData[] = [
        createPlayer('p1', 'u1', 'player1', 'Player 1', { x: 1, y: 2, z: 3 }),
        createNPC('npc1', 'Merchant', { x: 4, y: 5, z: 6 }, 'model.glb'),
        createItem('i1', 10, { x: 7, y: 8, z: 9 }, 5),
      ];

      const json = EntitySerializer.arrayToJSON(original);
      const deserialized = EntitySerializer.arrayFromJSON(json);

      expect(deserialized).not.toBeNull();
      expect(deserialized).toHaveLength(3);
      expect(deserialized![0].type).toBe(EntityType.PLAYER);
      expect(deserialized![1].type).toBe(EntityType.NPC);
      expect(deserialized![2].type).toBe(EntityType.ITEM);
      expect(deserialized![2].stackCount).toBe(5);
    });
  });
});
