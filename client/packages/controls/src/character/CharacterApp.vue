<template>
  <div class="min-h-screen flex flex-col">
    <!-- Header -->
    <header class="navbar bg-base-200 shadow-lg">
      <div class="flex-1">
        <h1 class="text-xl font-bold px-4">Nimbus Character Editor</h1>
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

      <!-- Character List or Editor -->
      <div v-else>
        <CharacterList
          v-if="!selectedCharacterId"
          @select="handleCharacterSelect"
          @create="handleCreateNew"
        />
        <CharacterEditor
          v-else
          :character-id="selectedCharacterId"
          @back="handleBack"
          @saved="handleSaved"
        />
      </div>
    </main>

    <!-- Footer -->
    <footer class="footer footer-center p-4 bg-base-300 text-base-content">
      <div>
        <p>Nimbus Character Editor v1.0.0</p>
      </div>
    </footer>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { useRegion } from '@/composables/useRegion';
import RegionSelector from './components/RegionSelector.vue';
import CharacterList from './views/CharacterList.vue';
import CharacterEditor from './views/CharacterEditor.vue';

const { currentRegionId } = useRegion();

const selectedCharacterId = ref<string | null>(null);

const handleCharacterSelect = (id: string) => {
  selectedCharacterId.value = id;
};

const handleCreateNew = () => {
  selectedCharacterId.value = 'new';
};

const handleBack = () => {
  selectedCharacterId.value = null;
};

const handleSaved = () => {
  selectedCharacterId.value = null;
};
</script>
