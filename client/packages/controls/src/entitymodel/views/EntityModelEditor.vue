<template>
  <div class="space-y-4">
    <!-- Header -->
    <div class="flex items-center justify-between">
      <button class="btn btn-ghost gap-2" @click="handleBack">
        <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" />
        </svg>
        Back to List
      </button>
      <h2 class="text-2xl font-bold">
        {{ isNew ? 'Create New Entity Model' : 'Edit Entity Model' }}
      </h2>
    </div>

    <!-- Error State -->
    <div v-if="error" class="alert alert-error">
      <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
      </svg>
      <span>{{ error }}</span>
    </div>

    <!-- Loading State -->
    <div v-if="loading" class="flex justify-center py-12">
      <span class="loading loading-spinner loading-lg"></span>
    </div>

    <!-- Edit Form -->
    <div v-else class="space-y-4">
      <!-- Basic Info Card -->
      <div class="card bg-base-100 shadow-xl">
        <div class="card-body">
          <h3 class="card-title">Basic Information</h3>
          <form @submit.prevent="handleSave" class="space-y-4">
            <!-- Model ID -->
            <div class="form-control">
              <label class="label">
                <span class="label-text font-medium">Model ID</span>
              </label>
              <input
                v-model="formData.modelId"
                type="text"
                placeholder="Enter unique model ID"
                class="input input-bordered w-full"
                :disabled="!isNew"
                required
              />
              <label class="label">
                <span class="label-text-alt">Unique identifier for this entity model</span>
              </label>
            </div>

            <!-- Enabled Status -->
            <div v-if="!isNew" class="form-control">
              <label class="label cursor-pointer justify-start gap-4">
                <span class="label-text font-medium">Enabled</span>
                <input
                  v-model="formData.enabled"
                  type="checkbox"
                  class="toggle toggle-success"
                />
              </label>
            </div>

            <!-- Action Buttons -->
            <div class="card-actions justify-end mt-6">
              <button type="button" class="btn btn-ghost" @click="handleBack">
                Cancel
              </button>
              <button type="submit" class="btn btn-primary" :disabled="saving">
                <span v-if="saving" class="loading loading-spinner loading-sm"></span>
                <span v-else>{{ isNew ? 'Create' : 'Save' }}</span>
              </button>
            </div>
          </form>
        </div>
      </div>

      <!-- Public Data Card -->
      <div class="card bg-base-100 shadow-xl">
        <div class="card-body">
          <h3 class="card-title">Entity Model Data (JSON)</h3>
          <textarea
            v-model="publicDataJson"
            class="textarea textarea-bordered font-mono text-sm w-full"
            rows="20"
            placeholder="Enter JSON data for entity model"
          ></textarea>
          <div class="card-actions justify-end mt-4">
            <button
              type="button"
              class="btn btn-sm btn-primary"
              @click="handleSavePublicData"
              :disabled="saving"
            >
              <span v-if="saving" class="loading loading-spinner loading-xs"></span>
              <span v-else>Save Model Data</span>
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- Success Message -->
    <div v-if="successMessage" class="alert alert-success">
      <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
      </svg>
      <span>{{ successMessage }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useWorld } from '@/composables/useWorld';
import { entityModelService, type EntityModelData } from '../services/EntityModelService';

const props = defineProps<{
  entityModel: EntityModelData | 'new';
}>();

const emit = defineEmits<{
  back: [];
  saved: [];
}>();

const { currentWorldId } = useWorld();

const isNew = computed(() => props.entityModel === 'new');

const loading = ref(false);
const saving = ref(false);
const error = ref<string | null>(null);
const successMessage = ref<string | null>(null);

const formData = ref({
  modelId: '',
  enabled: true,
});

const publicDataJson = ref('{}');

const loadEntityModel = () => {
  if (isNew.value) {
    formData.value = {
      modelId: '',
      enabled: true,
    };
    publicDataJson.value = JSON.stringify({ id: '', displayName: '' }, null, 2);
    return;
  }

  const model = props.entityModel as EntityModelData;
  formData.value = {
    modelId: model.modelId,
    enabled: model.enabled,
  };
  publicDataJson.value = JSON.stringify(model.publicData, null, 2);
};

const handleSave = async () => {
  if (!currentWorldId.value) {
    error.value = 'No world selected';
    return;
  }

  saving.value = true;
  error.value = null;
  successMessage.value = null;

  try {
    if (isNew.value) {
      let publicData;
      try {
        publicData = JSON.parse(publicDataJson.value);
      } catch {
        publicData = { id: formData.value.modelId, displayName: formData.value.modelId };
      }

      await entityModelService.createEntityModel(currentWorldId.value, {
        modelId: formData.value.modelId,
        publicData,
      });
      successMessage.value = 'Entity model created successfully';
    } else {
      await entityModelService.updateEntityModel(currentWorldId.value, formData.value.modelId, {
        enabled: formData.value.enabled,
      });
      successMessage.value = 'Entity model updated successfully';
    }

    setTimeout(() => {
      emit('saved');
    }, 1000);
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Failed to save entity model';
    console.error('[EntityModelEditor] Failed to save entity model:', e);
  } finally {
    saving.value = false;
  }
};

const handleSavePublicData = async () => {
  if (!currentWorldId.value) {
    error.value = 'No world selected';
    return;
  }

  saving.value = true;
  error.value = null;
  successMessage.value = null;

  try {
    const publicData = JSON.parse(publicDataJson.value);
    await entityModelService.updateEntityModel(currentWorldId.value, formData.value.modelId, {
      publicData,
    });
    successMessage.value = 'Model data updated successfully';
  } catch (e) {
    if (e instanceof SyntaxError) {
      error.value = 'Invalid JSON format';
    } else {
      error.value = e instanceof Error ? e.message : 'Failed to save model data';
    }
    console.error('[EntityModelEditor] Failed to save model data:', e);
  } finally {
    saving.value = false;
  }
};

const handleBack = () => {
  emit('back');
};

onMounted(() => {
  loadEntityModel();
});
</script>
