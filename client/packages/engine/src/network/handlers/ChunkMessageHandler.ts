/**
 * ChunkMessageHandler - Handles chunk update messages
 *
 * Receives chunk data from server and forwards to ChunkService
 * for processing and storage.
 */

import {
  BaseMessage,
  MessageType,
  ChunkDataTransferObject,
  getLogger,
} from '@nimbus/shared';
import { MessageHandler } from '../MessageHandler';
import type { ChunkService } from '../../services/ChunkService';

const logger = getLogger('ChunkMessageHandler');

/**
 * Handles CHUNK_UPDATE messages from server
 */
export class ChunkMessageHandler extends MessageHandler<ChunkDataTransferObject[]> {
  readonly messageType = MessageType.CHUNK_UPDATE;

  constructor(private chunkService: ChunkService) {
    super();
  }

  handle(message: BaseMessage<ChunkDataTransferObject[]>): void {
    const chunks = message.d;

    if (!chunks || chunks.length === 0) {
      logger.debug('Received empty chunk update');
      return;
    }

    logger.debug('Received chunk update', {
      count: chunks.length,
      chunks: chunks.map(c => ({ cx: c.cx, cz: c.cz, blocks: c.b.length })),
    });

    // Forward to ChunkService
    this.chunkService.onChunkUpdate(chunks);
  }
}
