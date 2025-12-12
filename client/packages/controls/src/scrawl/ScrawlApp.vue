<template>
  <div class="min-h-screen flex flex-col bg-base-100">
    <!-- Header -->
    <header class="navbar bg-base-300 shadow-lg">
      <div class="flex-1">
        <h1 class="text-xl font-bold ml-4">Scrawl Script Editor</h1>
      </div>
      <div class="flex-none gap-2 mr-4">
        <!-- World Selector -->
        <WorldSelector />
        <button class="btn btn-sm btn-primary" @click="createNewScript">
          <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
          </svg>
          New Script
        </button>
      </div>
    </header>

    <!-- Main Content -->
    <main class="flex-1 flex">
      <!-- Script List (Left Panel) -->
      <div v-if="!selectedScript" class="flex-1 p-6">
        <ScriptListView
          @select="openScript"
          @duplicate="duplicateScript"
        />
      </div>

      <!-- Script Editor (Full Screen) -->
      <div v-else class="flex-1 flex flex-col p-6">
        <ScrawlAppEmbedded
          :initial-script="selectedScript"
          @save="saveScript"
          @cancel="closeEditor"
        />
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import type { ScrawlScript } from '@nimbus/shared';
import ScriptListView from './views/ScriptListView.vue';
import ScrawlAppEmbedded from './ScrawlAppEmbedded.vue';
import WorldSelector from '@material/components/WorldSelector.vue';
import { ApiService } from '../services/ApiService';
import { useWorld } from '@/composables/useWorld';

const apiService = new ApiService();
const { currentWorldId } = useWorld();
const selectedScript = ref<ScrawlScript | null>(null);
const isNewScript = ref(false);
const saving = ref(false);
const error = ref<string | null>(null);

function createNewScript() {
  selectedScript.value = {
    id: '',
    root: {
      kind: 'Sequence',
      steps: [],
    },
  };
  isNewScript.value = true;
}

function openScript(script: ScrawlScript) {
  selectedScript.value = { ...script };
  isNewScript.value = false;
}

function duplicateScript(script: ScrawlScript) {
  selectedScript.value = {
    ...script,
    id: `${script.id}_copy`,
  };
  isNewScript.value = true;
}

async function saveScript(script: ScrawlScript) {
  if (!script.id) {
    alert('Script ID is required');
    return;
  }

  if (!currentWorldId.value) {
    alert('No world selected');
    return;
  }

  saving.value = true;
  error.value = null;

  try {
    // Remove .scrawl.json if already present, then add it
    let scriptId = script.id.replace(/\.scrawl\.json$/i, '');
    const filename = `${scriptId}.scrawl.json`;
    const assetPath = `scrawl/${filename}`;
    const scriptJson = JSON.stringify(script, null, 2);
    const blob = new Blob([scriptJson], { type: 'application/json' });

    // Save as asset using PUT (creates if not exists, updates if exists)
    await apiService.updateBinary(`/api/worlds/${currentWorldId.value}/assets/${assetPath}`, blob, 'application/json');

    console.log('Script saved:', scriptId);
    selectedScript.value = null;
    isNewScript.value = false;

    // Reload script list (trigger refresh in ScriptListView)
    window.location.reload();
  } catch (e: any) {
    error.value = e.message || 'Failed to save script';
    console.error('Failed to save script:', e);
    alert('Failed to save script: ' + error.value);
  } finally {
    saving.value = false;
  }
}

function closeEditor() {
  selectedScript.value = null;
  isNewScript.value = false;
}

async function deleteScript(scriptId: string) {
  if (!confirm(`Delete script "${scriptId}"?`)) {
    return;
  }

  if (!currentWorldId.value) {
    return;
  }

  try {
    const assetPath = `scrawl/${scriptId}.scrawl.json`;

    await apiService.delete(`/api/worlds/${currentWorldId.value}/assets/${assetPath}`);

    console.log('Script deleted:', scriptId);
    selectedScript.value = null;

    // Reload script list
    window.location.reload();
  } catch (e: any) {
    error.value = e.message || 'Failed to delete script';
    console.error('Failed to delete script:', e);
    alert('Failed to delete script: ' + error.value);
  }
}
</script>
