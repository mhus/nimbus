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

      <!-- Two column layout -->
      <div class="grid gap-1" style="grid-template-columns: 50% 50%;">
        <!-- Left Column: Edit Action, Navigator, Selected Block -->
        <div class="space-y-1">
          <!-- Edit Action Configuration - Compact -->
          <div class="card bg-base-100 shadow-sm">
            <div class="card-body p-2">
              <div class="flex justify-between items-center mb-2">
                <h2 class="card-title text-sm">Edit Action</h2>
                <button
                  @click="fetchEditState"
                  class="btn btn-ghost btn-xs btn-circle"
                  title="Refresh"
                >
                  <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
                  </svg>
                </button>
              </div>

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

          <!-- Navigator - Collapsible -->
          <div class="collapse collapse-arrow bg-base-100 shadow-sm">
            <input type="checkbox" />
            <div class="collapse-title text-sm font-semibold p-2">
              Navigator
            </div>
            <div class="collapse-content">
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

        <!-- Right Column: Layer Selection, Edit Mode Control -->
        <div class="space-y-1">
          <!-- Layer Selection - NEW -->
          <div class="card bg-base-100 shadow-sm">
            <div class="card-body p-2">
              <div class="flex justify-between items-center mb-2">
                <h2 class="card-title text-sm">Layer Selection</h2>
                <div class="flex gap-1">
                  <a
                    :href="getLayerEditorUrl()"
                    target="_blank"
                    class="btn btn-primary btn-xs"
                    title="Open Layer Editor"
                  >
                    <svg xmlns="http://www.w3.org/2000/svg" class="h-3 w-3" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 6H6a2 2 0 00-2 2v10a2 2 0 002 2h10a2 2 0 002-2v-4M14 4h6m0 0v6m0-6L10 14" />
                    </svg>
                    Edit Layers
                  </a>
                  <button
                    @click="fetchLayers"
                    class="btn btn-ghost btn-xs btn-circle"
                    title="Refresh layers"
                  >
                    <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
                    </svg>
                  </button>
                </div>
              </div>

              <div class="form-control w-full">
                <label class="label py-1">
                  <span class="label-text-alt text-xs">Edit Layer</span>
                </label>
                <select
                  v-model="editState.selectedLayer"
                  class="select select-bordered select-sm w-full text-xs"
                  :disabled="saving || editState.editMode"
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

          <!-- Edit Mode Control - NEW -->
          <div class="card bg-base-100 shadow-sm">
            <div class="card-body p-2">
              <h2 class="card-title text-sm mb-2">Edit Mode Control</h2>

              <!-- Status Display -->
              <div class="alert text-xs" :class="editState.editMode ? 'alert-success' : 'alert-info'">
                <div class="flex flex-col gap-1 w-full">
                  <span>
                    <strong>{{ editState.editMode ? '✓ ACTIVE' : '○ INACTIVE' }}</strong>
                  </span>
                  <span v-if="editState.editMode && editState.selectedLayer" class="text-xs opacity-80">
                    Layer: {{ editState.selectedLayer }}
                  </span>
                </div>
              </div>

              <!-- Activate Button (when inactive) -->
              <button
                v-if="!editState.editMode"
                @click="activateEditMode"
                :disabled="!editState.selectedLayer || activating"
                class="btn btn-primary btn-sm btn-block mt-2">
                <span v-if="activating" class="loading loading-spinner loading-xs"></span>
                <span v-else>Activate Edit Mode</span>
              </button>

              <!-- Action Buttons (when active) -->
              <div v-else class="flex gap-2 flex-col mt-2">
                <button @click="saveOverlays"
                        :disabled="saving"
                        class="btn btn-success btn-sm">
                  <span v-if="saving" class="loading loading-spinner loading-xs"></span>
                  <span v-else>💾 Save to Layer</span>
                </button>

                <button @click="openDiscardModal"
                        :disabled="discarding"
                        class="btn btn-error btn-sm">
                  <span v-if="discarding" class="loading loading-spinner loading-xs"></span>
                  <span v-else>🗑️ Discard All</span>
                </button>
              </div>

              <!-- Layer Lock Info -->
              <div v-if="editState.editMode" class="alert alert-warning text-xs mt-2 py-1 px-2">
                <svg class="w-4 h-4 shrink-0" fill="currentColor" viewBox="0 0 20 20">
                  <path fill-rule="evenodd" d="M5 9V7a5 5 0 0110 0v2a2 2 0 012 2v5a2 2 0 01-2 2H5a2 2 0 01-2-2v-5a2 2 0 012-2zm8-2v2H7V7a3 3 0 016 0z" clip-rule="evenodd"/>
                </svg>
                <span>Layer locked while active</span>
              </div>
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

    <!-- Discard Confirmation Modal -->
    <dialog ref="discardModal" class="modal">
      <div class="modal-box">
        <h3 class="font-bold text-lg">⚠️ Discard All Changes?</h3>
        <div class="alert alert-warning my-4">
          <svg class="w-6 h-6 shrink-0" fill="currentColor" viewBox="0 0 20 20">
            <path fill-rule="evenodd" d="M8.257 3.099c.765-1.36 2.722-1.36 3.486 0l5.58 9.92c.75 1.334-.213 2.98-1.742 2.98H4.42c-1.53 0-2.493-1.646-1.743-2.98l5.58-9.92zM11 13a1 1 0 11-2 0 1 1 0 012 0zm-1-8a1 1 0 00-1 1v3a1 1 0 002 0V6a1 1 0 00-1-1z" clip-rule="evenodd"/>
          </svg>
          <div>
            <p class="font-semibold">This will delete ALL overlay blocks!</p>
            <p class="text-sm">This action cannot be undone.</p>
          </div>
        </div>
        <div class="modal-action">
          <button @click="closeDiscardModal" class="btn btn-ghost">Cancel</button>
          <button @click="confirmDiscard" class="btn btn-error" :disabled="discarding">
            <span v-if="discarding" class="loading loading-spinner loading-xs"></span>
            <span v-else>Discard All Changes</span>
          </button>
        </div>
      </div>
      <form method="dialog" class="modal-backdrop">
        <button>close</button>
      </form>
    </dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue';
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

// Modal state
const discardModal = ref<HTMLDialogElement | null>(null);

// Edit mode control state
const activating = ref(false);
const discarding = ref(false);

// No automatic polling - use manual refresh button

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

// Helper function to check if two values are deeply equal
function isEqual(a: any, b: any): boolean {
  if (a === b) return true;
  if (a == null || b == null) return false;
  if (typeof a !== 'object' || typeof b !== 'object') return false;

  const keysA = Object.keys(a);
  const keysB = Object.keys(b);

  if (keysA.length !== keysB.length) return false;

  for (const key of keysA) {
    if (!keysB.includes(key)) return false;
    if (!isEqual(a[key], b[key])) return false;
  }

  return true;
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

    // Create new state object
    const newState = {
      editMode: data.editMode || false,
      editAction: data.editAction || 'OPEN_CONFIG_DIALOG',
      selectedLayer: data.selectedLayer || null,
      mountX: data.mountX || 0,
      mountY: data.mountY || 0,
      mountZ: data.mountZ || 0,
      selectedGroup: data.selectedGroup || 0,
    };

    // Only update if state actually changed
    if (!isEqual(editState.value, newState)) {
      editState.value = newState;

      // Update legacy refs for backward compatibility
      currentEditAction.value = editState.value.editAction;
      savedEditAction.value = editState.value.editAction;
    }

    // Update selected block only if changed
    const newSelectedBlock = data.selectedBlock || null;
    if (!isEqual(selectedBlock.value, newSelectedBlock)) {
      selectedBlock.value = newSelectedBlock;
    }

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

// Generate Layer Editor URL with preserved parameters
function getLayerEditorUrl(): string {
  const params = new URLSearchParams(window.location.search);

  // Use relative path to layer-editor.html
  const baseUrl = 'layer-editor.html';

  // Preserve worldId and sessionId parameters
  const newParams = new URLSearchParams();
  if (params.get('worldId')) newParams.set('worldId', params.get('worldId')!);
  if (params.get('sessionId')) newParams.set('sessionId', params.get('sessionId')!);

  return `${baseUrl}?${newParams.toString()}`;
}

// Manual refresh - no automatic polling

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
  // No automatic polling - user clicks refresh button manually
});

// ===== EDIT MODE CONTROL FUNCTIONS =====

async function activateEditMode() {
  if (!editState.value.selectedLayer) {
    error.value = 'Please select a layer first';
    return;
  }

  activating.value = true;
  error.value = null;

  try {
    const response = await fetch(
      `${apiUrl.value}/api/editor/${worldId.value}/session/${sessionId.value}/activate`,
      { method: 'POST' }
    );

    if (!response.ok) {
      const data = await response.json();
      throw new Error(data.error || 'Activation failed');
    }

    await fetchEditState(); // Refresh

  } catch (err) {
    error.value = err instanceof Error ? err.message : 'Unknown error';
  } finally {
    activating.value = false;
  }
}

function openDiscardModal() {
  discardModal.value?.showModal();
}

function closeDiscardModal() {
  discardModal.value?.close();
}

async function confirmDiscard() {
  discarding.value = true;

  try {
    const response = await fetch(
      `${apiUrl.value}/api/editor/${worldId.value}/session/${sessionId.value}/discard`,
      { method: 'POST' }
    );

    if (!response.ok) throw new Error('Discard failed');

    closeDiscardModal();
    await fetchEditState(); // Refresh

  } catch (err) {
    error.value = err instanceof Error ? err.message : 'Unknown error';
  } finally {
    discarding.value = false;
  }
}

async function saveOverlays() {
  saving.value = true;
  error.value = null;

  try {
    const response = await fetch(
      `${apiUrl.value}/api/editor/${worldId.value}/session/${sessionId.value}/save`,
      { method: 'POST', headers: { 'Accept': 'application/json' } }
    );

    if (!response.ok) throw new Error('Save failed');

    const data = await response.json();
    console.log('Save started:', data.message);
    // Optional: Show success notification

  } catch (err) {
    error.value = err instanceof Error ? err.message : 'Unknown error';
  } finally {
    saving.value = false;
  }
}
</script>
