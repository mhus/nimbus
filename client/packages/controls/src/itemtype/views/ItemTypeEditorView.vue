<template>
  <div class="p-6 max-w-4xl mx-auto">
    <!-- Loading State -->
    <div v-if="loading" class="flex justify-center py-8">
      <span class="loading loading-spinner loading-lg"></span>
    </div>

    <!-- Error State -->
    <div v-else-if="error" class="alert alert-error mb-4">
      <svg xmlns="http://www.w3.org/2000/svg" class="stroke-current shrink-0 h-6 w-6" fill="none" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 14l2-2m0 0l2-2m-2 2l-2-2m2 2l2 2m7-2a9 9 0 11-18 0 9 9 0 0118 0z" />
      </svg>
      <span>{{ error }}</span>
    </div>

    <!-- Editor Form -->
    <div v-else class="space-y-6">
      <!-- Header -->
      <div class="flex justify-between items-center">
        <h2 class="text-2xl font-bold">
          {{ isNew ? 'New ItemType' : `Edit ItemType: ${itemType.name}` }}
        </h2>
      </div>

      <!-- Basic Properties -->
      <div class="card bg-base-200">
        <div class="card-body">
          <h3 class="card-title text-lg">Basic Properties</h3>

          <!-- ID (Type) -->
          <div class="form-control">
            <label class="label">
              <span class="label-text">ID (Type) *</span>
            </label>
            <input
              v-model="itemType.type"
              type="text"
              placeholder="e.g., sword, wand, potion"
              class="input input-bordered"
              :disabled="!isNew"
              required
            />
            <label class="label">
              <span class="label-text-alt text-warning">
                {{ isNew ? 'Alphanumeric and underscore only' : 'Cannot be changed' }}
              </span>
            </label>
          </div>

          <!-- Name -->
          <div class="form-control">
            <label class="label">
              <span class="label-text">Display Name *</span>
            </label>
            <input
              v-model="itemType.name"
              type="text"
              placeholder="e.g., Magic Sword"
              class="input input-bordered"
              required
            />
          </div>

          <!-- Description -->
          <div class="form-control">
            <label class="label">
              <span class="label-text">Description</span>
            </label>
            <textarea
              v-model="itemType.description"
              placeholder="Optional description..."
              class="textarea textarea-bordered h-24"
            />
          </div>
        </div>
      </div>

      <!-- Modifier Properties -->
      <div class="card bg-base-200">
        <div class="card-body">
          <h3 class="card-title text-lg">Visual Modifier</h3>

          <!-- Texture -->
          <div class="form-control">
            <label class="label">
              <span class="label-text">Texture Path</span>
            </label>
            <input
              v-model="itemType.modifier.texture"
              type="text"
              placeholder="e.g., textures/items/sword.png"
              class="input input-bordered"
            />
          </div>

          <!-- Scaling -->
          <div class="grid grid-cols-2 gap-4">
            <div class="form-control">
              <label class="label">
                <span class="label-text">Scale X</span>
              </label>
              <input
                v-model.number="itemType.modifier.scaleX"
                type="number"
                step="0.1"
                min="0"
                class="input input-bordered"
              />
            </div>

            <div class="form-control">
              <label class="label">
                <span class="label-text">Scale Y</span>
              </label>
              <input
                v-model.number="itemType.modifier.scaleY"
                type="number"
                step="0.1"
                min="0"
                class="input input-bordered"
              />
            </div>
          </div>

          <!-- Offset -->
          <div class="form-control">
            <label class="label">
              <span class="label-text">Offset [x, y, z]</span>
            </label>
            <div class="grid grid-cols-3 gap-2">
              <input
                v-model.number="itemType.modifier.offset[0]"
                type="number"
                step="0.1"
                placeholder="X"
                class="input input-bordered input-sm"
              />
              <input
                v-model.number="itemType.modifier.offset[1]"
                type="number"
                step="0.1"
                placeholder="Y"
                class="input input-bordered input-sm"
              />
              <input
                v-model.number="itemType.modifier.offset[2]"
                type="number"
                step="0.1"
                placeholder="Z"
                class="input input-bordered input-sm"
              />
            </div>
          </div>

          <!-- Color -->
          <div class="form-control">
            <label class="label">
              <span class="label-text">Color Tint (Hex)</span>
            </label>
            <input
              v-model="itemType.modifier.color"
              type="text"
              placeholder="e.g., #ff0000"
              class="input input-bordered"
            />
          </div>

          <!-- Pose -->
          <div class="form-control">
            <label class="label">
              <span class="label-text">Pose</span>
            </label>
            <input
              v-model="itemType.modifier.pose"
              type="text"
              placeholder="e.g., idle, attack"
              class="input input-bordered"
            />
          </div>

          <!-- Exclusive Flag -->
          <div class="form-control">
            <label class="label cursor-pointer">
              <span class="label-text">Exclusive (single block occupancy)</span>
              <input
                v-model="itemType.modifier.exclusive"
                type="checkbox"
                class="checkbox"
              />
            </label>
          </div>
        </div>
      </div>

      <!-- Parameters -->
      <div class="card bg-base-200">
        <div class="card-body">
          <h3 class="card-title text-lg">Custom Parameters (JSON)</h3>

          <div class="form-control">
            <textarea
              v-model="parametersJson"
              placeholder='{"key": "value"}'
              class="textarea textarea-bordered font-mono h-32"
              @blur="validateParameters"
            />
            <label v-if="parametersError" class="label">
              <span class="label-text-alt text-error">{{ parametersError }}</span>
            </label>
          </div>
        </div>
      </div>

      <!-- Action Buttons -->
      <div class="flex gap-2 justify-end">
        <button
          v-if="!isNew"
          class="btn btn-error"
          @click="handleDelete"
          :disabled="saving"
        >
          <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
          </svg>
          Delete
        </button>

        <button
          class="btn btn-ghost"
          @click="handleCancel"
          :disabled="saving"
        >
          Cancel
        </button>

        <button
          class="btn btn-primary"
          @click="handleSave"
          :disabled="saving || !isValid"
        >
          <span v-if="saving" class="loading loading-spinner"></span>
          <svg v-else xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
          </svg>
          Save
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue';
import type { ItemType } from '@nimbus/shared';
import {
  getItemType,
  createItemType,
  updateItemType,
  deleteItemType,
} from '../services/itemTypeApiService';

const props = defineProps<{
  itemTypeId: string;
  isNew: boolean;
}>();

const emit = defineEmits<{
  save: [];
  close: [];
  delete: [];
}>();

const itemType = ref<ItemType>({
  type: '',
  name: '',
  description: '',
  modifier: {
    texture: '',
    scaleX: 0.5,
    scaleY: 0.5,
    offset: [0, 0, 0],
    color: '',
    pose: '',
    exclusive: false,
  },
  parameters: {},
});

const parametersJson = ref('{}');
const parametersError = ref<string | null>(null);
const loading = ref(false);
const saving = ref(false);
const error = ref<string | null>(null);

const isValid = computed(() => {
  return (
    itemType.value.type.length > 0 &&
    itemType.value.name.length > 0 &&
    /^[a-zA-Z0-9_]+$/.test(itemType.value.type) &&
    !parametersError.value
  );
});

function validateParameters() {
  try {
    if (parametersJson.value.trim()) {
      const parsed = JSON.parse(parametersJson.value);
      itemType.value.parameters = parsed;
      parametersError.value = null;
    } else {
      itemType.value.parameters = {};
      parametersError.value = null;
    }
  } catch (err) {
    parametersError.value = 'Invalid JSON';
  }
}

async function loadItemType() {
  if (props.isNew) {
    // Reset for new ItemType
    itemType.value = {
      type: '',
      name: '',
      description: '',
      modifier: {
        texture: '',
        scaleX: 0.5,
        scaleY: 0.5,
        offset: [0, 0, 0],
        color: '',
        pose: '',
        exclusive: false,
      },
      parameters: {},
    };
    parametersJson.value = '{}';
    return;
  }

  loading.value = true;
  error.value = null;

  try {
    const data = await getItemType(props.itemTypeId);
    itemType.value = data;
    parametersJson.value = JSON.stringify(data.parameters || {}, null, 2);
  } catch (err) {
    error.value = (err as Error).message;
    console.error('Failed to load ItemType:', err);
  } finally {
    loading.value = false;
  }
}

async function handleSave() {
  if (!isValid.value) return;

  // Validate parameters one more time
  validateParameters();
  if (parametersError.value) return;

  saving.value = true;
  error.value = null;

  try {
    if (props.isNew) {
      await createItemType(itemType.value);
    } else {
      await updateItemType(props.itemTypeId, itemType.value);
    }

    emit('save');
  } catch (err) {
    error.value = (err as Error).message;
    console.error('Failed to save ItemType:', err);
  } finally {
    saving.value = false;
  }
}

async function handleDelete() {
  if (!confirm(`Are you sure you want to delete ItemType "${itemType.value.name}"?`)) {
    return;
  }

  saving.value = true;
  error.value = null;

  try {
    await deleteItemType(props.itemTypeId);
    emit('delete');
  } catch (err) {
    error.value = (err as Error).message;
    console.error('Failed to delete ItemType:', err);
  } finally {
    saving.value = false;
  }
}

function handleCancel() {
  emit('close');
}

// Watch for itemTypeId changes
watch(() => props.itemTypeId, () => {
  loadItemType();
});

onMounted(() => {
  loadItemType();
});
</script>
