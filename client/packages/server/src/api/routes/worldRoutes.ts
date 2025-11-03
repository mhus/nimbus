import { Router } from 'express';
import type { WorldManager } from '../../world/WorldManager';
import type { BlockUpdateBuffer } from '../../network/BlockUpdateBuffer';
import type { ClientSession } from '../../types/ServerTypes';
import { EditAction, MessageType } from '@nimbus/shared';

export function createWorldRoutes(
  worldManager: WorldManager,
  blockUpdateBuffer: BlockUpdateBuffer,
  sessions: Map<string, ClientSession>
): Router {
  const router = Router();

  // GET /api/worlds - List all worlds
  router.get('/', (_req, res) => {
    const worlds = worldManager.getAllWorlds();
    res.json(worlds.map(w => ({
      worldId: w.worldId,
      name: w.name,
      description: w.description,
      chunkSize: w.chunkSize,
      status: w.status,
    })));
  });

  // GET /api/worlds/:id - Get world details
  router.get('/:id', (req, res) => {
    const world = worldManager.getWorld(req.params.id);
    if (!world) {
      return res.status(404).json({ error: 'World not found' });
    }
    return res.json(world);
  });

  // GET /api/worlds/:id/blocktypes - Get all BlockTypes or search
  router.get('/:id/blocktypes', (req, res) => {
    const world = worldManager.getWorld(req.params.id);
    if (!world) {
      return res.status(404).json({ error: 'World not found' });
    }

    const query = req.query.query as string | undefined;
    const registry = worldManager.getBlockTypeRegistry();

    if (query) {
      // Search BlockTypes
      const blockTypes = registry.searchBlockTypes(query);
      return res.json({ blockTypes });
    } else {
      // Get all BlockTypes
      const blockTypes = registry.getAllBlockTypes();
      return res.json({ blockTypes });
    }
  });

  // GET /api/worlds/:id/blocktypes/:blockId - Get single BlockType
  router.get('/:id/blocktypes/:blockId', (req, res) => {
    const blockType = worldManager.getBlockTypeRegistry().getBlockType(Number(req.params.blockId));
    if (!blockType) {
      return res.status(404).json({ error: 'BlockType not found' });
    }
    return res.json(blockType);
  });

  // GET /api/worlds/:id/blocktypes/:from/:to - Get BlockType range
  router.get('/:id/blocktypes/:from/:to', (req, res) => {
    const from = Number(req.params.from);
    const to = Number(req.params.to);
    const blockTypes = worldManager.getBlockTypeRegistry().getBlockTypeRange(from, to);
    res.json(blockTypes);
  });

  // POST /api/worlds/:id/blocktypes - Create new BlockType
  router.post('/:id/blocktypes', (req, res) => {
    const world = worldManager.getWorld(req.params.id);
    if (!world) {
      return res.status(404).json({ error: 'World not found' });
    }

    const blockTypeData = req.body;
    const registry = worldManager.getBlockTypeRegistry();

    // If no ID provided, get next available
    if (!blockTypeData.id) {
      blockTypeData.id = registry.getNextAvailableId();
    }

    // Validate required fields
    if (!blockTypeData.modifiers || Object.keys(blockTypeData.modifiers).length === 0) {
      return res.status(400).json({ error: 'BlockType must have at least one modifier (status 0)' });
    }

    // Create BlockType
    const createdBlockType = registry.createBlockType(blockTypeData);

    if (!createdBlockType) {
      return res.status(400).json({ error: 'Failed to create BlockType (may already exist)' });
    }

    return res.status(201).json({ id: createdBlockType.id });
  });

  // PUT /api/worlds/:id/blocktypes/:blockTypeId - Update BlockType
  router.put('/:id/blocktypes/:blockTypeId', (req, res) => {
    const world = worldManager.getWorld(req.params.id);
    if (!world) {
      return res.status(404).json({ error: 'World not found' });
    }

    const blockTypeId = Number(req.params.blockTypeId);
    const blockTypeData = req.body;
    const registry = worldManager.getBlockTypeRegistry();

    // Ensure ID matches URL parameter
    blockTypeData.id = blockTypeId;

    // Validate required fields
    if (!blockTypeData.modifiers || Object.keys(blockTypeData.modifiers).length === 0) {
      return res.status(400).json({ error: 'BlockType must have at least one modifier (status 0)' });
    }

    // Update BlockType
    const updatedBlockType = registry.updateBlockType(blockTypeData);

    if (!updatedBlockType) {
      return res.status(404).json({ error: 'BlockType not found' });
    }

    return res.json(updatedBlockType);
  });

  // DELETE /api/worlds/:id/blocktypes/:blockTypeId - Delete BlockType
  router.delete('/:id/blocktypes/:blockTypeId', (req, res) => {
    const world = worldManager.getWorld(req.params.id);
    if (!world) {
      return res.status(404).json({ error: 'World not found' });
    }

    const blockTypeId = Number(req.params.blockTypeId);
    const registry = worldManager.getBlockTypeRegistry();

    // Delete BlockType
    const deleted = registry.deleteBlockType(blockTypeId);

    if (!deleted) {
      return res.status(404).json({ error: 'BlockType not found' });
    }

    return res.status(204).send();
  });

  // GET /api/worlds/:id/blocks/:x/:y/:z - Get block at position
  router.get('/:id/blocks/:x/:y/:z', async (req, res) => {
    const world = worldManager.getWorld(req.params.id);
    if (!world) {
      return res.status(404).json({ error: 'World not found' });
    }

    const x = Number(req.params.x);
    const y = Number(req.params.y);
    const z = Number(req.params.z);

    if (isNaN(x) || isNaN(y) || isNaN(z)) {
      return res.status(400).json({ error: 'Invalid coordinates' });
    }

    const block = await worldManager.getBlock(req.params.id, x, y, z);

    if (!block) {
      return res.status(404).json({ error: 'Block not found at position' });
    }

    return res.json(block);
  });

  // POST /api/worlds/:id/blocks/:x/:y/:z - Create block at position
  router.post('/:id/blocks/:x/:y/:z', async (req, res) => {
    const world = worldManager.getWorld(req.params.id);
    if (!world) {
      return res.status(404).json({ error: 'World not found' });
    }

    const x = Number(req.params.x);
    const y = Number(req.params.y);
    const z = Number(req.params.z);

    if (isNaN(x) || isNaN(y) || isNaN(z)) {
      return res.status(400).json({ error: 'Invalid coordinates' });
    }

    // Validate request body
    const blockData = req.body;
    if (!blockData.blockTypeId) {
      return res.status(400).json({ error: 'blockTypeId is required' });
    }

    // Verify blockTypeId exists
    const blockType = worldManager.getBlockTypeRegistry().getBlockType(blockData.blockTypeId);
    if (!blockType) {
      return res.status(400).json({ error: 'Invalid blockTypeId' });
    }

    // Check if block already exists at position
    const existingBlock = await worldManager.getBlock(req.params.id, x, y, z);
    if (existingBlock) {
      return res.status(400).json({ error: 'Block already exists at position' });
    }

    // Create block
    const block = {
      position: { x, y, z },
      blockTypeId: blockData.blockTypeId,
      status: blockData.status,
      offsets: blockData.offsets,
      faceVisibility: blockData.faceVisibility,
      modifiers: blockData.modifiers,
      metadata: blockData.metadata,
    };

    const success = await worldManager.setBlock(req.params.id, block);

    if (!success) {
      return res.status(500).json({ error: 'Failed to create block' });
    }

    // Add to update buffer for broadcasting to clients
    console.log('🔵 REST API: Adding block to update buffer', {
      worldId: req.params.id,
      position: block.position,
      blockTypeId: block.blockTypeId,
    });
    blockUpdateBuffer.addUpdate(req.params.id, block);

    return res.status(201).json(block);
  });

  // PUT /api/worlds/:id/blocks/:x/:y/:z - Update block at position
  router.put('/:id/blocks/:x/:y/:z', async (req, res) => {
    const world = worldManager.getWorld(req.params.id);
    if (!world) {
      return res.status(404).json({ error: 'World not found' });
    }

    const x = Number(req.params.x);
    const y = Number(req.params.y);
    const z = Number(req.params.z);

    if (isNaN(x) || isNaN(y) || isNaN(z)) {
      return res.status(400).json({ error: 'Invalid coordinates' });
    }

    // Check if block exists
    const existingBlock = await worldManager.getBlock(req.params.id, x, y, z);
    if (!existingBlock) {
      return res.status(404).json({ error: 'Block not found at position' });
    }

    // Validate request body
    const blockData = req.body;
    if (!blockData.blockTypeId) {
      return res.status(400).json({ error: 'blockTypeId is required' });
    }

    // Verify blockTypeId exists
    const blockType = worldManager.getBlockTypeRegistry().getBlockType(blockData.blockTypeId);
    if (!blockType) {
      return res.status(400).json({ error: 'Invalid blockTypeId' });
    }

    // Update block (preserve existing values if not provided)
    const block = {
      position: { x, y, z },
      blockTypeId: blockData.blockTypeId,
      status: blockData.status !== undefined ? blockData.status : existingBlock.status,
      offsets: blockData.offsets !== undefined ? blockData.offsets : existingBlock.offsets,
      faceVisibility: blockData.faceVisibility !== undefined ? blockData.faceVisibility : existingBlock.faceVisibility,
      modifiers: blockData.modifiers !== undefined ? blockData.modifiers : existingBlock.modifiers,
      metadata: blockData.metadata !== undefined ? blockData.metadata : existingBlock.metadata,
    };

    const success = await worldManager.setBlock(req.params.id, block);

    if (!success) {
      return res.status(500).json({ error: 'Failed to update block' });
    }

    // Add to update buffer for broadcasting to clients
    console.log('🔵 REST API (PUT): Adding block to update buffer', {
      worldId: req.params.id,
      position: block.position,
      blockTypeId: block.blockTypeId,
    });
    blockUpdateBuffer.addUpdate(req.params.id, block);

    return res.json(block);
  });

  // DELETE /api/worlds/:id/blocks/:x/:y/:z - Delete block at position
  router.delete('/:id/blocks/:x/:y/:z', async (req, res) => {
    const world = worldManager.getWorld(req.params.id);
    if (!world) {
      return res.status(404).json({ error: 'World not found' });
    }

    const x = Number(req.params.x);
    const y = Number(req.params.y);
    const z = Number(req.params.z);

    if (isNaN(x) || isNaN(y) || isNaN(z)) {
      return res.status(400).json({ error: 'Invalid coordinates' });
    }

    const deleted = await worldManager.deleteBlock(req.params.id, x, y, z);

    if (!deleted) {
      return res.status(404).json({ error: 'Block not found at position' });
    }

    // Broadcast deletion as block with blockTypeId: 0
    const deletionBlock = {
      position: { x, y, z },
      blockTypeId: 0, // 0 = deletion
      status: 0,
      metadata: {},
    };
    console.log('🔵 REST API (DELETE): Adding deletion to update buffer', {
      worldId: req.params.id,
      position: { x, y, z },
      blockTypeId: 0,
    });
    blockUpdateBuffer.addUpdate(req.params.id, deletionBlock);

    return res.status(204).send();
  });

  // GET /api/worlds/:worldId/session/:sessionId/editAction - Get current editAction
  router.get('/:worldId/session/:sessionId/editAction', (req, res) => {
    const session = sessions.get(req.params.sessionId);
    if (!session) {
      return res.status(404).json({ error: 'Session not found' });
    }

    return res.json({ editAction: session.editAction || EditAction.OPEN_CONFIG_DIALOG });
  });

  // PUT /api/worlds/:worldId/session/:sessionId/editAction - Set editAction
  router.put('/:worldId/session/:sessionId/editAction', (req, res) => {
    const session = sessions.get(req.params.sessionId);
    if (!session) {
      return res.status(404).json({ error: 'Session not found' });
    }

    const { editAction } = req.body;

    // Validate editAction
    if (!editAction || !Object.values(EditAction).includes(editAction)) {
      return res.status(400).json({ error: 'Invalid editAction. Must be one of: ' + Object.values(EditAction).join(', ') });
    }

    session.editAction = editAction;

    return res.json({ editAction: session.editAction });
  });

  // GET /api/worlds/:worldId/session/:sessionId/selectedEditBlock - Get selectedEditBlock and markedEditBlock
  router.get('/:worldId/session/:sessionId/selectedEditBlock', (req, res) => {
    const session = sessions.get(req.params.sessionId);
    if (!session) {
      return res.status(404).json({ error: 'Session not found' });
    }

    return res.json({
      selectedEditBlock: session.selectedEditBlock || null,
      markedEditBlock: session.markedEditBlock || null,
    });
  });

  // PUT /api/worlds/:worldId/session/:sessionId/selectedEditBlock - Set selectedEditBlock and execute action
  router.put('/:worldId/session/:sessionId/selectedEditBlock', async (req, res) => {
    const session = sessions.get(req.params.sessionId);
    if (!session) {
      return res.status(404).json({ error: 'Session not found' });
    }

    const { x, y, z } = req.body;

    // If no coordinates provided, clear selection
    if (x === undefined && y === undefined && z === undefined) {
      session.selectedEditBlock = null;

      // Send setSelectedEditBlock to client to clear highlight
      const requestId = `scmd_${Date.now()}_${Math.random().toString(36).substring(7)}`;
      session.ws.send(JSON.stringify({
        i: requestId,
        t: MessageType.SCMD,
        d: { cmd: 'setSelectedEditBlock', args: [] },
      }));

      return res.json({ success: true, selectedEditBlock: null });
    }

    // Validate coordinates
    if (typeof x !== 'number' || typeof y !== 'number' || typeof z !== 'number') {
      return res.status(400).json({ error: 'Invalid coordinates. Expected numbers for x, y, z' });
    }

    const position = { x, y, z };
    session.selectedEditBlock = position;

    // Send setSelectedEditBlock to client to show green highlight
    const requestId = `scmd_${Date.now()}_${Math.random().toString(36).substring(7)}`;
    session.ws.send(JSON.stringify({
      i: requestId,
      t: MessageType.SCMD,
      d: {
        cmd: 'setSelectedEditBlock',
        args: [x.toString(), y.toString(), z.toString()],
      },
    }));

    // Execute action based on editAction
    const action = session.editAction || EditAction.OPEN_CONFIG_DIALOG;
    try {
      await executeEditAction(session, action, position, worldManager);
    } catch (error) {
      return res.status(500).json({
        error: 'Action execution failed',
        message: error instanceof Error ? error.message : 'Unknown error',
      });
    }

    return res.json({ success: true, selectedEditBlock: position });
  });

  return router;
}

/**
 * Execute edit action for REST API endpoint
 */
async function executeEditAction(
  session: ClientSession,
  action: EditAction,
  position: { x: number; y: number; z: number },
  worldManager: WorldManager
): Promise<void> {
  switch (action) {
    case EditAction.OPEN_CONFIG_DIALOG:
      // Send openComponent command to client
      sendClientCommand(session, 'openComponent', ['edit_config']);
      break;

    case EditAction.OPEN_EDITOR:
      // Send openComponent command to client with position
      sendClientCommand(session, 'openComponent', [
        'block_editor',
        position.x.toString(),
        position.y.toString(),
        position.z.toString(),
      ]);
      break;

    case EditAction.MARK_BLOCK:
      // Store marked block (server-side only)
      session.markedEditBlock = position;
      break;

    case EditAction.COPY_BLOCK:
      await executeCopyBlock(session, position, worldManager);
      break;

    case EditAction.DELETE_BLOCK:
      await executeDeleteBlock(session, position, worldManager);
      break;

    case EditAction.MOVE_BLOCK:
      await executeMoveBlock(session, position, worldManager);
      break;

    default:
      throw new Error(`Unknown action: ${action}`);
  }
}

/**
 * Send command to client (SCMD) - fire and forget
 */
function sendClientCommand(session: ClientSession, cmd: string, args: string[]): void {
  const requestId = `scmd_${Date.now()}_${Math.random().toString(36).substring(7)}`;
  session.ws.send(JSON.stringify({
    i: requestId,
    t: MessageType.SCMD,
    d: { cmd, args },
  }));
}

/**
 * Execute COPY_BLOCK action
 */
async function executeCopyBlock(
  session: ClientSession,
  targetPosition: { x: number; y: number; z: number },
  worldManager: WorldManager
): Promise<void> {
  if (!session.markedEditBlock) {
    throw new Error('No block marked for copy. Use MARK_BLOCK action first.');
  }

  const worldId = session.worldId;
  if (!worldId) {
    throw new Error('No world selected');
  }

  const sourceBlock = await worldManager.getBlock(
    worldId,
    session.markedEditBlock.x,
    session.markedEditBlock.y,
    session.markedEditBlock.z
  );

  if (!sourceBlock) {
    throw new Error(`No block found at marked position (${session.markedEditBlock.x}, ${session.markedEditBlock.y}, ${session.markedEditBlock.z})`);
  }

  const targetBlock = {
    ...sourceBlock,
    position: targetPosition,
  };

  const success = await worldManager.setBlock(worldId, targetBlock);
  if (!success) {
    throw new Error('Failed to copy block');
  }
}

/**
 * Execute DELETE_BLOCK action
 */
async function executeDeleteBlock(
  session: ClientSession,
  position: { x: number; y: number; z: number },
  worldManager: WorldManager
): Promise<void> {
  const worldId = session.worldId;
  if (!worldId) {
    throw new Error('No world selected');
  }

  console.log(`[DELETE_BLOCK REST] Attempting to delete block at (${position.x}, ${position.y}, ${position.z}) in world ${worldId}`);

  const success = await worldManager.deleteBlock(worldId, position.x, position.y, position.z);
  if (!success) {
    console.error(`[DELETE_BLOCK REST] Failed to delete block at (${position.x}, ${position.y}, ${position.z})`);
    throw new Error(`Failed to delete block at (${position.x}, ${position.y}, ${position.z}) - block may not exist`);
  }

  console.log(`[DELETE_BLOCK REST] Successfully deleted block at (${position.x}, ${position.y}, ${position.z})`);

  // Clear selection after deletion
  session.selectedEditBlock = null;
  sendClientCommand(session, 'setSelectedEditBlock', []);
}

/**
 * Execute MOVE_BLOCK action
 */
async function executeMoveBlock(
  session: ClientSession,
  targetPosition: { x: number; y: number; z: number },
  worldManager: WorldManager
): Promise<void> {
  if (!session.markedEditBlock) {
    throw new Error('No block marked for move. Use MARK_BLOCK action first.');
  }

  const worldId = session.worldId;
  if (!worldId) {
    throw new Error('No world selected');
  }

  const sourceBlock = await worldManager.getBlock(
    worldId,
    session.markedEditBlock.x,
    session.markedEditBlock.y,
    session.markedEditBlock.z
  );

  if (!sourceBlock) {
    throw new Error(`No block found at marked position (${session.markedEditBlock.x}, ${session.markedEditBlock.y}, ${session.markedEditBlock.z})`);
  }

  const targetBlock = {
    ...sourceBlock,
    position: targetPosition,
  };

  const copySuccess = await worldManager.setBlock(worldId, targetBlock);
  if (!copySuccess) {
    throw new Error('Failed to copy block to new position');
  }

  const deleteSuccess = await worldManager.deleteBlock(
    worldId,
    session.markedEditBlock.x,
    session.markedEditBlock.y,
    session.markedEditBlock.z
  );

  if (!deleteSuccess) {
    throw new Error(`Block copied but failed to delete old position at (${session.markedEditBlock.x}, ${session.markedEditBlock.y}, ${session.markedEditBlock.z})`);
  }

  session.markedEditBlock = null;
}
