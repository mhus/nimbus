<template>
  <div class="space-y-4">
    <!-- Header with Search and Actions -->
    <div class="flex flex-col sm:flex-row gap-4 items-stretch sm:items-center justify-between">
      <div class="flex-1">
        <SearchInput
          v-model="searchQuery"
          placeholder="Search hex grids (by name or position)..."
          @search="handleSearch"
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
          New Hex Grid
        </button>
      </div>
    </div>

    <!-- Loading State -->
    <LoadingSpinner v-if="loading && hexGrids.length === 0" />

    <!-- Error State -->
    <ErrorAlert v-else-if="error" :message="error" />

    <!-- Empty State -->
    <div v-else-if="!loading && hexGrids.length === 0" class="text-center py-12">
      <p class="text-base-content/70 text-lg">No hex grids found</p>
      <p class="text-base-content/50 text-sm mt-2">Create your first hex grid to get started</p>
    </div>

    <!-- Hex Grid List -->
    <HexGridList
      v-else
      :hex-grids="filteredHexGrids"
      :loading="loading"
      @edit="openEditDialog"
      @delete="handleDelete"
      @toggle-enabled="handleToggleEnabled"
    />

    <!-- Editor Dialog -->
    <HexGridEditorPanel
      v-if="isEditorOpen"
      :hex-grid="selectedHexGrid"
      :world-id="currentWorldId!"
      @close="closeEditor"
      @saved="handleSaved"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue';
import { useWorld } from '@/composables/useWorld';
import { useHexGrids, type HexGridWithId } from '@/composables/useHexGrids';
import SearchInput from '@components/SearchInput.vue';
import LoadingSpinner from '@components/LoadingSpinner.vue';
import ErrorAlert from '@components/ErrorAlert.vue';
import HexGridList from '@material/components/HexGridList.vue';
import HexGridEditorPanel from '@material/components/HexGridEditorPanel.vue';

const { currentWorldId } = useWorld();

const hexGridsComposable = computed(() => {
  if (!currentWorldId.value) return null;
  return useHexGrids(currentWorldId.value);
});

const hexGrids = computed(() => hexGridsComposable.value?.hexGrids.value || []);
const loading = computed(() => hexGridsComposable.value?.loading.value || false);
const error = computed(() => hexGridsComposable.value?.error.value || null);
const searchQuery = ref('');

const isEditorOpen = ref(false);
const selectedHexGrid = ref<HexGridWithId | null>(null);

// Load hex grids when world changes
watch(currentWorldId, () => {
  if (currentWorldId.value) {
    hexGridsComposable.value?.loadHexGrids();
  }
}, { immediate: true });

/**
 * Filter hex grids based on search query
 */
const filteredHexGrids = computed(() => {
  if (!searchQuery.value) {
    return hexGrids.value;
  }

  const query = searchQuery.value.toLowerCase();
  return hexGrids.value.filter(grid => {
    const position = `${grid.position.q}:${grid.position.r}`;
    return (
      grid.name?.toLowerCase().includes(query) ||
      grid.description?.toLowerCase().includes(query) ||
      position.includes(query)
    );
  });
});

/**
 * Handle search
 */
const handleSearch = (query: string) => {
  searchQuery.value = query;
};

/**
 * Open create dialog
 */
const openCreateDialog = () => {
  selectedHexGrid.value = null;
  isEditorOpen.value = true;
};

/**
 * Open edit dialog
 */
const openEditDialog = (hexGrid: HexGridWithId) => {
  selectedHexGrid.value = hexGrid;
  isEditorOpen.value = true;
};

/**
 * Close editor
 */
const closeEditor = () => {
  isEditorOpen.value = false;
  selectedHexGrid.value = null;
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
const handleDelete = async (hexGrid: HexGridWithId) => {
  if (!hexGridsComposable.value) return;

  const position = `${hexGrid.position.q}:${hexGrid.position.r}`;
  if (!confirm(`Are you sure you want to delete hex grid "${hexGrid.name || position}"?`)) {
    return;
  }

  await hexGridsComposable.value.deleteHexGrid(hexGrid.position.q, hexGrid.position.r);
};

/**
 * Handle toggle enabled
 */
const handleToggleEnabled = async (hexGrid: HexGridWithId) => {
  if (!hexGridsComposable.value) return;

  if (hexGrid.enabled) {
    await hexGridsComposable.value.disableHexGrid(hexGrid.position.q, hexGrid.position.r);
  } else {
    await hexGridsComposable.value.enableHexGrid(hexGrid.position.q, hexGrid.position.r);
  }
};
</script>
