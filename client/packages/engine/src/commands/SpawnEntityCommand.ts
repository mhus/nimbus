/**
 * SpawnEntityCommand - Spawns a test entity in the world
 *
 * Usage: spawnEntity <id> <entityModelId> <x> <y> <z>
 *
 * Creates a test entity at the specified position with the given model.
 * The entity will be added to the EntityService cache and rendered if within visibility range.
 */

import { CommandHandler } from './CommandHandler';
import type { AppContext } from '../AppContext';
import { createEntity, createEntityPathway, toNumber } from '@nimbus/shared';

/**
 * SpawnEntity command - Spawn a test entity in the world
 */
export class SpawnEntityCommand extends CommandHandler {
  private appContext: AppContext;

  constructor(appContext: AppContext) {
    super();
    this.appContext = appContext;
  }

  name(): string {
    return 'spawnEntity';
  }

  description(): string {
    return 'Spawns a test entity at position. Usage: spawnEntity <id> <entityModelId> <x> <y> <z>';
  }

  async execute(parameters: any[]): Promise<any> {
    // Validate parameters
    if (parameters.length !== 5) {
      console.error('Usage: spawnEntity <id> <entityModelId> <x> <y> <z>');
      return {
        error: 'Invalid parameters',
        usage: 'spawnEntity <id> <entityModelId> <x> <y> <z>',
        example: 'spawnEntity test_cow1 cow1 10 64 5',
      };
    }

    const entityId = parameters[0];
    const entityModelId = parameters[1];
    const x = toNumber(parameters[2]);
    const y = toNumber(parameters[3]);
    const z = toNumber(parameters[4]);

    if (isNaN(x) || isNaN(y) || isNaN(z)) {
      console.error('Coordinates must be numbers');
      return {
        error: 'Invalid coordinates',
        x: parameters[2],
        y: parameters[3],
        z: parameters[4],
      };
    }

    const entityService = this.appContext.services.entity;

    if (!entityService) {
      console.error('EntityService not available');
      return { error: 'EntityService not available' };
    }

    try {
      console.log(`Spawning entity '${entityId}' with model '${entityModelId}' at (${x}, ${y}, ${z})...`);

      // Load entity model first to verify it exists
      const entityModel = await entityService.getEntityModel(entityModelId);
      if (!entityModel) {
        console.error(`Entity model '${entityModelId}' not found`);
        console.log('');
        console.log('Available entity models can be loaded from the server.');
        console.log('Make sure the model file exists in files/entitymodels/ directory.');
        return {
          error: 'Entity model not found',
          entityModelId,
        };
      }

      console.log(`✓ Entity model '${entityModelId}' loaded`);

      // Create entity instance
      const entity = createEntity(entityId, entityModelId, entityModelId, 'passive');

      // Load entity (this creates a ClientEntity and adds it to cache)
      // We manually create the entity in the cache since it's a test entity
      const clientEntity = await entityService.getEntity(entityId);

      // If entity doesn't exist yet in cache, we need to create it manually
      if (!clientEntity) {
        // Create a test pathway with a single waypoint at the spawn position
        const pathway = createEntityPathway(
          entityId,
          Date.now(),
          [
            {
              timestamp: Date.now(),
              target: { x, y, z },
              rotation: { y: 0, p: 0 },
              pose: 0,
            },
          ],
          false // not looping
        );

        // Set the pathway (this will auto-load the entity if it doesn't exist)
        await entityService.setEntityPathway(pathway);

        console.log(`✓ Entity '${entityId}' spawned at (${x}, ${y}, ${z})`);
        console.log('');

        // Get player position to calculate distance
        const playerService = this.appContext.services.player;
        if (playerService) {
          const playerPos = playerService.getPosition();
          const distance = Math.sqrt(
            Math.pow(x - playerPos.x, 2) +
            Math.pow(y - playerPos.y, 2) +
            Math.pow(z - playerPos.z, 2)
          );

          console.log(`Distance to player: ${distance.toFixed(2)} blocks`);
          console.log(`Visibility radius: ${entityService.visibilityRadius} blocks`);

          if (distance <= entityService.visibilityRadius) {
            console.log('✓ Entity is within visibility range and should be visible');
          } else {
            console.log('⚠ Entity is outside visibility range and will not be visible');
            console.log(`  Move closer or increase visibility radius with: entityService.visibilityRadius = ${Math.ceil(distance)}`);
          }
        }

        console.log('');
        console.log('Use "entityInfo ' + entityId + '" to view entity details');
        console.log('Use "listEntities" to see all loaded entities');

        return {
          success: true,
          entityId,
          entityModelId,
          position: { x, y, z },
          message: 'Entity spawned successfully',
        };
      } else {
        console.error(`Entity '${entityId}' already exists`);
        return {
          error: 'Entity already exists',
          entityId,
        };
      }
    } catch (error) {
      console.error('Failed to spawn entity:', error);
      return {
        error: 'Failed to spawn entity',
        message: error instanceof Error ? error.message : 'Unknown error',
      };
    }
  }
}
