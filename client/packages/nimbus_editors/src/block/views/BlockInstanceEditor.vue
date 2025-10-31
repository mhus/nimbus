<template>
  <div class="space-y-4">
    <!-- Loading State -->
    <div v-if="loading" class="flex justify-center py-12">
      <LoadingSpinner />
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
          <h2 class="card-title">
            Block at ({{ blockCoordinates.x }}, {{ blockCoordinates.y }}, {{ blockCoordinates.z }})
          </h2>

          <!-- Block Type Selection -->
          <div class="form-control">
            <label class="label">
              <span class="label-text font-semibold">Block Type</span>
              <span class="label-text-alt text-error" v-if="!blockData.blockTypeId">Required</span>
            </label>
            <select
              v-model.number="blockData.blockTypeId"
              class="select select-bordered"
              :class="{ 'select-error': !blockData.blockTypeId }"
            >
              <option :value="0" disabled>Select a block type...</option>
              <option
                v-for="blockType in blockTypes"
                :key="blockType.id"
                :value="blockType.id"
              >
                {{ blockType.id }} - {{ blockType.description || 'Unnamed' }}
              </option>
            </select>
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

          <!-- Metadata Section -->
          <div class="divider">Metadata</div>

          <div class="grid grid-cols-2 gap-4">
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
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue';
import type { Block, BlockModifier } from '@nimbus/shared';
import { useModal, ModalSizePreset } from '@/composables/useModal';
import { useBlockTypes } from '@/composables/useBlockTypes';
import LoadingSpinner from '@/components/LoadingSpinner.vue';
import ErrorAlert from '@/components/ErrorAlert.vue';
import ModifierEditorDialog from '@/components/ModifierEditorDialog.vue';

// Parse URL parameters
const params = new URLSearchParams(window.location.search);
const blockParam = params.get('block');
const worldId = params.get('world') || import.meta.env.VITE_WORLD_ID || 'main';

const blockCoordinates = computed(() => {
  if (!blockParam) return null;

  const parts = blockParam.split(',').map(Number);
  if (parts.length !== 3 || parts.some(isNaN)) return null;

  return { x: parts[0], y: parts[1], z: parts[2] };
});

// Modal composable
const {
  isEmbedded,
  closeModal,
  sendNotification,
  notifyReady,
} = useModal();

// Block types composable
const { blockTypes, loading: loadingBlockTypes } = useBlockTypes(worldId);

// State
const loading = ref(true);
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

// Computed
const isValid = computed(() => {
  return blockData.value.blockTypeId > 0;
});

const hasChanges = computed(() => {
  if (!originalBlock.value) return true; // New block
  return JSON.stringify(blockData.value) !== JSON.stringify(originalBlock.value);
});

// Load block data
async function loadBlock() {
  if (!blockCoordinates.value) return;

  loading.value = true;
  error.value = null;

  try {
    const { x, y, z } = blockCoordinates.value;
    const apiUrl = import.meta.env.VITE_API_URL || 'http://localhost:3000';
    const response = await fetch(`${apiUrl}/api/worlds/${worldId}/blocks/${x}/${y}/${z}`);

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
    } else if (response.ok) {
      // Block exists - load data
      const block = await response.json();
      blockExists.value = true;
      blockData.value = block;
      originalBlock.value = JSON.parse(JSON.stringify(block));
    } else {
      throw new Error(`Failed to load block: ${response.statusText}`);
    }
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'Failed to load block';
    console.error('Failed to load block:', err);
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

// Save/Delete operations
async function saveBlock(closeAfter: boolean = false) {
  if (!blockCoordinates.value || !isValid.value) return;

  saving.value = true;
  error.value = null;

  try {
    const { x, y, z } = blockCoordinates.value;
    const apiUrl = import.meta.env.VITE_API_URL || 'http://localhost:3000';

    const method = blockExists.value ? 'PUT' : 'POST';
    const response = await fetch(`${apiUrl}/api/worlds/${worldId}/blocks/${x}/${y}/${z}`, {
      method,
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(blockData.value),
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

  if (!confirm('Are you sure you want to delete this block?')) {
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

function handleCancel() {
  if (hasChanges.value && !confirm('Discard unsaved changes?')) {
    return;
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
