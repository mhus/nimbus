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

    <!-- Main Content -->
    <main class="flex-1 container mx-auto px-4 py-6 max-w-2xl">
      <!-- Error Display -->
      <div v-if="error" class="alert alert-error shadow-lg mb-4">
        <svg xmlns="http://www.w3.org/2000/svg" class="stroke-current shrink-0 h-6 w-6" fill="none" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 14l2-2m0 0l2-2m-2 2l-2-2m2 2l2 2m7-2a9 9 0 11-18 0 9 9 0 0118 0z" />
        </svg>
        <span>{{ error }}</span>
      </div>

      <!-- Edit Action Configuration -->
      <div class="card bg-base-100 shadow-xl mb-4">
        <div class="card-body">
          <h2 class="card-title">Edit Action</h2>
          <p class="text-sm text-base-content/70 mb-4">
            Choose the action to perform when selecting a block
          </p>

          <div class="form-control w-full">
            <label class="label">
              <span class="label-text">Current Action</span>
            </label>
            <select
              v-model="currentEditAction"
              class="select select-bordered w-full"
              :disabled="saving"
            >
              <option v-for="action in editActions" :key="action" :value="action">
                {{ formatActionName(action) }}
              </option>
            </select>
            <label class="label">
              <span class="label-text-alt">{{ getActionDescription(currentEditAction) }}</span>
            </label>
          </div>

          <div class="card-actions justify-end mt-4">
            <button
              class="btn btn-primary"
              @click="saveEditAction"
              :disabled="saving || !hasChanges"
            >
              <span v-if="saving" class="loading loading-spinner loading-sm"></span>
              {{ saving ? 'Saving...' : 'Save' }}
            </button>
          </div>
        </div>
      </div>

      <!-- Selected Block Display -->
      <div class="card bg-base-100 shadow-xl">
        <div class="card-body">
          <h2 class="card-title">Selected Block</h2>
          <p class="text-sm text-base-content/70 mb-4">
            Current block selection (updates automatically)
          </p>

          <div v-if="selectedBlock" class="stats shadow w-full">
            <div class="stat place-items-center">
              <div class="stat-title">X</div>
              <div class="stat-value text-primary">{{ selectedBlock.x }}</div>
            </div>
            <div class="stat place-items-center">
              <div class="stat-title">Y</div>
              <div class="stat-value text-secondary">{{ selectedBlock.y }}</div>
            </div>
            <div class="stat place-items-center">
              <div class="stat-title">Z</div>
              <div class="stat-value text-accent">{{ selectedBlock.z }}</div>
            </div>
          </div>

          <div v-else class="alert alert-info shadow-lg">
            <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" class="stroke-current shrink-0 w-6 h-6">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path>
            </svg>
            <span>No block selected</span>
          </div>

          <!-- Marked Block Display -->
          <div v-if="markedBlock" class="mt-4">
            <h3 class="font-semibold mb-2">Marked Block (for copy/move)</h3>
            <div class="stats shadow w-full bg-warning/10">
              <div class="stat place-items-center">
                <div class="stat-title">X</div>
                <div class="stat-value text-sm">{{ markedBlock.x }}</div>
              </div>
              <div class="stat place-items-center">
                <div class="stat-title">Y</div>
                <div class="stat-value text-sm">{{ markedBlock.y }}</div>
              </div>
              <div class="stat place-items-center">
                <div class="stat-title">Z</div>
                <div class="stat-value text-sm">{{ markedBlock.z }}</div>
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
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue';
import { useModal } from '@/composables/useModal';

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
const apiUrl = ref(import.meta.env.VITE_API_URL || 'http://localhost:3000');

// State
const currentEditAction = ref<EditAction>('OPEN_CONFIG_DIALOG');
const savedEditAction = ref<EditAction>('OPEN_CONFIG_DIALOG');
const selectedBlock = ref<{ x: number; y: number; z: number } | null>(null);
const markedBlock = ref<{ x: number; y: number; z: number } | null>(null);
const error = ref<string | null>(null);
const saving = ref(false);

// Polling interval
let pollInterval: number | null = null;

// Computed
const hasChanges = computed(() => currentEditAction.value !== savedEditAction.value);

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
      return 'Opens the config dialog when selecting a block';
    case 'OPEN_EDITOR':
      return 'Opens the block editor at the selected position';
    case 'MARK_BLOCK':
      return 'Marks the block for copy/move operations';
    case 'COPY_BLOCK':
      return 'Copies the marked block to the selected position';
    case 'DELETE_BLOCK':
      return 'Deletes the block at the selected position';
    case 'MOVE_BLOCK':
      return 'Moves the marked block to the selected position';
    default:
      return '';
  }
}

// Fetch current edit action from API
async function fetchEditAction() {
  try {
    const response = await fetch(
      `${apiUrl.value}/api/worlds/${worldId.value}/session/${sessionId.value}/editAction`
    );

    if (!response.ok) {
      throw new Error(`Failed to fetch edit action: ${response.statusText}`);
    }

    const data = await response.json();
    currentEditAction.value = data.editAction as EditAction;
    savedEditAction.value = data.editAction as EditAction;
    error.value = null;
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'Unknown error';
    console.error('Failed to fetch edit action:', err);
  }
}

// Save edit action to API
async function saveEditAction() {
  saving.value = true;
  error.value = null;

  try {
    const response = await fetch(
      `${apiUrl.value}/api/worlds/${worldId.value}/session/${sessionId.value}/editAction`,
      {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ editAction: currentEditAction.value }),
      }
    );

    if (!response.ok) {
      throw new Error(`Failed to save edit action: ${response.statusText}`);
    }

    const data = await response.json();
    savedEditAction.value = data.editAction as EditAction;
    error.value = null;
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'Unknown error';
    console.error('Failed to save edit action:', err);
  } finally {
    saving.value = false;
  }
}

// Fetch selected block from API
async function fetchSelectedBlock() {
  try {
    const response = await fetch(
      `${apiUrl.value}/api/worlds/${worldId.value}/session/${sessionId.value}/selectedEditBlock`
    );

    if (!response.ok) {
      throw new Error(`Failed to fetch selected block: ${response.statusText}`);
    }

    const data = await response.json();
    selectedBlock.value = data.selectedEditBlock;
    markedBlock.value = data.markedEditBlock;
  } catch (err) {
    console.error('Failed to fetch selected block:', err);
    // Don't set error here, as this is a polling operation
  }
}

// Start polling for selected block updates
function startPolling() {
  // Initial fetch
  fetchSelectedBlock();

  // Poll every 1 second
  pollInterval = window.setInterval(() => {
    fetchSelectedBlock();
  }, 1000);
}

// Stop polling
function stopPolling() {
  if (pollInterval !== null) {
    clearInterval(pollInterval);
    pollInterval = null;
  }
}

// Lifecycle hooks
onMounted(async () => {
  if (!sessionId.value) {
    error.value = 'No session ID provided in URL';
    return;
  }

  await fetchEditAction();
  startPolling();
});

onUnmounted(() => {
  stopPolling();
});
</script>
