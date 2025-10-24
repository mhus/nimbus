/**
 * Chunk Commands
 * Commands for chunk re-rendering and management
 */
import type { Command } from '../Command';
import type { CmdExecutionContext } from '../CmdExecutionContext';
import type { VoxelClient } from '../../VoxelClient';

/**
 * Rechunk Command - Re-render current chunk
 */
export class RechunkCommand implements Command {
  private client: VoxelClient;

  constructor(client: VoxelClient) {
    this.client = client;
  }

  getName(): string {
    return 'rechunk';
  }

  getDescription(): string {
    return 'Re-render the current chunk';
  }

  getHelp(): string {
    return `Usage: rechunk

Re-renders the chunk at the player's current position.
This forces a complete rebuild of the chunk mesh, useful after
making changes to block rendering or materials.

Examples:
  rechunk       - Re-render chunk at current position`;
  }

  execute(context: CmdExecutionContext): void {
    const chunkManager = (this.client as any).chunkManager;

    if (!chunkManager) {
      context.writeError('Chunk manager not available');
      return;
    }

    try {
      chunkManager.rerenderCurrentChunk();
      context.writeLine('Re-rendering current chunk...');
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : String(error);
      context.writeError(`Failed to re-render chunk: ${errorMessage}`);
    }
  }
}

/**
 * Rechunk All Command - Re-render all visible chunks
 */
export class RechunkAllCommand implements Command {
  private client: VoxelClient;

  constructor(client: VoxelClient) {
    this.client = client;
  }

  getName(): string {
    return 'rechunkall';
  }

  getDescription(): string {
    return 'Re-render all visible chunks';
  }

  getHelp(): string {
    return `Usage: rechunkall

Re-renders all chunks within the current render distance.
This forces a complete rebuild of all visible chunk meshes.

Note: This operation may cause temporary performance impact
while chunks are being rebuilt.

Examples:
  rechunkall    - Re-render all visible chunks`;
  }

  execute(context: CmdExecutionContext): void {
    const chunkManager = (this.client as any).chunkManager;

    if (!chunkManager) {
      context.writeError('Chunk manager not available');
      return;
    }

    try {
      const count = chunkManager.rerenderVisibleChunks();
      context.writeLine(`Re-rendering ${count} visible chunks...`);
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : String(error);
      context.writeError(`Failed to re-render chunks: ${errorMessage}`);
    }
  }
}
