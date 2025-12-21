<template>
  <div class="modal modal-open" @click.self="emit('close')">
    <div class="modal-box max-w-4xl" @click.stop>
      <h3 class="font-bold text-lg mb-4">
        {{ isEditMode ? 'Edit Layer Model' : 'Create Layer Model' }}
      </h3>

      <!-- Error Alert -->
      <ErrorAlert v-if="errorMessage" :message="errorMessage" class="mb-4" />

      <form @submit.prevent="handleSave" class="space-y-4">
        <!-- Name -->
        <div class="form-control">
          <label class="label">
            <span class="label-text">Name</span>
          </label>
          <input
            v-model="formData.name"
            type="text"
            class="input input-bordered"
            placeholder="Technical name (optional)"
          />
          <label class="label">
            <span class="label-text-alt">Technical identifier for this model</span>
          </label>
        </div>

        <!-- Title -->
        <div class="form-control">
          <label class="label">
            <span class="label-text">Title</span>
          </label>
          <input
            v-model="formData.title"
            type="text"
            class="input input-bordered"
            placeholder="Display title (optional)"
          />
          <label class="label">
            <span class="label-text-alt">Human-readable display name</span>
          </label>
        </div>

        <!-- Mount Point -->
        <div class="form-control">
          <label class="label">
            <span class="label-text">Mount Point *</span>
          </label>
          <div class="grid grid-cols-3 gap-4">
            <div>
              <input
                v-model.number="formData.mountX"
                type="number"
                class="input input-bordered w-full"
                placeholder="X"
                required
              />
            </div>
            <div>
              <input
                v-model.number="formData.mountY"
                type="number"
                class="input input-bordered w-full"
                placeholder="Y"
                required
              />
            </div>
            <div>
              <input
                v-model.number="formData.mountZ"
                type="number"
                class="input input-bordered w-full"
                placeholder="Z"
                required
              />
            </div>
          </div>
          <label class="label">
            <span class="label-text-alt">World coordinates where this model will be placed</span>
          </label>
        </div>

        <!-- Rotation -->
        <div class="form-control">
          <label class="label">
            <span class="label-text">Rotation</span>
          </label>
          <select
            v-model.number="formData.rotation"
            class="select select-bordered"
          >
            <option :value="0">0° (No rotation)</option>
            <option :value="1">90° (Clockwise)</option>
            <option :value="2">180°</option>
            <option :value="3">270° (Counter-clockwise)</option>
          </select>
          <label class="label">
            <span class="label-text-alt">Rotation in 90 degree steps</span>
          </label>
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
            placeholder="Render order"
            required
          />
          <label class="label">
            <span class="label-text-alt">Lower values are rendered first (bottom), higher values on top</span>
          </label>
        </div>

        <!-- Reference Model ID -->
        <div class="form-control">
          <label class="label">
            <span class="label-text">Reference Model ID</span>
          </label>
          <input
            v-model="formData.referenceModelId"
            type="text"
            class="input input-bordered"
            placeholder="Optional reference to another model"
          />
          <label class="label">
            <span class="label-text-alt">If set, the referenced model will be rendered first, then this model on top</span>
          </label>
        </div>

        <!-- Groups -->
        <div class="form-control">
          <label class="label">
            <span class="label-text">Groups</span>
          </label>
          <div class="space-y-2">
            <div v-for="(groupId, groupName) in formData.groups" :key="groupName" class="flex gap-2">
              <input
                :value="groupName"
                type="text"
                class="input input-bordered flex-1"
                placeholder="Group name"
                @input="updateGroupName(groupName as string, ($event.target as HTMLInputElement).value)"
              />
              <input
                :value="groupId"
                type="number"
                class="input input-bordered w-24"
                placeholder="ID"
                @input="updateGroupId(groupName as string, parseInt(($event.target as HTMLInputElement).value))"
              />
              <button
                type="button"
                class="btn btn-ghost btn-square"
                @click="removeGroup(groupName as string)"
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
            <span class="label-text-alt">Group mapping: name → ID for organized block management</span>
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
            v-if="isEditMode"
            type="button"
            class="btn btn-secondary"
            :disabled="syncing"
            @click="handleSync"
          >
            <span v-if="syncing" class="loading loading-spinner"></span>
            <svg v-else class="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
            </svg>
            {{ syncing ? 'Syncing...' : 'Sync to Terrain' }}
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
import type { LayerModelDto, CreateLayerModelRequest, UpdateLayerModelRequest } from '@nimbus/shared';
import ErrorAlert from '@components/ErrorAlert.vue';
import { layerModelService } from '@/services/LayerModelService';
import { getLogger } from '@nimbus/shared';

const logger = getLogger('ModelEditorPanel');

interface Props {
  model: LayerModelDto | null;
  layerDataId: string;
  worldId: string;
  layerId: string;
}

const props = defineProps<Props>();

const emit = defineEmits<{
  (e: 'close'): void;
  (e: 'saved', model: LayerModelDto): void;
}>();

const isEditMode = computed(() => !!props.model);

const formData = ref<Partial<LayerModelDto>>({
  name: '',
  title: '',
  mountX: 0,
  mountY: 0,
  mountZ: 0,
  rotation: 0,
  referenceModelId: undefined,
  order: 100,
  groups: {}
});

const errorMessage = ref('');
const saving = ref(false);
const syncing = ref(false);

// Initialize form data
if (props.model) {
  formData.value = { ...props.model };
}

/**
 * Add a new group
 */
const addGroup = () => {
  const groups = formData.value.groups || {};
  const maxId = Math.max(0, ...Object.values(groups).map(v => typeof v === 'number' ? v : 0));
  const newId = maxId + 1;
  formData.value.groups = {
    ...groups,
    [`group${newId}`]: newId
  };
};

/**
 * Remove a group
 */
const removeGroup = (groupName: string) => {
  const groups = { ...formData.value.groups };
  delete groups[groupName];
  formData.value.groups = groups;
};

/**
 * Update group name
 */
const updateGroupName = (oldName: string, newName: string) => {
  if (oldName === newName) return;
  const groups = { ...formData.value.groups };
  const groupId = groups[oldName];
  delete groups[oldName];
  groups[newName] = groupId;
  formData.value.groups = groups;
};

/**
 * Update group ID
 */
const updateGroupId = (groupName: string, newId: number) => {
  formData.value.groups = {
    ...formData.value.groups,
    [groupName]: newId
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
    if (formData.value.mountX === undefined) {
      throw new Error('Mount X is required');
    }
    if (formData.value.mountY === undefined) {
      throw new Error('Mount Y is required');
    }
    if (formData.value.mountZ === undefined) {
      throw new Error('Mount Z is required');
    }
    if (formData.value.order === undefined) {
      throw new Error('Order is required');
    }

    if (isEditMode.value && props.model?.id) {
      // Update existing model
      const updateData: UpdateLayerModelRequest = {
        name: formData.value.name || undefined,
        title: formData.value.title || undefined,
        mountX: formData.value.mountX,
        mountY: formData.value.mountY,
        mountZ: formData.value.mountZ,
        rotation: formData.value.rotation,
        referenceModelId: formData.value.referenceModelId || undefined,
        order: formData.value.order,
        groups: formData.value.groups
      };
      await layerModelService.updateModel(props.worldId, props.layerId, props.model.id, updateData);
      logger.info('Updated model', { modelId: props.model.id });
    } else {
      // Create new model
      const createData: CreateLayerModelRequest = {
        name: formData.value.name || undefined,
        title: formData.value.title || undefined,
        layerDataId: props.layerDataId,
        mountX: formData.value.mountX,
        mountY: formData.value.mountY,
        mountZ: formData.value.mountZ,
        rotation: formData.value.rotation,
        referenceModelId: formData.value.referenceModelId || undefined,
        order: formData.value.order,
        groups: formData.value.groups
      };
      const id = await layerModelService.createModel(props.worldId, props.layerId, createData);
      logger.info('Created model', { modelId: id });
    }

    emit('saved', formData.value as LayerModelDto);
  } catch (error: any) {
    logger.error('Failed to save model', {}, error);
    errorMessage.value = error.message || 'Failed to save model';
  } finally {
    saving.value = false;
  }
};

/**
 * Handle manual sync to terrain
 */
const handleSync = async () => {
  if (!props.model?.id) return;

  errorMessage.value = '';
  syncing.value = true;

  try {
    await layerModelService.syncToTerrain(props.worldId, props.layerId, props.model.id);
    logger.info('Synced model to terrain', { modelId: props.model.id });

    // Show success message (optional)
    alert('Model synced to terrain successfully!');
  } catch (error: any) {
    logger.error('Failed to sync model', {}, error);
    errorMessage.value = error.message || 'Failed to sync model to terrain';
  } finally {
    syncing.value = false;
  }
};
</script>
