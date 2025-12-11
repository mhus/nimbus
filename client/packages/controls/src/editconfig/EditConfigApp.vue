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
                  @click="refreshAll"
                  class="btn btn-ghost btn-xs btn-circle"
                  title="Refresh all (layers, state, palette)"
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

              <!-- Marked Block Display - Content -->
              <div v-if="markedBlockContent" class="mt-2">
                <h3 class="text-xs font-semibold mb-1 text-base-content/70">Marked Block</h3>
                <div class="bg-warning/10 rounded p-2">
                  <div class="flex items-center gap-2">
                    <!-- Block Icon/Texture -->
                    <div class="w-8 h-8 bg-base-300 rounded flex items-center justify-center flex-shrink-0">
                      <img
                        v-if="markedBlockIcon"
                        :src="getTextureUrl(markedBlockIcon)"
                        :alt="markedBlockContent.name"
                        class="w-full h-full object-contain"
                        @error="markedBlockIcon = null"
                      />
                      <svg v-else class="w-4 h-4 text-base-content/30" fill="currentColor" viewBox="0 0 20 20">
                        <path d="M3 4a1 1 0 011-1h12a1 1 0 011 1v2a1 1 0 01-1 1H4a1 1 0 01-1-1V4zM3 10a1 1 0 011-1h6a1 1 0 011 1v6a1 1 0 01-1 1H4a1 1 0 01-1-1v-6zM14 9a1 1 0 00-1 1v6a1 1 0 001 1h2a1 1 0 001-1v-6a1 1 0 00-1-1h-2z"/>
                      </svg>
                    </div>

                    <!-- Block Info -->
                    <div class="flex-1 min-w-0">
                      <div class="text-xs font-bold truncate" :title="markedBlockContent.name">
                        {{ markedBlockContent.name }}
                      </div>
                      <div class="text-xs text-base-content/50 font-mono truncate">
                        {{ markedBlockContent.blockTypeId }}
                      </div>
                    </div>
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
                    @click="refreshAll"
                    class="btn btn-ghost btn-xs btn-circle"
                    title="Refresh all (layers, state, palette)"
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

          <!-- Block Palette - Collapsible -->
          <div class="collapse collapse-arrow bg-base-100 shadow-sm">
            <input type="checkbox" v-model="blocksPanelOpen" />
            <div class="collapse-title text-sm font-semibold p-2">
              Blocks ({{ palette.length }})
            </div>
            <div class="collapse-content">
              <div class="space-y-2">
                <!-- Add Marked Block Button -->
                <button
                  @click="addMarkedBlockToPalette"
                  :disabled="!markedBlockContent || addingToPalette"
                  class="btn btn-success btn-xs w-full"
                  title="Add currently marked block to palette"
                >
                  <svg xmlns="http://www.w3.org/2000/svg" class="h-3 w-3" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
                  </svg>
                  {{ addingToPalette ? 'Adding...' : 'Add Marked Block' }}
                </button>

                <!-- Palette Blocks List -->
                <div v-if="palette.length === 0" class="text-xs text-center text-base-content/50 py-4">
                  No blocks in palette. Mark a block and add it.
                </div>

                <div v-else class="grid grid-cols-2 gap-1 max-h-48 overflow-y-auto">
                  <div
                    v-for="(paletteBlock, index) in palette"
                    :key="paletteBlock.id"
                    class="relative group cursor-pointer"
                    :class="{ 'ring-2 ring-primary': selectedPaletteIndex === index }"
                    @click="selectPaletteBlock(index)"
                  >
                    <!-- Block Card -->
                    <div class="card bg-base-200 hover:bg-base-300 transition-colors">
                      <div class="card-body p-2">
                        <!-- Block Icon/Texture -->
                        <div class="w-full aspect-square bg-base-300 rounded flex items-center justify-center mb-1">
                          <img
                            v-if="paletteBlock.icon"
                            :src="getTextureUrl(paletteBlock.icon)"
                            :alt="paletteBlock.name"
                            class="w-full h-full object-contain"
                            @error="handleImageError($event, index)"
                          />
                          <svg v-else class="w-6 h-6 text-base-content/30" fill="currentColor" viewBox="0 0 20 20">
                            <path d="M3 4a1 1 0 011-1h12a1 1 0 011 1v2a1 1 0 01-1 1H4a1 1 0 01-1-1V4zM3 10a1 1 0 011-1h6a1 1 0 011 1v6a1 1 0 01-1 1H4a1 1 0 01-1-1v-6zM14 9a1 1 0 00-1 1v6a1 1 0 001 1h2a1 1 0 001-1v-6a1 1 0 00-1-1h-2z"/>
                          </svg>
                        </div>

                        <!-- Block Name -->
                        <div class="text-xs font-semibold truncate" :title="paletteBlock.name">
                          {{ paletteBlock.name }}
                        </div>

                        <!-- BlockType ID -->
                        <div class="text-xs text-base-content/50 truncate">
                          {{ paletteBlock.block.blockTypeId }}
                        </div>

                        <!-- Delete Button (visible on hover) -->
                        <button
                          @click.stop="removePaletteBlock(index)"
                          class="absolute top-1 right-1 btn btn-xs btn-circle btn-error opacity-0 group-hover:opacity-100 transition-opacity"
                          title="Remove from palette"
                        >
                          <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
                          </svg>
                        </button>
                      </div>
                    </div>
                  </div>
                </div>

                <!-- Save Palette Button -->
                <button
                  v-if="palette.length > 0"
                  @click="savePalette"
                  :disabled="savingPalette"
                  class="btn btn-primary btn-xs w-full mt-2"
                >
                  <span v-if="savingPalette" class="loading loading-spinner loading-xs"></span>
                  <span v-else>💾 Save Palette</span>
                </button>
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
import { ref, computed, watch, onMounted, onBeforeUnmount } from 'vue';
import { useModal } from '@/composables/useModal';
import NavigateSelectedBlockComponent from '@/components/NavigateSelectedBlockComponent.vue';
import { EditAction, type PaletteBlockDefinition, type Block, type BlockType } from '@nimbus/shared';

// Get all edit actions from enum
const editActions = Object.values(EditAction);

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
  editAction: EditAction.OPEN_CONFIG_DIALOG,
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
const currentEditAction = ref<EditAction>(EditAction.OPEN_CONFIG_DIALOG);
const savedEditAction = ref<EditAction>(EditAction.OPEN_CONFIG_DIALOG);
const selectedBlock = ref<{ x: number; y: number; z: number } | null>(null);
const markedBlock = ref<{ x: number; y: number; z: number } | null>(null);
const error = ref<string | null>(null);
const saving = ref(false);

// Modal state
const discardModal = ref<HTMLDialogElement | null>(null);

// Edit mode control state
const activating = ref(false);
const discarding = ref(false);

// Block Palette state
const palette = ref<PaletteBlockDefinition[]>([]);
const selectedPaletteIndex = ref<number | null>(null);
const blocksPanelOpen = ref(false);
const addingToPalette = ref(false);
const savingPalette = ref(false);

// Marked Block Content state
const markedBlockContent = ref<{ name: string; blockTypeId: string; block: Block } | null>(null);
const markedBlockIcon = ref<string | null>(null);
let markedBlockPollingInterval: number | null = null;

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
    case EditAction.OPEN_CONFIG_DIALOG:
      return 'Opens config dialog on block select';
    case EditAction.OPEN_EDITOR:
      return 'Opens block editor on select';
    case EditAction.MARK_BLOCK:
      return 'Marks block for paste';
    case EditAction.PASTE_BLOCK:
      return 'Pastes marked block to position';
    case EditAction.DELETE_BLOCK:
      return 'Deletes block at position';
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
      editAction: (data.editAction as EditAction) || EditAction.OPEN_CONFIG_DIALOG,
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

// Refresh all data (layers, edit state, palette)
async function refreshAll() {
  console.log('[Refresh] Reloading all data...');
  await Promise.all([
    fetchLayers(),
    fetchEditState(),
    loadEditSettings()
  ]);
  console.log('[Refresh] All data reloaded');
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
  await loadEditSettings();

  // Start marked block content polling
  startMarkedBlockPolling();
});

// Cleanup on unmount
onBeforeUnmount(() => {
  stopMarkedBlockPolling();
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

// ===== BLOCK PALETTE FUNCTIONS =====

// Load edit settings (palette) from API
async function loadEditSettings() {
  try {
    const response = await fetch(
      `${apiUrl.value}/api/editor/settings/worlds/${worldId.value}/editsettings?sessionId=${sessionId.value}`
    );

    if (!response.ok) {
      console.log('[Palette] Failed to load settings (status not ok), will create on first save');
      palette.value = [];
      return;
    }

    const data = await response.json();

    // Check if response contains an error
    if (data.error) {
      console.log('[Palette] No existing settings found, will create on first save');
      palette.value = [];
      return;
    }

    palette.value = data.palette || [];
    console.log('[Palette] Loaded', palette.value.length, 'blocks from palette');
  } catch (err) {
    console.log('[Palette] Error loading settings:', err, '- will create on first save');
    // Don't show error to user, palette is optional and will be created on first save
    palette.value = [];
  }
}

// Get texture URL for block icon
function getTextureUrl(icon: string): string {
  const apiBaseUrl = import.meta.env.VITE_API_URL || 'http://localhost:3000';
  return `${apiBaseUrl}/api/worlds/${worldId.value}/assets/${icon}`;
}

// Handle image load error (fallback to placeholder)
function handleImageError(event: Event, index: number) {
  const target = event.target as HTMLImageElement;
  target.style.display = 'none';
}

// Add marked block to palette
async function addMarkedBlockToPalette() {
  console.log('[Palette] Add marked block clicked', {
    hasMarkedBlockContent: !!markedBlockContent.value,
    markedBlockContent: markedBlockContent.value
  });

  if (!markedBlockContent.value) {
    error.value = 'No block is currently marked';
    console.error('[Palette] Cannot add - no marked block content');
    return;
  }

  addingToPalette.value = true;
  error.value = null;

  try {
    // Use already loaded marked block content
    const blockData = markedBlockContent.value.block;
    const blockTypeName = markedBlockContent.value.name;
    const icon = markedBlockIcon.value || undefined;

    console.log('[Palette] Creating palette entry:', {
      name: blockTypeName,
      blockTypeId: blockData.blockTypeId,
      hasIcon: !!icon
    });

    // Create palette entry
    const paletteBlock: PaletteBlockDefinition = {
      block: blockData,
      name: blockTypeName,
      icon,
    };

    // Add to palette
    palette.value.push(paletteBlock);
    console.log('[Palette] Added to palette, new size:', palette.value.length);

    // Auto-save palette
    await savePalette();
    console.log('[Palette] Block successfully added and saved');

  } catch (err) {
    error.value = err instanceof Error ? err.message : 'Failed to add block to palette';
    console.error('[Palette] Failed to add marked block to palette:', err);
  } finally {
    addingToPalette.value = false;
  }
}

// Select a palette block (sets as current marked block in Redis)
async function selectPaletteBlock(index: number) {
  selectedPaletteIndex.value = index;
  const paletteBlock = palette.value[index];

  console.log('[Palette] Selecting block:', paletteBlock.name);

  try {
    // Send block to Redis as marked block (will trigger polling update)
    const response = await fetch(
      `${apiUrl.value}/api/editor/${worldId.value}/session/${sessionId.value}/markedBlock`,
      {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(paletteBlock.block),
      }
    );

    if (!response.ok) {
      const errorText = await response.text();
      console.error('[Palette] Failed to set marked block:', errorText);
      throw new Error('Failed to set marked block');
    }

    const result = await response.json();
    console.log('[Palette] Marked block set successfully:', result);

    // Marked block content will be updated by polling within 2 seconds

  } catch (err) {
    error.value = err instanceof Error ? err.message : 'Failed to select block';
    console.error('[Palette] Failed to select palette block:', err);
  }
}

// Remove block from palette
function removePaletteBlock(index: number) {
  palette.value.splice(index, 1);

  // Clear selection if removed block was selected
  if (selectedPaletteIndex.value === index) {
    selectedPaletteIndex.value = null;
  } else if (selectedPaletteIndex.value !== null && selectedPaletteIndex.value > index) {
    // Adjust selection index if block before selected was removed
    selectedPaletteIndex.value--;
  }

  // Auto-save after removal
  savePalette();
}

// Save palette to server
async function savePalette() {
  console.log('[Palette] Saving palette with', palette.value.length, 'blocks');
  savingPalette.value = true;
  error.value = null;

  try {
    const url = `${apiUrl.value}/api/editor/settings/worlds/${worldId.value}/editsettings/palette?sessionId=${sessionId.value}`;
    console.log('[Palette] POST to:', url);
    console.log('[Palette] Payload:', palette.value);

    const response = await fetch(url, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(palette.value),
    });

    console.log('[Palette] Save response status:', response.status);

    if (!response.ok) {
      const errorData = await response.text();
      console.error('[Palette] Save failed with response:', errorData);
      throw new Error(`Failed to save palette: ${response.status} ${response.statusText}`);
    }

    const result = await response.json();
    console.log('[Palette] Save successful, response:', result);
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'Failed to save palette';
    console.error('[Palette] Save error:', err);
  } finally {
    savingPalette.value = false;
  }
}

// ===== MARKED BLOCK POLLING =====

// Poll marked block content
async function pollMarkedBlockContent() {
  console.log('[Polling] Checking marked block...', {
    markedBlockPosition: markedBlock.value,
    worldId: worldId.value,
    sessionId: sessionId.value
  });

  try {
    // Always try to fetch - server will return 404 if no block marked
    const url = `${apiUrl.value}/api/editor/${worldId.value}/session/${sessionId.value}/markedBlock`;
    console.log('[Polling] Fetching:', url);

    const response = await fetch(url);

    console.log('[Polling] Response status:', response.status);

    if (!response.ok) {
      // No marked block or error - clear content
      if (markedBlockContent.value !== null) {
        console.log('[Polling] Clearing marked block content (status not ok)');
        markedBlockContent.value = null;
        markedBlockIcon.value = null;
      }
      return;
    }

    const responseData = await response.json();
    console.log('[Polling] Received response data:', responseData);

    // Check if response contains an error
    if (responseData.error) {
      console.log('[Polling] Server returned error:', responseData.error);
      // Clear content when error is present
      if (markedBlockContent.value !== null) {
        console.log('[Polling] Clearing marked block content (error in response)');
        markedBlockContent.value = null;
        markedBlockIcon.value = null;
      }
      return;
    }

    const blockData: Block = responseData;

    // Check if content changed (compare blockTypeId)
    if (markedBlockContent.value?.blockTypeId === blockData.blockTypeId) {
      console.log('[Polling] Content unchanged, skipping update');
      return;
    }

    // Fetch BlockType to get description and texture
    let blockTypeName = `Block ${blockData.blockTypeId}`;
    let icon: string | null = null;

    try {
      const blockTypeResponse = await fetch(
        `${apiUrl.value}/api/worlds/${worldId.value}/blocktypes/${blockData.blockTypeId}`
      );

      if (blockTypeResponse.ok) {
        const blockType: BlockType = await blockTypeResponse.json();
        blockTypeName = blockType.description || blockTypeName;

        // Try to get texture from first modifier's visibility
        const firstModifier = blockType.modifiers?.[0];
        if (firstModifier?.visibility?.textures) {
          const textures = firstModifier.visibility.textures;
          // Get first available texture (top, front, or any face)
          icon = textures.top || textures.front || textures.side || Object.values(textures)[0] || null;
        }
      }
    } catch (err) {
      console.warn('Failed to fetch BlockType details for marked block:', err);
    }

    // Truncate name if too long
    if (blockTypeName.length > 40) {
      blockTypeName = blockTypeName.substring(0, 37) + '...';
    }

    // Update content
    markedBlockContent.value = {
      name: blockTypeName,
      blockTypeId: blockData.blockTypeId,
      block: blockData,
    };
    markedBlockIcon.value = icon;

    console.log('Marked block content updated:', blockTypeName);

  } catch (err) {
    // Silent fail - polling will retry
    console.debug('Failed to poll marked block content:', err);
  }
}

// Start marked block polling
function startMarkedBlockPolling() {
  if (markedBlockPollingInterval !== null) {
    console.log('[Polling] Already running, skipping start');
    return;
  }

  console.log('[Polling] Starting marked block polling (interval: 2s)');

  // Initial poll
  pollMarkedBlockContent();

  // Poll every 2 seconds
  markedBlockPollingInterval = window.setInterval(() => {
    pollMarkedBlockContent();
  }, 2000);
}

// Stop marked block polling
function stopMarkedBlockPolling() {
  if (markedBlockPollingInterval !== null) {
    clearInterval(markedBlockPollingInterval);
    markedBlockPollingInterval = null;
  }
}
</script>
