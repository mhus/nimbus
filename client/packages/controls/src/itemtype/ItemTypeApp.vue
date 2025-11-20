<template>
  <div class="min-h-screen flex flex-col bg-base-100">
    <!-- Header -->
    <header class="navbar bg-base-300 shadow-lg">
      <div class="flex-1">
        <h1 class="text-xl font-bold ml-4">ItemType Editor</h1>
      </div>
      <div class="flex-none gap-2 mr-4">
        <button
          v-if="!selectedItemTypeId"
          class="btn btn-sm btn-primary"
          @click="createNewItemType"
        >
          <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
          </svg>
          Add
        </button>
        <button
          v-if="selectedItemTypeId"
          class="btn btn-sm btn-ghost"
          @click="closeEditor"
        >
          <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
          </svg>
          Close
        </button>
      </div>
    </header>

    <!-- Main Content -->
    <main class="flex-1 flex overflow-hidden">
      <!-- ItemType List (Left Panel) -->
      <div v-if="!selectedItemTypeId" class="flex-1 p-6 overflow-auto">
        <ItemTypeListView
          @select="openItemType"
        />
      </div>

      <!-- ItemType Editor (Right Panel) -->
      <div v-else class="flex-1 overflow-auto">
        <ItemTypeEditorView
          :item-type-id="selectedItemTypeId"
          :is-new="isNewItemType"
          @save="saveItemType"
          @close="closeEditor"
          @delete="deleteCurrentItemType"
        />
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import ItemTypeListView from './views/ItemTypeListView.vue';
import ItemTypeEditorView from './views/ItemTypeEditorView.vue';

const selectedItemTypeId = ref<string | null>(null);
const isNewItemType = ref(false);

function createNewItemType() {
  selectedItemTypeId.value = 'new_itemtype';
  isNewItemType.value = true;
}

function openItemType(itemTypeId: string) {
  selectedItemTypeId.value = itemTypeId;
  isNewItemType.value = false;
}

function saveItemType() {
  // Refresh list after save
  selectedItemTypeId.value = null;
  isNewItemType.value = false;
}

function closeEditor() {
  selectedItemTypeId.value = null;
  isNewItemType.value = false;
}

function deleteCurrentItemType() {
  selectedItemTypeId.value = null;
  isNewItemType.value = false;
}
</script>
