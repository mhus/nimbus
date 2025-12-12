/**
 * useRegion Composable
 * Manages region selection state
 */

import { ref, computed } from 'vue';
import { regionService } from '../region/services/RegionService';
import type { Region } from '../region/services/RegionService';

// Shared state across all instances
const currentRegionId = ref<string | null>(null);
const regions = ref<Region[]>([]);
const loading = ref(false);
const error = ref<string | null>(null);

export function useRegion() {
  /**
   * Current region object
   */
  const currentRegion = computed(() => {
    return regions.value.find(r => r.name === currentRegionId.value);
  });

  /**
   * Load all regions from API
   */
  const loadRegions = async () => {
    loading.value = true;
    error.value = null;

    try {
      regions.value = await regionService.listRegions();
      console.log('[useRegion] Loaded regions', { count: regions.value.length });

      // Select first enabled region if none selected
      if (!currentRegionId.value && regions.value.length > 0) {
        const firstEnabled = regions.value.find(r => r.enabled);
        if (firstEnabled) {
          currentRegionId.value = firstEnabled.name;
          console.log('[useRegion] Auto-selected first enabled region', { regionId: currentRegionId.value });
        }
      }

      // Verify current region ID still exists
      if (currentRegionId.value && !regions.value.find(r => r.name === currentRegionId.value)) {
        // Fallback to first region if current ID not found
        if (regions.value.length > 0) {
          currentRegionId.value = regions.value[0].name;
          console.warn('[useRegion] Current region not found, using first region', { regionId: currentRegionId.value });
        }
      }
    } catch (err) {
      error.value = 'Failed to load regions';
      console.error('[useRegion] Failed to load regions', err);
    } finally {
      loading.value = false;
    }
  };

  /**
   * Select a region
   */
  const selectRegion = (regionName: string) => {
    if (!regions.value.find(r => r.name === regionName)) {
      console.warn('[useRegion] Cannot select non-existent region', { regionName });
      return;
    }

    currentRegionId.value = regionName;
    console.log('[useRegion] Selected region', { regionName });
  };

  return {
    currentRegion,
    currentRegionId,
    regions,
    loading,
    error,
    loadRegions,
    selectRegion,
  };
}
