<template>
  <div class="min-h-screen flex flex-col">
    <!-- Header -->
    <header class="navbar bg-base-200 shadow-lg">
      <div class="flex-none">
        <a href="/controls/index.html" class="btn btn-ghost btn-square">
          <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6" />
          </svg>
        </a>
      </div>
      <div class="flex-1">
        <h1 class="text-xl font-bold px-4">Nimbus World Editor</h1>
      </div>
      <div class="flex-none">
        <!-- Region Selector -->
        <RegionSelector />
      </div>
    </header>

    <!-- Main Content -->
    <main class="flex-1 container mx-auto px-4 py-6">
      <!-- Show message if no region selected -->
      <div v-if="!currentRegionId" class="alert alert-info">
        <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
        </svg>
        <span>Please select a region from the dropdown above</span>
      </div>

      <!-- World List or Editor -->
      <div v-else>
        <WorldList
          v-if="!selectedWorld"
          @select="handleWorldSelect"
          @create="handleCreateNew"
        />
        <WorldEditor
          v-else
          :world="selectedWorld"
          @back="handleBack"
          @saved="handleSaved"
        />
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onMounted } from 'vue';
import { useRegion } from '@/composables/useRegion';
import RegionSelector from './components/RegionSelector.vue';
import WorldList from './views/WorldList.vue';
import WorldEditor from './views/WorldEditor.vue';
import { worldServiceFrontend, type World } from './services/WorldServiceFrontend';

const { currentRegionId } = useRegion();

// Read id from URL query parameter
const getIdFromUrl = (): string | null => {
  const params = new URLSearchParams(window.location.search);
  return params.get('id');
};

const selectedWorld = ref<World | 'new' | null>(null);
const urlWorldId = getIdFromUrl();

// Load world from URL parameter if provided
const loadWorldFromUrl = async () => {
  if (!urlWorldId || !currentRegionId.value) return;

  try {
    const world = await worldServiceFrontend.getWorld(currentRegionId.value, urlWorldId);
    selectedWorld.value = world;
  } catch (e) {
    console.error('[WorldApp] Failed to load world from URL:', e);
  }
};

const handleWorldSelect = (world: World) => {
  selectedWorld.value = world;
};

const handleCreateNew = () => {
  selectedWorld.value = 'new';
};

const handleBack = () => {
  selectedWorld.value = null;
};

const handleSaved = () => {
  selectedWorld.value = null;
};

// Watch for region changes and load world if URL param exists
watch(currentRegionId, () => {
  if (urlWorldId && currentRegionId.value && !selectedWorld.value) {
    loadWorldFromUrl();
  }
}, { immediate: true });
</script>
