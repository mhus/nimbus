/**
 * StackModifierCreator - Central initialization of all StackModifiers
 *
 * This module creates all StackModifier instances centrally at application startup.
 * Services should only retrieve stacks via ModifierService.getModifierStack().
 *
 * Benefits:
 * - All stacks are guaranteed to exist
 * - No scattered lazy initialization checks
 * - Central documentation of all stacks
 * - Type-safe stack names via StackName enum
 */

import { getLogger } from '@nimbus/shared';
import type { AppContext } from '../types/AppContext';
import { StackName } from './ModifierService';

const logger = getLogger('StackModifierCreator');

/**
 * Creates all StackModifiers centrally
 *
 * Called once during application initialization in NimbusClient.ts
 * after ModifierService has been created.
 *
 * Note: Action callbacks may fail initially if referenced services
 * are not yet available. This is acceptable - ModifierStack.update()
 * handles errors gracefully with try-catch.
 *
 * @param appContext The application context
 */
export function createAllStackModifiers(appContext: AppContext): void {
  const modifierService = appContext.services.modifier;
  if (!modifierService) {
    logger.error('ModifierService not available - cannot create stacks');
    return;
  }

  logger.info('Creating all StackModifiers...');

  // ========================================
  // Player View Mode Stack
  // ========================================
  modifierService.createModifierStack<boolean>(
    StackName.PLAYER_VIEW_MODE,
    true, // Default: ego-view (first-person)
    (isEgo) => {
      // Callback may fail if PlayerService not ready yet - that's ok
      const playerService = appContext.services.player;
      if (playerService) {
        playerService.onViewModeChanged(isEgo);
      }
    }
  );

  // ========================================
  // Weitere Stacks hier hinzufügen
  // ========================================

  logger.info('All StackModifiers created', {
    stackCount: modifierService.stackNames.length,
    stacks: modifierService.stackNames,
  });
}
