/**
 * useWorld Composable
 * Manages world selection state
 */

import { ref, computed } from 'vue';
import { worldService, type WorldListResponse } from '../services/WorldService';
import { getLogger } from '@nimbus/shared';

const logger = getLogger('useWorld');

// Shared state across all instances
const currentWorldId = ref<string>(import.meta.env.VITE_WORLD_ID || 'test-world-1');
const worlds = ref<WorldListResponse[]>([]);
const loading = ref(false);
const error = ref<string | null>(null);

export function useWorld() {
  /**
   * Current world object
   */
  const currentWorld = computed(() => {
    return worlds.value.find(w => w.worldId === currentWorldId.value);
  });

  /**
   * Load all worlds from API
   */
  const loadWorlds = async () => {
    loading.value = true;
    error.value = null;

    try {
      worlds.value = await worldService.getWorlds();
      logger.info('Loaded worlds', { count: worlds.value.length });

      // Verify current world ID exists
      if (!worlds.value.find(w => w.worldId === currentWorldId.value)) {
        // Fallback to first world if current ID not found
        if (worlds.value.length > 0) {
          currentWorldId.value = worlds.value[0].worldId;
          logger.warn('Current world not found, using first world', { worldId: currentWorldId.value });
        }
      }
    } catch (err) {
      error.value = 'Failed to load worlds';
      logger.error('Failed to load worlds', {}, err as Error);
    } finally {
      loading.value = false;
    }
  };

  /**
   * Select a world
   */
  const selectWorld = (worldId: string) => {
    if (!worlds.value.find(w => w.worldId === worldId)) {
      logger.warn('Cannot select non-existent world', { worldId });
      return;
    }

    currentWorldId.value = worldId;
    logger.info('Selected world', { worldId });
  };

  return {
    currentWorld,
    currentWorldId,
    worlds,
    loading,
    error,
    loadWorlds,
    selectWorld,
  };
}
