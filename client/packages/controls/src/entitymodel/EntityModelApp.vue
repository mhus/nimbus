<template>
  <div class="min-h-screen flex flex-col">
    <!-- Header -->
    <header class="navbar bg-base-200 shadow-lg">
      <div class="flex-1">
        <h1 class="text-xl font-bold px-4">Nimbus Entity Model Editor</h1>
      </div>
      <div class="flex-none">
        <!-- World Selector -->
        <WorldSelector />
      </div>
    </header>

    <!-- Main Content -->
    <main class="flex-1 container mx-auto px-4 py-6">
      <!-- Show message if no world selected -->
      <div v-if="!currentWorldId" class="alert alert-info">
        <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
        </svg>
        <span>Please select a world from the dropdown above</span>
      </div>

      <!-- EntityModel List or Editor -->
      <div v-else>
        <EntityModelList
          v-if="!selectedEntityModel"
          @select="handleEntityModelSelect"
          @create="handleCreateNew"
        />
        <EntityModelEditor
          v-else
          :entityModel="selectedEntityModel"
          @back="handleBack"
          @saved="handleSaved"
        />
      </div>
    </main>

    <!-- Footer -->
    <footer class="footer footer-center p-4 bg-base-300 text-base-content">
      <div>
        <p>Nimbus Entity Model Editor v1.0.0</p>
      </div>
    </footer>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue';
import { useWorld } from '@/composables/useWorld';
import WorldSelector from '@material/components/WorldSelector.vue';
import EntityModelList from './views/EntityModelList.vue';
import EntityModelEditor from './views/EntityModelEditor.vue';
import { entityModelService, type EntityModelData } from './services/EntityModelService';

const { currentWorldId } = useWorld();

// Read id from URL query parameter
const getIdFromUrl = (): string | null => {
  const params = new URLSearchParams(window.location.search);
  return params.get('id');
};

const selectedEntityModel = ref<EntityModelData | 'new' | null>(null);
const urlModelId = getIdFromUrl();

// Load entity model from URL if provided
const loadEntityModelFromUrl = async () => {
  if (!urlModelId || !currentWorldId.value) return;

  try {
    const publicData = await entityModelService.getEntityModel(currentWorldId.value, urlModelId);
    selectedEntityModel.value = {
      modelId: urlModelId,
      publicData,
      worldId: currentWorldId.value,
      enabled: true,
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
    };
  } catch (e) {
    console.error('[EntityModelApp] Failed to load entity model from URL:', e);
  }
};

const handleEntityModelSelect = (entityModel: EntityModelData) => {
  selectedEntityModel.value = entityModel;
};

const handleCreateNew = () => {
  selectedEntityModel.value = 'new';
};

const handleBack = () => {
  selectedEntityModel.value = null;
};

const handleSaved = () => {
  selectedEntityModel.value = null;
};

// Watch for world changes and load entity model if URL param exists
watch(currentWorldId, () => {
  if (urlModelId && currentWorldId.value && !selectedEntityModel.value) {
    loadEntityModelFromUrl();
  }
}, { immediate: true });
</script>
