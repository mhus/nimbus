<template>
  <div class="min-h-screen flex flex-col bg-base-200">
    <!-- Header (hidden in embedded mode) -->
    <header v-if="!isEmbedded()" class="navbar bg-base-300 shadow-lg">
      <div class="flex-1">
        <h1 class="text-xl font-bold px-4">Edit Configuration</h1>
      </div>
      <div class="flex-none">
        <span class="text-sm text-base-content/70">Session: {{ sessionId }}</span>
      </div>
    </header>

    <!-- Main Content - Compact for iframe -->
    <main class="flex-1 px-1 py-2">
      <!-- Error Display -->
      <div v-if="error" class="alert alert-error alert-sm mb-2 text-xs">
        <svg xmlns="http://www.w3.org/2000/svg" class="stroke-current shrink-0 h-4 w-4" fill="none" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 14l2-2m0 0l2-2m-2 2l-2-2m2 2l2 2m7-2a9 9 0 11-18 0 9 9 0 0118 0z" />
        </svg>
        <span>{{ error }}</span>
      </div>

      <!-- Two-column layout: Form left (70%), Navigator right (30%) -->
      <div class="grid gap-1" style="grid-template-columns: 70% 30%;">
        <!-- Left Column: Form and Info -->
        <div class="space-y-1">
          <!-- Edit Action Configuration - Compact -->
          <div class="card bg-base-100 shadow-sm">
            <div class="card-body p-2">
              <h2 class="card-title text-sm mb-2">Edit Action</h2>

              <div class="form-control w-full">
                <select
                  v-model="currentEditAction"
                  class="select select-bordered select-sm w-full text-xs"
                  :disabled="saving"
                >
                  <option v-for="action in editActions" :key="action" :value="action">
                    {{ formatActionName(action) }}
                  </option>
                </select>
                <label class="label py-1">
                  <span class="label-text-alt text-xs">
                    {{ getActionDescription(currentEditAction) }}
                    <span v-if="saving" class="loading loading-spinner loading-xs ml-2"></span>
                  </span>
                </label>
              </div>
            </div>
          </div>

          <!-- Layer Selection - NEW -->
          <div class="card bg-base-100 shadow-sm">
            <div class="card-body p-2">
              <h2 class="card-title text-sm mb-2">Layer Selection</h2>

              <div class="form-control w-full">
                <label class="label py-1">
                  <span class="label-text-alt text-xs">Edit Layer</span>
                </label>
                <select
                  v-model="editState.selectedLayer"
                  class="select select-bordered select-sm w-full text-xs"
                  :disabled="saving"
                >
                  <option :value="null">All Layers (Legacy)</option>
                  <option v-for="layer in availableLayers" :key="layer.name" :value="layer.name">
                    {{ layer.name }} ({{ layer.layerType }}) - Order: {{ layer.order }}
                  </option>
                </select>
              </div>

              <!-- Mount Point for MODEL layers -->
              <div v-if="selectedLayerInfo?.layerType === 'MODEL'" class="mt-2">
                <label class="label py-1">
                  <span class="label-text-alt text-xs">Mount Point</span>
                </label>
                <div class="grid grid-cols-3 gap-1">
                  <input
                    v-model.number="editState.mountX"
                    type="number"
                    placeholder="X"
                    class="input input-bordered input-sm text-xs"
                  />
                  <input
                    v-model.number="editState.mountY"
                    type="number"
                    placeholder="Y"
                    class="input input-bordered input-sm text-xs"
                  />
                  <input
                    v-model.number="editState.mountZ"
                    type="number"
                    placeholder="Z"
                    class="input input-bordered input-sm text-xs"
                  />
                </div>
              </div>

              <!-- Group Selection -->
              <div class="form-control w-full mt-2">
                <label class="label py-1">
                  <span class="label-text-alt text-xs">Group (0 = all)</span>
                </label>
                <input
                  v-model.number="editState.selectedGroup"
                  type="number"
                  min="0"
                  class="input input-bordered input-sm w-full text-xs"
                />
              </div>
            </div>
          </div>

          <!-- Selected Block Display - Compact -->
          <div class="card bg-base-100 shadow-sm">
            <div class="card-body p-2">
              <h2 class="card-title text-sm mb-2">Selected Block</h2>

              <div v-if="selectedBlock" class="grid grid-cols-3 gap-2 text-center">
                <div class="bg-primary/10 rounded p-2">
                  <div class="text-xs text-base-content/70">X</div>
                  <div class="text-lg font-bold text-primary">{{ selectedBlock.x }}</div>
                </div>
                <div class="bg-secondary/10 rounded p-2">
                  <div class="text-xs text-base-content/70">Y</div>
                  <div class="text-lg font-bold text-secondary">{{ selectedBlock.y }}</div>
                </div>
                <div class="bg-accent/10 rounded p-2">
                  <div class="text-xs text-base-content/70">Z</div>
                  <div class="text-lg font-bold text-accent">{{ selectedBlock.z }}</div>
                </div>
              </div>

              <div v-else class="alert alert-info alert-sm text-xs">
                <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" class="stroke-current shrink-0 w-4 h-4">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path>
                </svg>
                <span>No block selected</span>
              </div>

              <!-- Marked Block Display - Compact -->
              <div v-if="markedBlock" class="mt-2">
                <h3 class="text-xs font-semibold mb-1 text-base-content/70">Marked (copy/move)</h3>
                <div class="grid grid-cols-3 gap-2 text-center">
                  <div class="bg-warning/10 rounded p-1">
                    <div class="text-xs text-base-content/70">X</div>
                    <div class="text-sm font-bold">{{ markedBlock.x }}</div>
                  </div>
                  <div class="bg-warning/10 rounded p-1">
                    <div class="text-xs text-base-content/70">Y</div>
                    <div class="text-sm font-bold">{{ markedBlock.y }}</div>
                  </div>
                  <div class="bg-warning/10 rounded p-1">
                    <div class="text-xs text-base-content/70">Z</div>
                    <div class="text-sm font-bold">{{ markedBlock.z }}</div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Right Column: Navigate Component -->
        <div class="flex items-start justify-center">
          <div class="card bg-base-100 shadow-sm w-full">
            <div class="card-body p-2">
              <NavigateSelectedBlockComponent
                :selected-block="selectedBlock"
                :step="1"
                :size="224"
                :show-execute-button="true"
                @navigate="handleNavigate"
                @execute="executeAction"
              />
            </div>
          </div>
        </div>
      </div>
    </main>

    <!-- Footer (hidden in embedded mode) -->
    <footer v-if="!isEmbedded()" class="footer footer-center p-4 bg-base-300 text-base-content">
      <div>
        <p>Nimbus Edit Configuration v2.0.0</p>
      </div>
    </footer>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted } from 'vue';
import { useModal } from '@/composables/useModal';
import NavigateSelectedBlockComponent from '@/components/NavigateSelectedBlockComponent.vue';

// Edit actions enum
const editActions = [
  'OPEN_CONFIG_DIALOG',
  'OPEN_EDITOR',
  'MARK_BLOCK',
  'COPY_BLOCK',
  'DELETE_BLOCK',
  'MOVE_BLOCK',
] as const;

type EditAction = typeof editActions[number];

// Modal composable for embedded detection
const { isEmbedded } = useModal();

// Get URL parameters
const params = new URLSearchParams(window.location.search);
const worldId = ref(params.get('worldId') || import.meta.env.VITE_WORLD_ID || 'main');
const sessionId = ref(params.get('sessionId') || '');
const apiUrl = ref(import.meta.env.VITE_CONTROL_API_URL || 'http://localhost:9043'); // world-control

// Edit State (unified)
const editState = ref({
  editMode: false,
  editAction: 'OPEN_CONFIG_DIALOG' as EditAction,
  selectedLayer: null as string | null,
  mountX: 0,
  mountY: 0,
  mountZ: 0,
  selectedGroup: 0,
});

// Available layers
const availableLayers = ref<Array<{
  name: string;
  layerType: string;
  enabled: boolean;
  order: number;
  mountX: number;
  mountY: number;
  mountZ: number;
  groups: string[];
}>>([]);

// Legacy state refs
const currentEditAction = ref<EditAction>('OPEN_CONFIG_DIALOG');
const savedEditAction = ref<EditAction>('OPEN_CONFIG_DIALOG');
const selectedBlock = ref<{ x: number; y: number; z: number } | null>(null);
const markedBlock = ref<{ x: number; y: number; z: number } | null>(null);
const error = ref<string | null>(null);
const saving = ref(false);

// Polling interval
let pollInterval: number | null = null;

// Format action name for display
function formatActionName(action: EditAction): string {
  return action.replace(/_/g, ' ').toLowerCase()
    .split(' ')
    .map(word => word.charAt(0).toUpperCase() + word.slice(1))
    .join(' ');
}

// Get action description
function getActionDescription(action: EditAction): string {
  switch (action) {
    case 'OPEN_CONFIG_DIALOG':
      return 'Opens config dialog on block select';
    case 'OPEN_EDITOR':
      return 'Opens block editor on select';
    case 'MARK_BLOCK':
      return 'Marks block for copy/move';
    case 'COPY_BLOCK':
      return 'Copies marked block to position';
    case 'DELETE_BLOCK':
      return 'Deletes block at position';
    case 'MOVE_BLOCK':
      return 'Moves marked block to position';
    default:
      return '';
  }
}

// Fetch available layers from API
async function fetchLayers() {
  try {
    const response = await fetch(`${apiUrl.value}/api/editor/${worldId.value}/layers`);

    if (!response.ok) {
      throw new Error(`Failed to fetch layers: ${response.statusText}`);
    }

    const data = await response.json();
    availableLayers.value = data.layers || [];
  } catch (err) {
    console.error('Failed to fetch layers:', err);
    // Don't set error here, not critical for edit mode
  }
}

// Fetch edit state from API (NEW unified endpoint)
async function fetchEditState() {
  try {
    const response = await fetch(
      `${apiUrl.value}/api/editor/${worldId.value}/session/${sessionId.value}/edit`
    );

    if (!response.ok) {
      throw new Error(`Failed to fetch edit state: ${response.statusText}`);
    }

    const data = await response.json();

    // Update edit state
    editState.value = {
      editMode: data.editMode || false,
      editAction: data.editAction || 'OPEN_CONFIG_DIALOG',
      selectedLayer: data.selectedLayer || null,
      mountX: data.mountX || 0,
      mountY: data.mountY || 0,
      mountZ: data.mountZ || 0,
      selectedGroup: data.selectedGroup || 0,
    };

    // Update legacy refs for backward compatibility
    currentEditAction.value = editState.value.editAction;
    savedEditAction.value = editState.value.editAction;

    // Update selected block
    selectedBlock.value = data.selectedBlock || null;

    error.value = null;
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'Unknown error';
    console.error('Failed to fetch edit state:', err);
  }
}

// Save edit state to API (NEW unified endpoint)
async function saveEditState() {
  saving.value = true;
  error.value = null;

  try {
    const response = await fetch(
      `${apiUrl.value}/api/editor/${worldId.value}/session/${sessionId.value}/edit`,
      {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(editState.value),
      }
    );

    if (!response.ok) {
      throw new Error(`Failed to save edit state: ${response.statusText}`);
    }

    const data = await response.json();
    // Update saved state
    savedEditAction.value = data.editAction as EditAction;
    error.value = null;
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'Unknown error';
    console.error('Failed to save edit state:', err);
  } finally {
    saving.value = false;
  }
}

// Computed: Get selected layer info
const selectedLayerInfo = computed(() => {
  if (!editState.value.selectedLayer) return null;
  return availableLayers.value.find(l => l.name === editState.value.selectedLayer);
});

// Start polling for edit state updates
function startPolling() {
  // Initial fetch
  fetchEditState();

  // Poll every 2 seconds
  pollInterval = window.setInterval(() => {
    fetchEditState();
  }, 2000);
}

// Stop polling
function stopPolling() {
  if (pollInterval !== null) {
    clearInterval(pollInterval);
    pollInterval = null;
  }
}

// Handle navigation from NavigateSelectedBlockComponent (selection only, no action)
async function handleNavigate(position: { x: number; y: number; z: number }) {
  try {
    const response = await fetch(
      `${apiUrl.value}/api/worlds/${worldId.value}/session/${sessionId.value}/selectedEditBlock/navigate`,
      {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(position),
      }
    );

    if (!response.ok) {
      throw new Error(`Failed to navigate to block: ${response.statusText}`);
    }

    // Update local state immediately for responsive UI
    selectedBlock.value = position;
    error.value = null;
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'Unknown error';
    console.error('Failed to navigate to block:', err);
  }
}

// Execute action on selected block (PUT triggers editAction)
async function executeAction() {
  if (!selectedBlock.value) {
    error.value = 'No block selected';
    return;
  }

  try {
    const response = await fetch(
      `${apiUrl.value}/api/worlds/${worldId.value}/session/${sessionId.value}/selectedEditBlock`,
      {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(selectedBlock.value),
      }
    );

    if (!response.ok) {
      throw new Error(`Failed to execute action: ${response.statusText}`);
    }

    error.value = null;
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'Unknown error';
    console.error('Failed to execute action:', err);
  }
}

// Watch for edit state changes and auto-save (with debounce)
let saveTimeout: number | null = null;
watch(editState, () => {
  if (saveTimeout) clearTimeout(saveTimeout);
  saveTimeout = window.setTimeout(async () => {
    await saveEditState();
  }, 500);
}, { deep: true });

// Watch currentEditAction for backward compatibility
watch(currentEditAction, (newAction) => {
  editState.value.editAction = newAction;
});

// Lifecycle hooks
onMounted(async () => {
  if (!sessionId.value) {
    error.value = 'No session ID provided in URL';
    return;
  }

  await fetchLayers();
  await fetchEditState();
  startPolling();
});

onUnmounted(() => {
  stopPolling();
});
</script>
