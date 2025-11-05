<template>
  <Teleport to="body">
    <div class="fixed inset-0 z-[70] flex items-center justify-center p-4" @click.self="emit('close')">
      <!-- Backdrop -->
      <div class="absolute inset-0 bg-black bg-opacity-50" @click="emit('close')"></div>

      <!-- Modal Content -->
      <div class="relative w-full max-w-4xl bg-base-100 rounded-2xl shadow-2xl p-6 max-h-[90vh] overflow-hidden flex flex-col">
        <h2 class="text-2xl font-bold mb-4">
          Select Asset
        </h2>

        <!-- Search -->
        <div class="mb-4">
          <SearchInput
            v-model="localSearchQuery"
            placeholder="Search assets..."
            @search="handleSearch"
          />
        </div>

        <!-- Loading State -->
        <LoadingSpinner v-if="loading && assets.length === 0" />

        <!-- Error State -->
        <ErrorAlert v-else-if="error" :message="error" />

        <!-- Empty State -->
        <div v-else-if="!loading && assets.length === 0" class="text-center py-12">
          <p class="text-base-content/70">No assets found</p>
        </div>

        <!-- Asset Grid -->
        <div v-else class="overflow-y-auto flex-1 -mx-2 px-2">
          <div class="grid grid-cols-3 sm:grid-cols-4 md:grid-cols-5 gap-3">
            <div
              v-for="asset in assets"
              :key="asset.path"
              class="card bg-base-200 shadow hover:shadow-lg transition-shadow cursor-pointer"
              @click="selectAsset(asset)"
            >
              <figure class="h-24 bg-base-300 flex items-center justify-center overflow-hidden">
                <img
                  v-if="isImage(asset)"
                  :src="getAssetUrl(asset.path)"
                  :alt="asset.path"
                  class="w-full h-full object-cover"
                  @error="(e: Event) => ((e.target as HTMLImageElement).style.display = 'none')"
                />
                <span v-else class="text-4xl">{{ getIcon(asset) }}</span>
              </figure>
              <div class="card-body p-2">
                <h3 class="text-xs font-medium truncate" :title="asset.path">
                  {{ getFileName(asset.path) }}
                </h3>
              </div>
            </div>
          </div>
        </div>

        <!-- Actions -->
        <div class="mt-4 flex justify-end gap-2 border-t pt-4">
          <button class="btn btn-ghost" @click="emit('close')">
            Cancel
          </button>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import type { Asset } from '../services/AssetService';
import { useAssets } from '../composables/useAssets';
import SearchInput from './SearchInput.vue';
import LoadingSpinner from './LoadingSpinner.vue';
import ErrorAlert from './ErrorAlert.vue';

interface Props {
  worldId: string;
  currentPath?: string; // Currently selected path (optional)
}

const props = defineProps<Props>();

const emit = defineEmits<{
  (e: 'close'): void;
  (e: 'select', path: string): void;
}>();

const { assets, loading, error, loadAssets, searchAssets, getAssetUrl, isImage, getIcon } = useAssets(props.worldId);

const localSearchQuery = ref('');

onMounted(() => {
  loadAssets();
});

const handleSearch = (query: string) => {
  searchAssets(query);
};

const selectAsset = (asset: Asset) => {
  emit('select', asset.path);
  emit('close');
};

const getFileName = (path: string): string => {
  const parts = path.split('/');
  return parts[parts.length - 1];
};
</script>
