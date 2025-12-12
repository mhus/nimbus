<template>
  <div class="space-y-4">
    <div class="flex items-center justify-between">
      <button class="btn btn-ghost gap-2" @click="handleBack">
        <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" />
        </svg>
        Back to List
      </button>
      <h2 class="text-2xl font-bold">
        {{ isNew ? 'Create New Backdrop' : 'Edit Backdrop' }}
      </h2>
    </div>

    <div v-if="error" class="alert alert-error">
      <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
      </svg>
      <span>{{ error }}</span>
    </div>

    <div class="space-y-4">
      <div class="card bg-base-100 shadow-xl">
        <div class="card-body">
          <h3 class="card-title">Basic Information</h3>
          <form @submit.prevent="handleSave" class="space-y-4">
            <div class="form-control">
              <label class="label">
                <span class="label-text font-medium">Backdrop ID</span>
              </label>
              <input
                v-model="formData.backdropId"
                type="text"
                placeholder="Enter unique backdrop ID"
                class="input input-bordered w-full"
                :disabled="!isNew"
                required
              />
            </div>

            <div v-if="!isNew" class="form-control">
              <label class="label cursor-pointer justify-start gap-4">
                <span class="label-text font-medium">Enabled</span>
                <input v-model="formData.enabled" type="checkbox" class="toggle toggle-success" />
              </label>
            </div>

            <div class="card-actions justify-end mt-6">
              <button type="button" class="btn btn-ghost" @click="handleBack">Cancel</button>
              <button type="submit" class="btn btn-primary" :disabled="saving">
                <span v-if="saving" class="loading loading-spinner loading-sm"></span>
                <span v-else>{{ isNew ? 'Create' : 'Save' }}</span>
              </button>
            </div>
          </form>
        </div>
      </div>

      <div class="card bg-base-100 shadow-xl">
        <div class="card-body">
          <h3 class="card-title">Backdrop Data (JSON)</h3>
          <textarea
            v-model="publicDataJson"
            class="textarea textarea-bordered font-mono text-sm w-full"
            rows="15"
            placeholder="Enter JSON data for backdrop"
          ></textarea>
          <div class="card-actions justify-end mt-4">
            <button type="button" class="btn btn-sm btn-primary" @click="handleSavePublicData" :disabled="saving">
              <span v-if="saving" class="loading loading-spinner loading-xs"></span>
              <span v-else>Save Backdrop Data</span>
            </button>
          </div>
        </div>
      </div>
    </div>

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
import { backdropService, type BackdropData } from '../services/BackdropService';

const props = defineProps<{ backdrop: BackdropData | 'new' }>();
const emit = defineEmits<{ back: []; saved: [] }>();
const { currentWorldId } = useWorld();
const isNew = computed(() => props.backdrop === 'new');

const saving = ref(false);
const error = ref<string | null>(null);
const successMessage = ref<string | null>(null);
const formData = ref({ backdropId: '', enabled: true });
const publicDataJson = ref('{}');

const loadBackdrop = () => {
  if (isNew.value) {
    formData.value = { backdropId: '', enabled: true };
    publicDataJson.value = JSON.stringify({ id: '' }, null, 2);
    return;
  }

  const backdrop = props.backdrop as BackdropData;
  formData.value = { backdropId: backdrop.backdropId, enabled: backdrop.enabled };
  publicDataJson.value = JSON.stringify(backdrop.publicData, null, 2);
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
      const publicData = JSON.parse(publicDataJson.value);
      await backdropService.createBackdrop(currentWorldId.value, {
        backdropId: formData.value.backdropId,
        publicData,
      });
      successMessage.value = 'Backdrop created successfully';
    } else {
      await backdropService.updateBackdrop(currentWorldId.value, formData.value.backdropId, {
        enabled: formData.value.enabled,
      });
      successMessage.value = 'Backdrop updated successfully';
    }

    setTimeout(() => emit('saved'), 1000);
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Failed to save backdrop';
  } finally {
    saving.value = false;
  }
};

const handleSavePublicData = async () => {
  if (!currentWorldId.value) return;

  saving.value = true;
  error.value = null;
  successMessage.value = null;

  try {
    const publicData = JSON.parse(publicDataJson.value);
    await backdropService.updateBackdrop(currentWorldId.value, formData.value.backdropId, { publicData });
    successMessage.value = 'Backdrop data updated successfully';
  } catch (e) {
    error.value = e instanceof SyntaxError ? 'Invalid JSON format' : (e instanceof Error ? e.message : 'Failed to save');
  } finally {
    saving.value = false;
  }
};

const handleBack = () => emit('back');

onMounted(() => loadBackdrop());
</script>
