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
          v-if="!selectedCharacter"
          @select="handleCharacterSelect"
          @create="handleCreateNew"
        />
        <CharacterEditor
          v-else
          :character="selectedCharacter"
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
import { ref, watch } from 'vue';
import { useRegion } from '@/composables/useRegion';
import RegionSelector from './components/RegionSelector.vue';
import CharacterList from './views/CharacterList.vue';
import CharacterEditor from './views/CharacterEditor.vue';
import { characterService, type Character } from './services/CharacterService';

const { currentRegionId } = useRegion();

// Read id and userId from URL
const getUrlParams = () => {
  const params = new URLSearchParams(window.location.search);
  return {
    id: params.get('id'),
    userId: params.get('userId'),
  };
};

const selectedCharacter = ref<Character | 'new' | null>(null);
const urlParams = getUrlParams();

// Load character from URL if provided
const loadCharacterFromUrl = async () => {
  if (!urlParams.id || !urlParams.userId || !currentRegionId.value) return;

  try {
    const character = await characterService.getCharacter(
      currentRegionId.value,
      urlParams.id,
      urlParams.userId,
      urlParams.id // name = id for now
    );
    selectedCharacter.value = character;
  } catch (e) {
    console.error('[CharacterApp] Failed to load character from URL:', e);
  }
};

const handleCharacterSelect = (character: Character) => {
  selectedCharacter.value = character;
};

const handleCreateNew = () => {
  selectedCharacter.value = 'new';
};

const handleBack = () => {
  selectedCharacter.value = null;
};

const handleSaved = () => {
  selectedCharacter.value = null;
};

// Watch for region changes and load character if URL params exist
watch(currentRegionId, () => {
  if (urlParams.id && currentRegionId.value && !selectedCharacter.value) {
    loadCharacterFromUrl();
  }
}, { immediate: true });
</script>
