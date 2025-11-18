<template>
  <div class="min-h-screen flex flex-col bg-base-100">
    <!-- Header -->
    <header class="navbar bg-base-300 shadow-lg">
      <div class="flex-1">
        <h1 class="text-xl font-bold ml-4">Scrawl Script Editor</h1>
      </div>
      <div class="flex-none gap-2 mr-4">
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

      <!-- Script Editor (Right Panel) -->
      <div v-else class="flex-1 flex flex-col">
        <ScriptEditorView
          :script="selectedScript"
          :is-new="isNewScript"
          @save="saveScript"
          @close="closeEditor"
          @delete="deleteScript"
        />
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import type { ScrawlScript } from '@nimbus/shared';
import ScriptListView from './views/ScriptListView.vue';
import ScriptEditorView from './views/ScriptEditorView.vue';

const selectedScript = ref<ScrawlScript | null>(null);
const isNewScript = ref(false);

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

function saveScript(script: ScrawlScript) {
  // TODO: Save via API
  console.log('Save script:', script);
  selectedScript.value = null;
}

function closeEditor() {
  selectedScript.value = null;
  isNewScript.value = false;
}

function deleteScript(scriptId: string) {
  // TODO: Delete via API
  console.log('Delete script:', scriptId);
  selectedScript.value = null;
}
</script>
