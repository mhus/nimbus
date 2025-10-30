<template>
  <div class="space-y-4">
    <!-- Header with Search and Actions -->
    <div class="flex flex-col sm:flex-row gap-4 items-stretch sm:items-center justify-between">
      <div class="flex-1">
        <SearchInput
          v-model="searchQuery"
          placeholder="Search block types..."
          @update:modelValue="handleSearch"
        />
      </div>
      <div class="flex gap-2">
        <button
          class="btn btn-primary"
          @click="openCreateDialog"
        >
          <svg class="w-5 h-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
          </svg>
          New Block Type
        </button>
      </div>
    </div>

    <!-- Loading State -->
    <LoadingSpinner v-if="loading && blockTypes.length === 0" />

    <!-- Error State -->
    <ErrorAlert v-else-if="error" :message="error" />

    <!-- Empty State -->
    <div v-else-if="!loading && blockTypes.length === 0" class="text-center py-12">
      <p class="text-base-content/70 text-lg">No block types found</p>
      <p class="text-base-content/50 text-sm mt-2">Create your first block type to get started</p>
    </div>

    <!-- Block Type List -->
    <BlockTypeList
      v-else
      :block-types="blockTypes"
      :loading="loading"
      @edit="openEditDialog"
      @delete="handleDelete"
    />

    <!-- Editor Dialog -->
    <BlockTypeEditorPanel
      v-if="isEditorOpen"
      :block-type="selectedBlockType"
      :world-id="currentWorldId!"
      @close="closeEditor"
      @saved="handleSaved"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue';
import type { BlockType } from '@nimbus/shared';
import { useWorld } from '../composables/useWorld';
import { useBlockTypes } from '../composables/useBlockTypes';
import SearchInput from '../components/SearchInput.vue';
import LoadingSpinner from '../components/LoadingSpinner.vue';
import ErrorAlert from '../components/ErrorAlert.vue';
import BlockTypeList from '../components/BlockTypeList.vue';
import BlockTypeEditorPanel from '../components/BlockTypeEditorPanel.vue';

const { currentWorldId } = useWorld();

const blockTypesComposable = computed(() => {
  if (!currentWorldId.value) return null;
  return useBlockTypes(currentWorldId.value);
});

const blockTypes = computed(() => blockTypesComposable.value?.blockTypes.value || []);
const loading = computed(() => blockTypesComposable.value?.loading.value || false);
const error = computed(() => blockTypesComposable.value?.error.value || null);
const searchQuery = ref('');

const isEditorOpen = ref(false);
const selectedBlockType = ref<BlockType | null>(null);

// Load block types when world changes
watch(currentWorldId, () => {
  if (currentWorldId.value) {
    blockTypesComposable.value?.loadBlockTypes();
  }
}, { immediate: true });

/**
 * Handle search
 */
const handleSearch = (query: string) => {
  if (!blockTypesComposable.value) return;
  blockTypesComposable.value.searchBlockTypes(query);
};

/**
 * Open create dialog
 */
const openCreateDialog = () => {
  selectedBlockType.value = null;
  isEditorOpen.value = true;
};

/**
 * Open edit dialog
 */
const openEditDialog = (blockType: BlockType) => {
  selectedBlockType.value = blockType;
  isEditorOpen.value = true;
};

/**
 * Close editor
 */
const closeEditor = () => {
  isEditorOpen.value = false;
  selectedBlockType.value = null;
};

/**
 * Handle saved
 */
const handleSaved = () => {
  closeEditor();
};

/**
 * Handle delete
 */
const handleDelete = async (blockType: BlockType) => {
  if (!blockTypesComposable.value) return;

  if (!confirm(`Are you sure you want to delete block type "${blockType.description || blockType.id}"?`)) {
    return;
  }

  await blockTypesComposable.value.deleteBlockType(blockType.id);
};

onMounted(() => {
  if (currentWorldId.value && blockTypesComposable.value) {
    blockTypesComposable.value.loadBlockTypes();
  }
});
</script>
