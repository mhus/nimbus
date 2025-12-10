<template>
  <div class="modal modal-open">
    <div class="modal-box max-w-4xl">
      <h3 class="font-bold text-lg mb-4">
        {{ isEditMode ? 'Edit Layer' : 'Create Layer' }}
      </h3>

      <!-- Error Alert -->
      <ErrorAlert v-if="errorMessage" :message="errorMessage" class="mb-4" />

      <form @submit.prevent="handleSave" class="space-y-4">
        <!-- Name -->
        <div class="form-control">
          <label class="label">
            <span class="label-text">Name *</span>
          </label>
          <input
            v-model="formData.name"
            type="text"
            class="input input-bordered"
            placeholder="Enter layer name"
            required
          />
        </div>

        <!-- Layer Type -->
        <div class="form-control">
          <label class="label">
            <span class="label-text">Layer Type *</span>
          </label>
          <select
            v-model="formData.layerType"
            class="select select-bordered"
            :disabled="isEditMode"
            required
          >
            <option value="">Select type</option>
            <option value="TERRAIN">Terrain</option>
            <option value="MODEL">Model</option>
          </select>
          <label class="label">
            <span class="label-text-alt text-warning">Layer type cannot be changed after creation</span>
          </label>
        </div>

        <!-- Mount Point (only for MODEL layers) -->
        <div v-if="formData.layerType === 'MODEL'" class="grid grid-cols-3 gap-4">
          <div class="form-control">
            <label class="label">
              <span class="label-text">Mount X</span>
            </label>
            <input
              v-model.number="formData.mountX"
              type="number"
              class="input input-bordered"
              placeholder="X"
            />
          </div>
          <div class="form-control">
            <label class="label">
              <span class="label-text">Mount Y</span>
            </label>
            <input
              v-model.number="formData.mountY"
              type="number"
              class="input input-bordered"
              placeholder="Y"
            />
          </div>
          <div class="form-control">
            <label class="label">
              <span class="label-text">Mount Z</span>
            </label>
            <input
              v-model.number="formData.mountZ"
              type="number"
              class="input input-bordered"
              placeholder="Z"
            />
          </div>
        </div>

        <!-- Order -->
        <div class="form-control">
          <label class="label">
            <span class="label-text">Order *</span>
          </label>
          <input
            v-model.number="formData.order"
            type="number"
            class="input input-bordered"
            placeholder="Layer order (lower values render first)"
            required
          />
          <label class="label">
            <span class="label-text-alt">Lower values are rendered first (bottom), higher values on top</span>
          </label>
        </div>

        <!-- Ground Level -->
        <div class="form-control">
          <label class="label cursor-pointer">
            <span class="label-text">Ground Level</span>
            <input
              v-model="formData.ground"
              type="checkbox"
              class="checkbox checkbox-primary"
            />
          </label>
          <label class="label">
            <span class="label-text-alt">If true, this layer defines ground level (affects terrain generation)</span>
          </label>
        </div>

        <!-- Enabled -->
        <div class="form-control">
          <label class="label cursor-pointer">
            <span class="label-text">Enabled</span>
            <input
              v-model="formData.enabled"
              type="checkbox"
              class="checkbox checkbox-primary"
            />
          </label>
          <label class="label">
            <span class="label-text-alt">Layer enabled flag (soft delete)</span>
          </label>
        </div>

        <!-- All Chunks -->
        <div class="form-control">
          <label class="label cursor-pointer">
            <span class="label-text">Affects All Chunks</span>
            <input
              v-model="formData.allChunks"
              type="checkbox"
              class="checkbox checkbox-primary"
            />
          </label>
          <label class="label">
            <span class="label-text-alt">If true, this layer affects all chunks in the world</span>
          </label>
        </div>

        <!-- Affected Chunks (only if not allChunks) -->
        <div v-if="!formData.allChunks" class="form-control">
          <label class="label">
            <span class="label-text">Affected Chunks</span>
          </label>
          <textarea
            v-model="affectedChunksText"
            class="textarea textarea-bordered"
            placeholder="Enter chunk keys (format: cx:cz), one per line. Example: 0:0"
            rows="4"
          ></textarea>
          <label class="label">
            <span class="label-text-alt">List of chunk keys affected by this layer</span>
          </label>
        </div>

        <!-- Groups -->
        <div class="form-control">
          <label class="label">
            <span class="label-text">Groups</span>
          </label>
          <div class="space-y-2">
            <div v-for="(groupName, groupId) in formData.groups" :key="groupId" class="flex gap-2">
              <input
                :value="groupId"
                type="number"
                class="input input-bordered w-24"
                placeholder="ID"
                disabled
              />
              <input
                :value="groupName"
                type="text"
                class="input input-bordered flex-1"
                placeholder="Group name"
                @input="updateGroupName(parseInt(groupId as string), ($event.target as HTMLInputElement).value)"
              />
              <button
                type="button"
                class="btn btn-ghost btn-square"
                @click="removeGroup(parseInt(groupId as string))"
              >
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
                </svg>
              </button>
            </div>
            <button
              type="button"
              class="btn btn-sm btn-outline"
              @click="addGroup"
            >
              <svg class="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
              </svg>
              Add Group
            </button>
          </div>
          <label class="label">
            <span class="label-text-alt">Group names defined in this layer for organized block management</span>
          </label>
        </div>

        <!-- Action Buttons -->
        <div class="modal-action">
          <button
            type="button"
            class="btn"
            @click="emit('close')"
          >
            Cancel
          </button>
          <button
            type="submit"
            class="btn btn-primary"
            :disabled="saving"
          >
            <span v-if="saving" class="loading loading-spinner"></span>
            {{ saving ? 'Saving...' : 'Save' }}
          </button>
        </div>
      </form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue';
import type { WLayer } from '@nimbus/shared';
import ErrorAlert from '@components/ErrorAlert.vue';

interface Props {
  layer: WLayer | null;
  worldId: string;
}

const props = defineProps<Props>();

const emit = defineEmits<{
  (e: 'close'): void;
  (e: 'saved', layer: WLayer): void;
}>();

const isEditMode = computed(() => !!props.layer);

const formData = ref<Partial<WLayer>>({
  name: '',
  layerType: undefined,
  mountX: undefined,
  mountY: undefined,
  mountZ: undefined,
  order: 0,
  ground: false,
  enabled: true,
  allChunks: true,
  affectedChunks: [],
  groups: {}
});

const affectedChunksText = ref('');
const errorMessage = ref('');
const saving = ref(false);

// Initialize form data
if (props.layer) {
  formData.value = {
    ...props.layer,
    groups: { ...props.layer.groups }
  };
  affectedChunksText.value = (props.layer.affectedChunks || []).join('\n');
}

// Watch affectedChunksText and update formData
watch(affectedChunksText, (newValue) => {
  formData.value.affectedChunks = newValue
    .split('\n')
    .map(line => line.trim())
    .filter(line => line.length > 0);
});

/**
 * Add a new group
 */
const addGroup = () => {
  const groups = formData.value.groups || {};
  const maxId = Math.max(0, ...Object.keys(groups).map(k => parseInt(k)));
  const newId = maxId + 1;
  formData.value.groups = {
    ...groups,
    [newId]: `Group ${newId}`
  };
};

/**
 * Remove a group
 */
const removeGroup = (groupId: number) => {
  const groups = { ...formData.value.groups };
  delete groups[groupId];
  formData.value.groups = groups;
};

/**
 * Update group name
 */
const updateGroupName = (groupId: number, newName: string) => {
  formData.value.groups = {
    ...formData.value.groups,
    [groupId]: newName
  };
};

/**
 * Handle save
 */
const handleSave = async () => {
  errorMessage.value = '';
  saving.value = true;

  try {
    // Validate
    if (!formData.value.name?.trim()) {
      throw new Error('Name is required');
    }
    if (!formData.value.layerType) {
      throw new Error('Layer type is required');
    }
    if (formData.value.order === undefined) {
      throw new Error('Order is required');
    }

    // Prepare data
    const layerData: Partial<WLayer> = {
      ...formData.value,
      worldId: props.worldId,
      name: formData.value.name.trim()
    };

    // TODO: Call API to save layer
    // For now, just emit the saved event
    console.log('Saving layer:', layerData);

    emit('saved', layerData as WLayer);
  } catch (error: any) {
    errorMessage.value = error.message || 'Failed to save layer';
  } finally {
    saving.value = false;
  }
};
</script>
