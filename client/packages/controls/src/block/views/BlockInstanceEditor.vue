<template>
  <div class="space-y-4">
    <!-- Loading State -->
    <div v-if="isLoading" class="flex justify-center py-12">
      <LoadingSpinner />
      <div class="ml-4 text-sm text-base-content/70">
        Loading block data...
      </div>
    </div>

    <!-- Error State -->
    <ErrorAlert v-else-if="error" :error="error" />

    <!-- No Coordinates -->
    <div v-else-if="!blockCoordinates" class="text-center py-12">
      <p class="text-base-content/70 text-lg">No block coordinates specified</p>
      <p class="text-base-content/50 text-sm mt-2">
        Add <code>?block=x,y,z</code> to the URL
      </p>
    </div>

    <!-- Block Editor -->
    <div v-else class="space-y-4">
      <!-- Block Info Card -->
      <div class="card bg-base-100 shadow-xl">
        <div class="card-body">
          <div class="flex items-center justify-between mb-4">
            <h2 class="card-title">
              Block at ({{ blockCoordinates.x }}, {{ blockCoordinates.y }}, {{ blockCoordinates.z }})
            </h2>
            <div class="badge" :class="blockExists ? 'badge-primary' : 'badge-success'">
              {{ blockExists ? 'Existing Block' : 'New Block' }}
            </div>
          </div>

          <!-- Two-column layout: BlockType/Status left (70%), Navigation right (30%) -->
          <div class="grid gap-2 mb-4" style="grid-template-columns: 70% 30%;">
            <!-- Left Column: Block Type and Status -->
            <div class="space-y-4">
              <!-- Block Type Selection -->
              <div class="form-control">
                <label class="label">
                  <span class="label-text font-semibold">Block Type</span>
                  <span class="label-text-alt text-error" v-if="!blockData.blockTypeId">Required</span>
                </label>

                <!-- Currently Selected Block Type -->
                <div
                  v-if="blockData.blockTypeId > 0 && !showBlockTypeSearch"
                  class="p-3 bg-base-200 rounded-lg flex items-center justify-between mb-2"
                >
                  <div>
                    <span class="font-mono font-bold">ID {{ blockData.blockTypeId }}</span>
                    <span class="mx-2">-</span>
                    <span v-if="selectedBlockType">{{ selectedBlockType.description || 'Unnamed' }}</span>
                    <span v-else class="text-base-content/50 italic">(BlockType details not loaded)</span>
                  </div>
                  <div class="flex gap-2">
                    <a
                      :href="getBlockTypeEditorUrl(blockData.blockTypeId)"
                      target="_blank"
                      class="btn btn-sm btn-primary"
                      title="Open BlockType in new tab"
                    >
                      Edit Type
                    </a>
                    <button class="btn btn-sm btn-ghost" @click="clearBlockType">
                      Change
                    </button>
                  </div>
                </div>

                <!-- Search Field (shown when changing or no block type selected) -->
                <div v-if="showBlockTypeSearch || blockData.blockTypeId === 0">
                  <SearchInput
                    v-model="blockTypeSearch"
                    placeholder="Search block types by ID or description..."
                    @search="handleBlockTypeSearch"
                  />

                  <!-- Search Results (shown when searching) -->
                  <div
                    v-if="blockTypeSearch && blockTypeSearchResults.length > 0"
                    class="mt-2 border border-base-300 rounded-lg max-h-60 overflow-y-auto bg-base-100"
                  >
                    <div
                      v-for="blockType in blockTypeSearchResults"
                      :key="blockType.id"
                      class="p-3 hover:bg-base-200 cursor-pointer border-b border-base-300 last:border-b-0"
                      @click="selectBlockType(blockType)"
                    >
                      <span class="font-mono font-bold">ID {{ blockType.id }}</span>
                      <span class="mx-2">-</span>
                      <span>{{ blockType.description || 'Unnamed' }}</span>
                    </div>
                  </div>

                  <!-- No Results -->
                  <div
                    v-else-if="hasSearched && blockTypeSearch && blockTypeSearchResults.length === 0 && !loadingBlockTypes"
                    class="mt-2 p-4 text-center text-base-content/50 text-sm"
                  >
                    No block types found for "{{ blockTypeSearch }}"
                  </div>
                </div>
              </div>

              <!-- Status -->
              <div class="form-control">
                <label class="label">
                  <span class="label-text font-semibold">Status</span>
                  <span class="label-text-alt">0-255</span>
                </label>
                <input
                  v-model.number="blockData.status"
                  type="number"
                  min="0"
                  max="255"
                  class="input input-bordered"
                  placeholder="0"
                />
              </div>
            </div>

            <!-- Right Column: Navigate Component -->
            <div class="flex items-start justify-center">
              <NavigateSelectedBlockComponent
                :selected-block="blockCoordinates"
                :step="1"
                :size="140"
                :show-execute-button="false"
                @navigate="handleNavigate"
              />
            </div>
          </div>

          <!-- Geometry Offsets Section -->
          <CollapsibleSection
            title="Geometry Offsets"
            :model-value="hasOffsets"
            @update:model-value="toggleOffsets"
          >
            <OffsetsEditor
              v-model="blockData.offsets"
              :shape="currentShape"
            />
          </CollapsibleSection>

          <!-- Face Visibility Section -->
          <CollapsibleSection
            title="Face Visibility"
            :model-value="hasFaceVisibility"
            @update:model-value="toggleFaceVisibility"
          >
            <div class="space-y-3 pt-2">
              <p class="text-sm text-base-content/70">Control which faces are rendered</p>

              <!-- Face checkboxes in a grid -->
              <div class="grid grid-cols-3 gap-2">
                <label class="label cursor-pointer justify-start gap-2">
                  <input
                    type="checkbox"
                    class="checkbox checkbox-sm"
                    :checked="isFaceVisible(1)"
                    @change="toggleFace(1)"
                  />
                  <span class="label-text">Top</span>
                </label>

                <label class="label cursor-pointer justify-start gap-2">
                  <input
                    type="checkbox"
                    class="checkbox checkbox-sm"
                    :checked="isFaceVisible(2)"
                    @change="toggleFace(2)"
                  />
                  <span class="label-text">Bottom</span>
                </label>

                <label class="label cursor-pointer justify-start gap-2">
                  <input
                    type="checkbox"
                    class="checkbox checkbox-sm"
                    :checked="isFaceVisible(4)"
                    @change="toggleFace(4)"
                  />
                  <span class="label-text">Left</span>
                </label>

                <label class="label cursor-pointer justify-start gap-2">
                  <input
                    type="checkbox"
                    class="checkbox checkbox-sm"
                    :checked="isFaceVisible(8)"
                    @change="toggleFace(8)"
                  />
                  <span class="label-text">Right</span>
                </label>

                <label class="label cursor-pointer justify-start gap-2">
                  <input
                    type="checkbox"
                    class="checkbox checkbox-sm"
                    :checked="isFaceVisible(16)"
                    @change="toggleFace(16)"
                  />
                  <span class="label-text">Front</span>
                </label>

                <label class="label cursor-pointer justify-start gap-2">
                  <input
                    type="checkbox"
                    class="checkbox checkbox-sm"
                    :checked="isFaceVisible(32)"
                    @change="toggleFace(32)"
                  />
                  <span class="label-text">Back</span>
                </label>
              </div>

              <!-- Fixed/Auto mode -->
              <div class="divider divider-start text-xs">Mode</div>
              <label class="label cursor-pointer justify-start gap-2">
                <input
                  type="checkbox"
                  class="checkbox checkbox-sm"
                  :checked="isFixedMode"
                  @change="toggleFixedMode"
                />
                <span class="label-text">Fixed Mode (disable auto-calculation)</span>
              </label>

              <!-- Current value display -->
              <div class="text-xs text-base-content/50">
                Bitfield value: {{ blockData.faceVisibility?.value || 0 }}
              </div>
            </div>
          </CollapsibleSection>

          <!-- Metadata Section -->
          <div class="divider">Metadata</div>

          <div v-if="blockData.metadata" class="grid grid-cols-2 gap-4">
            <!-- Group ID -->
            <div class="form-control">
              <label class="label">
                <span class="label-text">Group ID</span>
              </label>
              <input
                v-model.number="blockData.metadata.groupId"
                type="number"
                class="input input-bordered input-sm"
                placeholder="Optional"
              />
            </div>

            <!-- Interactive -->
            <div class="form-control">
              <label class="label cursor-pointer justify-start gap-2">
                <input
                  v-model="blockData.metadata.interactive"
                  type="checkbox"
                  class="checkbox checkbox-sm"
                />
                <span class="label-text">Interactive</span>
              </label>
            </div>
          </div>

          <!-- Modifiers Section -->
          <div class="divider">Modifiers (per Status)</div>

          <div class="space-y-2">
            <div
              v-for="(modifier, status) in blockData.modifiers"
              :key="status"
              class="flex items-center gap-2 p-2 bg-base-200 rounded"
            >
              <span class="font-mono text-sm">Status {{ status }}:</span>
              <span class="flex-1 text-sm text-base-content/70">Modifier defined</span>
              <button class="btn btn-xs btn-ghost" @click="editModifier(Number(status))">
                Edit
              </button>
              <button class="btn btn-xs btn-error btn-ghost" @click="deleteModifier(Number(status))">
                Delete
              </button>
            </div>

            <button class="btn btn-sm btn-outline w-full" @click="addModifier">
              + Add Modifier for Status {{ blockData.status ?? 0 }}
            </button>
          </div>
        </div>
      </div>

      <!-- Action Buttons -->
      <div class="card bg-base-100 shadow-xl">
        <div class="card-body">
          <div class="flex justify-between gap-2">
            <button
              class="btn btn-error"
              @click="handleDelete"
              :disabled="!blockExists || saving"
            >
              Delete Block
            </button>

            <div class="flex gap-2">
              <button class="btn btn-ghost" @click="handleCancel" :disabled="saving">
                Cancel
              </button>
              <button
                class="btn btn-primary"
                @click="handleApply"
                :disabled="!isValid || saving || !hasChanges"
              >
                {{ saving ? 'Saving...' : 'Apply' }}
              </button>
              <button
                class="btn btn-success"
                @click="handleSave"
                :disabled="!isValid || saving || !hasChanges"
              >
                {{ saving ? 'Saving...' : 'Save & Close' }}
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Modifier Editor Dialog -->
    <ModifierEditorDialog
      v-if="showModifierDialog"
      :modifier="editingModifier!"
      :status-number="editingStatus!"
      :world-id="worldId"
      @close="showModifierDialog = false"
      @save="handleModifierSave"
    />

    <!-- Confirm Dialog -->
    <dialog ref="confirmDialog" class="modal">
      <div class="modal-box">
        <h3 class="font-bold text-lg">{{ confirmTitle }}</h3>
        <p class="py-4">{{ confirmMessage }}</p>
        <div class="modal-action">
          <button class="btn" @click="handleConfirmCancel">Cancel</button>
          <button class="btn btn-error" @click="handleConfirmOk">{{ confirmOkText }}</button>
        </div>
      </div>
      <form method="dialog" class="modal-backdrop">
        <button @click="handleConfirmCancel">close</button>
      </form>
    </dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue';
import type { Block, BlockModifier, BlockType } from '@nimbus/shared';
import { useModal, ModalSizePreset } from '@/composables/useModal';
import { useBlockTypes } from '@/composables/useBlockTypes';
import LoadingSpinner from '@/components/LoadingSpinner.vue';
import ErrorAlert from '@/components/ErrorAlert.vue';
import SearchInput from '@/components/SearchInput.vue';
import CollapsibleSection from '@/components/CollapsibleSection.vue';
import OffsetsEditor from '@editors/OffsetsEditor.vue';
import ModifierEditorDialog from '@/components/ModifierEditorDialog.vue';
import NavigateSelectedBlockComponent from '@/components/NavigateSelectedBlockComponent.vue';

// Parse URL parameters
function parseBlockCoordinates(): { x: number; y: number; z: number } | null {
  const params = new URLSearchParams(window.location.search);

  // Support two formats:
  // 1. ?block=x,y,z (comma-separated)
  // 2. ?x=10&y=64&z=5 (separate parameters, used by client)
  const blockParam = params.get('block');

  if (blockParam) {
    // Format 1: comma-separated
    const parts = blockParam.split(',').map(Number);
    if (parts.length !== 3 || parts.some(isNaN)) {
      return null;
    }
    return { x: parts[0], y: parts[1], z: parts[2] };
  }

  // Format 2: separate parameters
  const x = params.get('x');
  const y = params.get('y');
  const z = params.get('z');

  if (x && y && z) {
    const coords = { x: Number(x), y: Number(y), z: Number(z) };
    if (!isNaN(coords.x) && !isNaN(coords.y) && !isNaN(coords.z)) {
      return coords;
    }
  }

  return null;
}

// Block coordinates (reactive ref, not computed)
const blockCoordinates = ref<{ x: number; y: number; z: number } | null>(parseBlockCoordinates());

// Get worldId from URL (once, not reactive - needed for composables)
const params = new URLSearchParams(window.location.search);
const worldId = params.get('world') || import.meta.env.VITE_WORLD_ID || 'main';

// Modal composable
const {
  isEmbedded,
  closeModal,
  sendNotification,
  notifyReady,
} = useModal();

// Block types composable
const { blockTypes, loading: loadingBlockTypes, searchBlockTypes, getBlockType } = useBlockTypes(worldId);

// BlockType search state
const blockTypeSearch = ref('');
const blockTypeSearchResults = ref<BlockType[]>([]);
const loadedBlockType = ref<BlockType | null>(null);
const showBlockTypeSearch = ref(false);
const hasSearched = ref(false); // Track if search has been executed

// State
const loading = ref(false);
const saving = ref(false);
const error = ref<string | null>(null);
const blockExists = ref(false);
const originalBlock = ref<Block | null>(null);
const blockData = ref<Block>({
  position: { x: 0, y: 0, z: 0 },
  blockTypeId: 0,
  status: 0,
  metadata: {},
});

// Modifier dialog state
const showModifierDialog = ref(false);
const editingModifier = ref<BlockModifier | null>(null);
const editingStatus = ref<number | null>(null);

// Confirm dialog state
const confirmDialog = ref<HTMLDialogElement | null>(null);
const confirmTitle = ref('');
const confirmMessage = ref('');
const confirmOkText = ref('OK');
const confirmResolve = ref<((value: boolean) => void) | null>(null);

// Computed
const isLoading = computed(() => {
  return loading.value || loadingBlockTypes.value;
});

const selectedBlockType = computed(() => {
  if (!blockData.value.blockTypeId) return null;
  return loadedBlockType.value;
});

const isValid = computed(() => {
  return blockData.value.blockTypeId > 0;
});

const hasChanges = computed(() => {
  if (!originalBlock.value) return true; // New block
  return JSON.stringify(blockData.value) !== JSON.stringify(originalBlock.value);
});

const hasOffsets = computed(() => {
  return blockData.value.offsets && blockData.value.offsets.length > 0;
});

const hasFaceVisibility = computed(() => {
  return blockData.value.faceVisibility !== undefined;
});

const isFixedMode = computed(() => {
  if (!blockData.value.faceVisibility) return false;
  return (blockData.value.faceVisibility.value & 64) !== 0; // FIXED flag
});

const currentShape = computed(() => {
  // Try to get shape from block's own modifiers first
  const status = blockData.value.status ?? 0;
  if (blockData.value.modifiers?.[status]?.visibility?.shape) {
    return blockData.value.modifiers[status].visibility!.shape;
  }

  // Fallback to loaded block type
  if (selectedBlockType.value?.modifiers?.[status]?.visibility?.shape) {
    return selectedBlockType.value.modifiers[status].visibility!.shape;
  }

  // Default to CUBE
  return 1;
});

// Generate BlockTypeEditor URL with blockTypeId parameter
function getBlockTypeEditorUrl(blockTypeId: number): string {
  const params = new URLSearchParams(window.location.search);

  // Use relative path to material-editor.html
  const baseUrl = 'material-editor.html';

  // Preserve world and sessionId parameters, add blockTypeId
  const newParams = new URLSearchParams();
  if (params.get('world')) newParams.set('world', params.get('world')!);
  if (params.get('sessionId')) newParams.set('sessionId', params.get('sessionId')!);
  newParams.set('id', blockTypeId.toString());

  return `${baseUrl}?${newParams.toString()}`;
}

// Handle navigation from NavigateSelectedBlockComponent
async function handleNavigate(position: { x: number; y: number; z: number }) {
  // Update URL parameters
  const params = new URLSearchParams(window.location.search);
  params.set('x', position.x.toString());
  params.set('y', position.y.toString());
  params.set('z', position.z.toString());

  // Update URL without page reload
  const newUrl = `${window.location.pathname}?${params.toString()}`;
  window.history.pushState({}, '', newUrl);

  // Update blockCoordinates ref to trigger reactivity
  // This will automatically trigger the watcher which calls loadBlock()
  blockCoordinates.value = { ...position };

  // Also notify server to update selection highlight in 3D client
  try {
    const sessionId = params.get('sessionId');
    if (sessionId) {
      const apiUrl = import.meta.env.VITE_API_URL || 'http://localhost:3000';
      const response = await fetch(
        `${apiUrl}/api/worlds/${worldId}/session/${sessionId}/selectedEditBlock/navigate`,
        {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
          },
          body: JSON.stringify(position),
        }
      );

      if (!response.ok) {
        console.warn('Failed to update server selection:', response.statusText);
      }
    }
  } catch (err) {
    console.warn('Failed to notify server about navigation:', err);
    // Don't block the UI if server notification fails
  }
}

// Confirm dialog helpers
function showConfirm(title: string, message: string, okText: string = 'OK'): Promise<boolean> {
  // Use native confirm if not embedded
  if (!isEmbedded()) {
    return Promise.resolve(window.confirm(message));
  }

  // Use custom modal if embedded
  return new Promise((resolve) => {
    confirmTitle.value = title;
    confirmMessage.value = message;
    confirmOkText.value = okText;
    confirmResolve.value = resolve;
    confirmDialog.value?.showModal();
  });
}

function handleConfirmOk() {
  confirmDialog.value?.close();
  confirmResolve.value?.(true);
  confirmResolve.value = null;
}

function handleConfirmCancel() {
  confirmDialog.value?.close();
  confirmResolve.value?.(false);
  confirmResolve.value = null;
}

// BlockType search and selection
async function handleBlockTypeSearch(query: string) {
  console.log('[BlockInstanceEditor] handleBlockTypeSearch called', { query, trimmed: query?.trim() });

  if (!query || query.trim().length === 0) {
    console.log('[BlockInstanceEditor] Empty query, clearing results');
    blockTypeSearchResults.value = [];
    hasSearched.value = false;
    return;
  }

  try {
    console.log('[BlockInstanceEditor] Calling searchBlockTypes with query:', query);
    await searchBlockTypes(query);
    hasSearched.value = true; // Mark that search has been executed
    console.log('[BlockInstanceEditor] Search completed, blockTypes.value:', blockTypes.value);
    console.log('[BlockInstanceEditor] blockTypes.value.length:', blockTypes.value.length);
    blockTypeSearchResults.value = blockTypes.value.slice(0, 20); // Limit to 20 results
    console.log('[BlockInstanceEditor] Search results:', blockTypeSearchResults.value.length, 'results');
  } catch (err) {
    console.error('[BlockInstanceEditor] Failed to search block types:', err);
  }
}

function selectBlockType(blockType: BlockType) {
  blockData.value.blockTypeId = blockType.id;
  loadedBlockType.value = blockType;
  blockTypeSearch.value = '';
  blockTypeSearchResults.value = [];
  showBlockTypeSearch.value = false; // Hide search after selection
}

function clearBlockType() {
  blockData.value.blockTypeId = 0;
  loadedBlockType.value = null;
  blockTypeSearch.value = '';
  blockTypeSearchResults.value = [];
  hasSearched.value = false; // Reset search state
  showBlockTypeSearch.value = true; // Show search when changing
}

// Load BlockType details
async function loadBlockTypeDetails(blockTypeId: number) {
  try {
    const blockType = await getBlockType(blockTypeId);
    if (blockType) {
      loadedBlockType.value = blockType;
    } else {
      console.warn(`BlockType ${blockTypeId} not found`);
      loadedBlockType.value = null;
    }
  } catch (err) {
    console.error('Failed to load BlockType details:', err);
    loadedBlockType.value = null;
  }
}

// Load block data
async function loadBlock() {
  if (!blockCoordinates.value) {
    return;
  }

  loading.value = true;
  error.value = null;

  try {
    const { x, y, z } = blockCoordinates.value;
    const apiUrl = import.meta.env.VITE_API_URL || 'http://localhost:3000';
    const url = `${apiUrl}/api/worlds/${worldId}/blocks/${x}/${y}/${z}`;

    // Fetch with timeout
    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), 5000);

    const response = await fetch(url, {
      signal: controller.signal,
      headers: {
        'Accept': 'application/json',
      }
    }).finally(() => clearTimeout(timeoutId));

    if (response.status === 404) {
      // Block doesn't exist yet - create new
      blockExists.value = false;
      blockData.value = {
        position: blockCoordinates.value,
        blockTypeId: 0,
        status: 0,
        metadata: {},
      };
      originalBlock.value = null;
      loadedBlockType.value = null;
    } else if (response.ok) {
      // Block exists - load data
      const block = await response.json();
      blockExists.value = true;

      // Ensure metadata is always an object
      if (!block.metadata) {
        block.metadata = {};
      }

      blockData.value = block;
      originalBlock.value = JSON.parse(JSON.stringify(block));

      // Load BlockType details if blockTypeId is set
      if (block.blockTypeId && block.blockTypeId > 0) {
        await loadBlockTypeDetails(block.blockTypeId);
      } else {
        loadedBlockType.value = null;
      }
    } else {
      throw new Error(`Failed to load block: ${response.statusText}`);
    }
  } catch (err) {
    if (err instanceof Error) {
      if (err.name === 'AbortError') {
        error.value = 'Request timeout - is the server running?';
      } else {
        error.value = err.message;
      }
    } else {
      error.value = 'Failed to load block';
    }
  } finally {
    loading.value = false;
  }
}

// Modifier management
function addModifier() {
  const status = blockData.value.status ?? 0;
  const modifier: BlockModifier = {
    visibility: { shape: 1, textures: {} },
  };

  editingModifier.value = modifier;
  editingStatus.value = status;
  showModifierDialog.value = true;
}

function editModifier(status: number) {
  const modifier = blockData.value.modifiers?.[status];
  if (!modifier) return;

  editingModifier.value = JSON.parse(JSON.stringify(modifier));
  editingStatus.value = status;
  showModifierDialog.value = true;
}

function deleteModifier(status: number) {
  if (blockData.value.modifiers) {
    delete blockData.value.modifiers[status];

    // Clean up modifiers object if empty
    if (Object.keys(blockData.value.modifiers).length === 0) {
      blockData.value.modifiers = undefined;
    }
  }
}

function handleModifierSave(modifier: BlockModifier) {
  if (editingStatus.value === null) return;

  if (!blockData.value.modifiers) {
    blockData.value.modifiers = {};
  }

  blockData.value.modifiers[editingStatus.value] = modifier;
  showModifierDialog.value = false;
}

// Toggle offsets
function toggleOffsets(enabled: boolean) {
  if (!enabled) {
    blockData.value.offsets = undefined;
  } else if (!blockData.value.offsets) {
    blockData.value.offsets = [];
  }
}

// Toggle face visibility
function toggleFaceVisibility(enabled: boolean) {
  if (!enabled) {
    blockData.value.faceVisibility = undefined;
  } else if (!blockData.value.faceVisibility) {
    // Initialize with all faces visible
    blockData.value.faceVisibility = { value: 63 }; // 0b00111111 = all 6 faces
  }
}

// Check if specific face is visible
function isFaceVisible(faceFlag: number): boolean {
  if (!blockData.value.faceVisibility) return false;
  return (blockData.value.faceVisibility.value & faceFlag) !== 0;
}

// Toggle specific face
function toggleFace(faceFlag: number) {
  if (!blockData.value.faceVisibility) {
    blockData.value.faceVisibility = { value: 0 };
  }
  blockData.value.faceVisibility.value ^= faceFlag; // XOR to toggle
}

// Toggle fixed mode
function toggleFixedMode() {
  if (!blockData.value.faceVisibility) {
    blockData.value.faceVisibility = { value: 0 };
  }
  blockData.value.faceVisibility.value ^= 64; // Toggle FIXED flag (bit 6)
}

// Save/Delete operations
async function saveBlock(closeAfter: boolean = false) {
  if (!blockCoordinates.value || !isValid.value) return;

  saving.value = true;
  error.value = null;

  try {
    const { x, y, z } = blockCoordinates.value;
    const apiUrl = import.meta.env.VITE_API_URL || 'http://localhost:3000';

    const method = blockExists.value ? 'PUT' : 'POST';

    // Prepare block data for API (remove position as it's in URL)
    const { position, ...blockDataForApi } = blockData.value;

    const response = await fetch(`${apiUrl}/api/worlds/${worldId}/blocks/${x}/${y}/${z}`, {
      method,
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(blockDataForApi),
    });

    if (!response.ok) {
      const errorData = await response.json().catch(() => ({ error: response.statusText }));
      throw new Error(errorData.error || 'Failed to save block');
    }

    const savedBlock = await response.json();
    blockExists.value = true;
    blockData.value = savedBlock;
    originalBlock.value = JSON.parse(JSON.stringify(savedBlock));

    // Send notification
    if (isEmbedded()) {
      sendNotification('0', 'Block Editor', 'Block saved successfully');
    }

    if (closeAfter && isEmbedded()) {
      closeModal('saved');
    }
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'Failed to save block';
    console.error('Failed to save block:', err);

    if (isEmbedded()) {
      sendNotification('1', 'Block Editor', `Error: ${error.value}`);
    }
  } finally {
    saving.value = false;
  }
}

async function deleteBlock() {
  if (!blockCoordinates.value || !blockExists.value) return;

  const confirmed = await showConfirm(
    'Delete Block',
    'Are you sure you want to delete this block?',
    'Delete'
  );

  if (!confirmed) {
    return;
  }

  saving.value = true;
  error.value = null;

  try {
    const { x, y, z } = blockCoordinates.value;
    const apiUrl = import.meta.env.VITE_API_URL || 'http://localhost:3000';

    const response = await fetch(`${apiUrl}/api/worlds/${worldId}/blocks/${x}/${y}/${z}`, {
      method: 'DELETE',
    });

    if (!response.ok && response.status !== 404) {
      const errorData = await response.json().catch(() => ({ error: response.statusText }));
      throw new Error(errorData.error || 'Failed to delete block');
    }

    // Send notification
    if (isEmbedded()) {
      sendNotification('0', 'Block Editor', 'Block deleted successfully');
      closeModal('deleted');
    } else {
      // Reload page to show "no block" state
      window.location.reload();
    }
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'Failed to delete block';
    console.error('Failed to delete block:', err);

    if (isEmbedded()) {
      sendNotification('1', 'Block Editor', `Error: ${error.value}`);
    }
  } finally {
    saving.value = false;
  }
}

// Button handlers
async function handleSave() {
  await saveBlock(true); // Save and close
}

async function handleApply() {
  await saveBlock(false); // Save but keep open
}

async function handleCancel() {
  if (hasChanges.value) {
    const confirmed = await showConfirm(
      'Discard Changes',
      'Discard unsaved changes?',
      'Discard'
    );

    if (!confirmed) {
      return;
    }
  }

  if (isEmbedded()) {
    closeModal('cancelled');
  }
}

async function handleDelete() {
  await deleteBlock();
}

// Lifecycle
onMounted(async () => {
  if (isEmbedded()) {
    notifyReady();
  }

  await loadBlock();
});

// Watch for block type changes to reload
watch(() => blockCoordinates.value, () => {
  if (blockCoordinates.value) {
    loadBlock();
  }
});
</script>
