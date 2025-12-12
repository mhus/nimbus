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
import { ref } from 'vue';
import { useWorld } from '@/composables/useWorld';
import WorldSelector from '@material/components/WorldSelector.vue';
import EntityModelList from './views/EntityModelList.vue';
import EntityModelEditor from './views/EntityModelEditor.vue';
import type { EntityModelData } from './services/EntityModelService';

const { currentWorldId } = useWorld();

const selectedEntityModel = ref<EntityModelData | 'new' | null>(null);

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
</script>
