<template>
  <div class="min-h-screen flex flex-col">
    <!-- Header -->
    <header class="navbar bg-base-200 shadow-lg">
      <div class="flex-1">
        <h1 class="text-xl font-bold px-4">Nimbus Backdrop Editor</h1>
      </div>
      <div class="flex-none">
        <WorldSelector />
      </div>
    </header>

    <!-- Main Content -->
    <main class="flex-1 container mx-auto px-4 py-6">
      <div v-if="!currentWorldId" class="alert alert-info">
        <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
        </svg>
        <span>Please select a world from the dropdown above</span>
      </div>

      <div v-else>
        <BackdropList
          v-if="!selectedBackdrop"
          @select="handleBackdropSelect"
          @create="handleCreateNew"
        />
        <BackdropEditor
          v-else
          :backdrop="selectedBackdrop"
          @back="handleBack"
          @saved="handleSaved"
        />
      </div>
    </main>

    <footer class="footer footer-center p-4 bg-base-300 text-base-content">
      <div>
        <p>Nimbus Backdrop Editor v1.0.0</p>
      </div>
    </footer>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue';
import { useWorld } from '@/composables/useWorld';
import WorldSelector from '@material/components/WorldSelector.vue';
import BackdropList from './views/BackdropList.vue';
import BackdropEditor from './views/BackdropEditor.vue';
import { backdropService, type BackdropData } from './services/BackdropService';

const { currentWorldId } = useWorld();

// Read id from URL query parameter
const getIdFromUrl = (): string | null => {
  const params = new URLSearchParams(window.location.search);
  return params.get('id');
};

const selectedBackdrop = ref<BackdropData | 'new' | null>(null);
const urlBackdropId = getIdFromUrl();

// Load backdrop from URL if provided
const loadBackdropFromUrl = async () => {
  if (!urlBackdropId || !currentWorldId.value) return;

  try {
    const publicData = await backdropService.getBackdrop(currentWorldId.value, urlBackdropId);
    selectedBackdrop.value = {
      backdropId: urlBackdropId,
      publicData,
      worldId: currentWorldId.value,
      enabled: true,
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
    };
  } catch (e) {
    console.error('[BackdropApp] Failed to load backdrop from URL:', e);
  }
};

const handleBackdropSelect = (backdrop: BackdropData) => {
  selectedBackdrop.value = backdrop;
};

const handleCreateNew = () => {
  selectedBackdrop.value = 'new';
};

const handleBack = () => {
  selectedBackdrop.value = null;
};

const handleSaved = () => {
  selectedBackdrop.value = null;
};

// Watch for world changes and load backdrop if URL param exists
watch(currentWorldId, () => {
  if (urlBackdropId && currentWorldId.value && !selectedBackdrop.value) {
    loadBackdropFromUrl();
  }
}, { immediate: true });
</script>
