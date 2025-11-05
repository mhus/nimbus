/**
 * BlockUpdateHandler - Handles block update messages (b.u)
 *
 * Receives block updates from server and forwards to ChunkService
 * for updating individual blocks in loaded chunks.
 *
 * Block updates can be:
 * - New blocks
 * - Modified blocks
 * - Deleted blocks (blockTypeId: 0)
 */

import {
  BaseMessage,
  MessageType,
  type Block,
  getLogger,
} from '@nimbus/shared';
import { MessageHandler } from '../MessageHandler';
import type { ChunkService } from '../../services/ChunkService';

const logger = getLogger('BlockUpdateHandler');

/**
 * Handles BLOCK_UPDATE messages from server (b.u)
 */
export class BlockUpdateHandler extends MessageHandler<Block[]> {
  readonly messageType = MessageType.BLOCK_UPDATE;

  constructor(private chunkService: ChunkService) {
    super();
  }

  async handle(message: BaseMessage<Block[]>): Promise<void> {
    const blocks = message.d;

    logger.info('🔵 BLOCK UPDATE MESSAGE RECEIVED (b.u)', {
      messageType: message.t,
      blockCount: blocks?.length || 0,
      rawMessage: message,
    });

    if (!blocks || blocks.length === 0) {
      logger.warn('Received empty block update');
      return;
    }

    logger.info('Processing block updates', {
      count: blocks.length,
      blocks: blocks.map(b => ({
        position: b.position,
        blockTypeId: b.blockTypeId,
        isDelete: b.blockTypeId === 0,
      })),
    });

    // Forward to ChunkService (await to ensure BlockTypes are loaded)
    await this.chunkService.onBlockUpdate(blocks);

    logger.info('Block updates forwarded to ChunkService');
  }
}
