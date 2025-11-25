/**
 * EffectParameterUpdateHandler - Handles effect parameter update messages from server
 *
 * Receives parameter updates for running effects from server (synchronized from other clients)
 * and updates the local effect parameters.
 */

import {
  BaseMessage,
  MessageType,
  type EffectParameterUpdateData,
  getLogger,
} from '@nimbus/shared';
import { MessageHandler } from '../MessageHandler';
import type { ScrawlService } from '../../scrawl/ScrawlService';

const logger = getLogger('EffectParameterUpdateHandler');

/**
 * Handles EFFECT_PARAMETER_UPDATE messages from server (ef.p.u)
 */
export class EffectParameterUpdateHandler extends MessageHandler<EffectParameterUpdateData> {
  readonly messageType = MessageType.EFFECT_PARAMETER_UPDATE;

  constructor(private scrawlService: ScrawlService) {
    super();
  }

  async handle(message: BaseMessage<EffectParameterUpdateData>): Promise<void> {
    const data = message.d;

    if (!data) {
      logger.warn('Effect parameter update message without data');
      return;
    }

    logger.debug('Effect parameter update received from server', {
      effectId: data.effectId,
      paramName: data.paramName,
      value: data.value,
      hasTargeting: !!data.targeting,
    });

    if (!data.effectId || !data.paramName) {
      logger.warn('Invalid effect parameter update data');
      return;
    }

    try {
      // Update the executor parameter with optional targeting context
      // The effectId from server corresponds to the executor ID
      this.scrawlService.updateExecutorParameter(data.effectId, data.paramName, data.value, data.targeting);

      logger.debug('Remote parameter update applied', {
        effectId: data.effectId,
        paramName: data.paramName,
        hasTargeting: !!data.targeting,
      });
    } catch (error) {
      logger.warn('Failed to apply remote parameter update', {
        effectId: data.effectId,
        paramName: data.paramName,
        error: (error as Error).message,
      });
    }
  }
}
