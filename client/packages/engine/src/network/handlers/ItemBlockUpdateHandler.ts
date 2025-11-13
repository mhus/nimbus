/**
 * ItemBlockUpdateHandler - Handles item block update messages (b.iu)
 *
 * Receives item block updates from server and forwards to ChunkService
 * for updating individual items in loaded chunks.
 *
 * Item updates can be:
 * - New items (blockTypeId: 1)
 * - Modified items (blockTypeId: 1, replaces existing item)
 * - Deleted items (blockTypeId: 0, only if item exists)
 *
 * Items can only exist at AIR positions or replace existing items.
 */

import {
  BaseMessage,
  MessageType,
  type Block,
  getLogger,
} from '@nimbus/shared';
import { MessageHandler } from '../MessageHandler';
import type { ChunkService } from '../../services/ChunkService';

const logger = getLogger('ItemBlockUpdateHandler');

/**
 * Handles ITEM_BLOCK_UPDATE messages from server (b.iu)
 */
export class ItemBlockUpdateHandler extends MessageHandler<Block[]> {
  readonly messageType = MessageType.ITEM_BLOCK_UPDATE;

  constructor(private chunkService: ChunkService) {
    super();
  }

  async handle(message: BaseMessage<Block[]>): Promise<void> {
    const items = message.d;

    logger.info('🔵 ITEM BLOCK UPDATE MESSAGE RECEIVED (b.iu)', {
      messageType: message.t,
      itemCount: items?.length || 0,
      rawMessage: message,
    });

    if (!items || items.length === 0) {
      logger.warn('Received empty item block update');
      return;
    }

    logger.info('Processing item updates', {
      count: items.length,
      items: items.map(item => ({
        position: item.position,
        blockTypeId: item.blockTypeId,
        itemId: item.metadata?.id,
        displayName: item.metadata?.displayName,
        isDelete: item.blockTypeId === 0,
      })),
    });

    // Forward to ChunkService (await to ensure BlockTypes are loaded)
    await this.chunkService.onItemBlockUpdate(items);

    logger.info('Item updates forwarded to ChunkService');
  }
}
