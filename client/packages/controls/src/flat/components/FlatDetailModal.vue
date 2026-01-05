<template>
  <div class="modal modal-open">
    <div class="modal-box max-w-5xl">
      <!-- Header -->
      <div class="flex justify-between items-center mb-4">
        <h2 class="text-2xl font-bold">{{ flat?.flatId || 'Loading...' }}</h2>
        <button class="btn btn-sm btn-circle btn-ghost" @click="$emit('close')">✕</button>
      </div>

      <!-- Loading State -->
      <div v-if="loading" class="flex justify-center py-12">
        <span class="loading loading-spinner loading-lg"></span>
      </div>

      <!-- Error State -->
      <div v-else-if="error" class="alert alert-error">
        <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
        </svg>
        <span>{{ error }}</span>
      </div>

      <!-- Flat Details -->
      <template v-else-if="flat">
        <!-- Metadata -->
        <div class="grid grid-cols-2 gap-4 mb-6 p-4 bg-base-200 rounded-lg">
          <div>
            <span class="font-semibold">Size:</span>
            <span class="ml-2">{{ flat.sizeX }}x{{ flat.sizeZ }}</span>
          </div>
          <div>
            <span class="font-semibold">Mount:</span>
            <span class="ml-2">({{ flat.mountX }}, {{ flat.mountZ }})</span>
          </div>
          <div>
            <span class="font-semibold">Ocean Level:</span>
            <span class="ml-2">{{ flat.oceanLevel }}</span>
          </div>
          <div>
            <span class="font-semibold">Ocean Block:</span>
            <span class="ml-2">{{ flat.oceanBlockId }}</span>
          </div>
          <div>
            <span class="font-semibold">Layer Data ID:</span>
            <span class="ml-2">{{ flat.layerDataId }}</span>
          </div>
          <div>
            <span class="font-semibold">Unknown Protected:</span>
            <span class="ml-2">{{ flat.unknownProtected ? 'Yes' : 'No' }}</span>
          </div>
        </div>

        <!-- Visualizations -->
        <div class="space-y-6">
          <!-- Height Map -->
          <div class="bg-base-100 p-4 rounded-lg border border-base-300">
            <h3 class="text-lg font-semibold mb-3">Height Map</h3>
            <p class="text-sm text-base-content/70 mb-2">Blue (low) → Green (mid) → Red (high)</p>
            <div class="flex justify-center">
              <img
                :src="heightMapUrl"
                :alt="`Height map for ${flat.flatId}`"
                class="border border-base-300 bg-white max-w-full"
                style="image-rendering: pixelated;"
              />
            </div>
          </div>

          <!-- Block Map -->
          <div class="bg-base-100 p-4 rounded-lg border border-base-300">
            <h3 class="text-lg font-semibold mb-3">Block Map</h3>
            <p class="text-sm text-base-content/70 mb-2">Each color represents a different block type</p>
            <div class="flex justify-center">
              <img
                :src="blockMapUrl"
                :alt="`Block map for ${flat.flatId}`"
                class="border border-base-300 bg-white max-w-full"
                style="image-rendering: pixelated;"
              />
            </div>
          </div>
        </div>
      </template>
    </div>
    <div class="modal-backdrop" @click="$emit('close')"></div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue';
import { flatService, type FlatDetail } from '@/services/FlatService';
import { apiService } from '@/services/ApiService';

const props = defineProps<{
  flatId: string;
}>();

defineEmits<{
  close: [];
}>();

const flat = ref<FlatDetail | null>(null);
const loading = ref(false);
const error = ref<string | null>(null);

/**
 * Computed URLs for images
 */
const heightMapUrl = computed(() => {
  if (!props.flatId) return '';
  return `${apiService.getBaseUrl()}/control/flats/${encodeURIComponent(props.flatId)}/height-map`;
});

const blockMapUrl = computed(() => {
  if (!props.flatId) return '';
  return `${apiService.getBaseUrl()}/control/flats/${encodeURIComponent(props.flatId)}/block-map`;
});

/**
 * Load flat details
 */
const loadFlat = async () => {
  loading.value = true;
  error.value = null;

  try {
    flat.value = await flatService.getFlat(props.flatId);
  } catch (e: any) {
    console.error('[FlatDetailModal] Failed to load flat:', e);
    error.value = e.message;
  } finally {
    loading.value = false;
  }
};

// Watch for flatId changes
watch(() => props.flatId, () => {
  if (props.flatId) {
    loadFlat();
  }
}, { immediate: true });
</script>
