<template>
  <TransitionRoot :show="true" as="template">
    <Dialog as="div" class="relative z-50" @close="$emit('close')">
      <TransitionChild
        as="template"
        enter="ease-out duration-300"
        enter-from="opacity-0"
        enter-to="opacity-100"
        leave="ease-in duration-200"
        leave-from="opacity-100"
        leave-to="opacity-0"
      >
        <div class="fixed inset-0 bg-black bg-opacity-25" />
      </TransitionChild>

      <div class="fixed inset-0 overflow-y-auto">
        <div class="flex min-h-full items-center justify-center p-4">
          <TransitionChild
            as="template"
            enter="ease-out duration-300"
            enter-from="opacity-0 scale-95"
            enter-to="opacity-100 scale-100"
            leave="ease-in duration-200"
            leave-from="opacity-100 scale-100"
            leave-to="opacity-0 scale-95"
          >
            <DialogPanel class="w-full max-w-4xl transform overflow-hidden rounded-2xl bg-base-100 p-6 text-left align-middle shadow-xl transition-all">
              <DialogTitle class="text-2xl font-bold mb-4">
                Create New Job
              </DialogTitle>

              <!-- Form -->
              <form @submit.prevent="handleCreate" class="space-y-4">
                <!-- Executor and Presets -->
                <div class="form-control">
                  <label class="label">
                    <span class="label-text">Quick Presets</span>
                  </label>
                  <div class="flex gap-2 flex-wrap">
                    <button
                      type="button"
                      class="btn btn-sm btn-outline"
                      @click="loadPreset('flat')"
                    >
                      Flat Terrain
                    </button>
                    <button
                      type="button"
                      class="btn btn-sm btn-outline"
                      @click="loadPreset('normal')"
                    >
                      Normal Terrain
                    </button>
                    <button
                      type="button"
                      class="btn btn-sm btn-outline"
                      @click="loadPreset('hilly')"
                    >
                      Hilly Terrain
                    </button>
                  </div>
                </div>

                <!-- Executor -->
                <div class="form-control">
                  <label class="label">
                    <span class="label-text">Executor</span>
                  </label>
                  <input
                    v-model="formData.executor"
                    type="text"
                    class="input input-bordered"
                    placeholder="e.g., flat-world-generator"
                    required
                  />
                </div>

                <!-- Type -->
                <div class="form-control">
                  <label class="label">
                    <span class="label-text">Job Type</span>
                  </label>
                  <input
                    v-model="formData.type"
                    type="text"
                    class="input input-bordered"
                    placeholder="e.g., terrain-generation"
                    required
                  />
                </div>

                <!-- Priority and Retries -->
                <div class="grid grid-cols-2 gap-4">
                  <div class="form-control">
                    <label class="label">
                      <span class="label-text">Priority (1-10)</span>
                    </label>
                    <input
                      v-model.number="formData.priority"
                      type="number"
                      min="1"
                      max="10"
                      class="input input-bordered"
                      required
                    />
                  </div>
                  <div class="form-control">
                    <label class="label">
                      <span class="label-text">Max Retries</span>
                    </label>
                    <input
                      v-model.number="formData.maxRetries"
                      type="number"
                      min="0"
                      max="10"
                      class="input input-bordered"
                      required
                    />
                  </div>
                </div>

                <!-- Parameters -->
                <div class="form-control">
                  <label class="label">
                    <span class="label-text">Parameters</span>
                    <button
                      type="button"
                      class="btn btn-xs btn-outline"
                      @click="addParameter"
                    >
                      <svg class="w-4 h-4 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
                      </svg>
                      Add
                    </button>
                  </label>

                  <div class="overflow-x-auto border border-base-300 rounded">
                    <table class="table table-sm w-full">
                      <thead>
                        <tr>
                          <th class="w-1/3">Key</th>
                          <th class="w-1/2">Value</th>
                          <th class="w-16">Actions</th>
                        </tr>
                      </thead>
                      <tbody>
                        <tr v-for="(param, index) in parameterList" :key="index">
                          <td>
                            <input
                              v-model="param.key"
                              type="text"
                              class="input input-xs input-bordered w-full"
                              placeholder="Parameter name"
                              required
                            />
                          </td>
                          <td>
                            <input
                              v-model="param.value"
                              type="text"
                              class="input input-xs input-bordered w-full"
                              placeholder="Parameter value"
                              required
                            />
                          </td>
                          <td>
                            <button
                              type="button"
                              class="btn btn-xs btn-ghost text-error"
                              @click="removeParameter(index)"
                              title="Remove"
                            >
                              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
                              </svg>
                            </button>
                          </td>
                        </tr>
                        <tr v-if="parameterList.length === 0">
                          <td colspan="3" class="text-center text-base-content/50 py-4">
                            No parameters. Click "Add" or use a preset.
                          </td>
                        </tr>
                      </tbody>
                    </table>
                  </div>
                </div>

                <!-- Error Display -->
                <ErrorAlert v-if="saveError" :message="saveError" />

                <!-- Actions -->
                <div class="mt-6 flex justify-end gap-2">
                  <button
                    type="button"
                    class="btn"
                    @click="$emit('close')"
                    :disabled="saving"
                  >
                    Cancel
                  </button>
                  <button
                    type="submit"
                    class="btn btn-primary"
                    :disabled="saving || !isFormValid"
                  >
                    <span v-if="saving" class="loading loading-spinner loading-sm"></span>
                    {{ saving ? 'Creating...' : 'Create Job' }}
                  </button>
                </div>
              </form>
            </DialogPanel>
          </TransitionChild>
        </div>
      </div>
    </Dialog>
  </TransitionRoot>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import { Dialog, DialogPanel, DialogTitle, TransitionRoot, TransitionChild } from '@headlessui/vue';
import { useJobs, type JobCreateRequest } from '@/composables/useJobs';
import ErrorAlert from '@components/ErrorAlert.vue';

const props = defineProps<{
  worldId: string;
}>();

const emit = defineEmits<{
  close: [];
  created: [];
}>();

const { createJob } = useJobs(props.worldId);

// Form data
const formData = ref({
  executor: '',
  type: 'terrain-generation',
  priority: 5,
  maxRetries: 2,
});

// Parameter list (key-value pairs)
const parameterList = ref<Array<{ key: string; value: string }>>([]);

const saving = ref(false);
const saveError = ref<string | null>(null);

/**
 * Validate form
 */
const isFormValid = computed(() => {
  return (
    formData.value.executor !== '' &&
    formData.value.type.trim() !== '' &&
    parameterList.value.length > 0 &&
    parameterList.value.every(p => p.key.trim() !== '' && p.value.trim() !== '')
  );
});

/**
 * Add new parameter row
 */
const addParameter = () => {
  parameterList.value.push({ key: '', value: '' });
};

/**
 * Remove parameter row
 */
const removeParameter = (index: number) => {
  parameterList.value.splice(index, 1);
};

/**
 * Load preset
 */
const loadPreset = (preset: string) => {
  parameterList.value = [];

  switch (preset) {
    case 'flat':
      formData.value.executor = 'flat-world-generator';
      formData.value.type = 'terrain-generation';
      parameterList.value = [
        { key: 'grid', value: '0:0' },
        { key: 'layer', value: 'terrain' },
        { key: 'groundLevel', value: '64' },
        { key: 'layerCount', value: '2' },
        { key: 'groundBlockTypeId', value: 'w/310' }
      ];
      break;

    case 'normal':
      formData.value.executor = 'normal-world-generator';
      formData.value.type = 'terrain-generation';
      parameterList.value = [
        { key: 'grid', value: '0:0' },
        { key: 'layer', value: 'terrain' },
        { key: 'waterLevel', value: '62' },
        { key: 'baseHeight', value: '64' },
        { key: 'heightVariation', value: '32' },
        { key: 'seed', value: String(Date.now()) },
        { key: 'grassBlockTypeId', value: 'w/310' },
        { key: 'dirtBlockTypeId', value: 'w/279' },
        { key: 'sandBlockTypeId', value: 'w/520' },
        { key: 'waterBlockTypeId', value: 'w/5000' }
      ];
      break;

    case 'hilly':
      formData.value.executor = 'hilly-world-generator';
      formData.value.type = 'terrain-generation';
      parameterList.value = [
        { key: 'grid', value: '0:0' },
        { key: 'layer', value: 'terrain' },
        { key: 'waterLevel', value: '62' },
        { key: 'baseHeight', value: '64' },
        { key: 'hillHeight', value: '64' },
        { key: 'seed', value: String(Date.now()) },
        { key: 'grassBlockTypeId', value: 'w/310' },
        { key: 'dirtBlockTypeId', value: 'w/279' },
        { key: 'sandBlockTypeId', value: 'w/520' },
        { key: 'waterBlockTypeId', value: 'w/5000' }
      ];
      break;
  }
};

/**
 * Build parameters object from list
 */
const buildParameters = (): Record<string, string> => {
  const result: Record<string, string> = {};
  parameterList.value.forEach(param => {
    if (param.key.trim() && param.value.trim()) {
      result[param.key.trim()] = param.value.trim();
    }
  });
  return result;
};

/**
 * Handle create
 */
const handleCreate = async () => {
  if (!isFormValid.value) {
    return;
  }

  saving.value = true;
  saveError.value = null;

  try {
    const request: JobCreateRequest = {
      executor: formData.value.executor,
      type: formData.value.type,
      parameters: buildParameters(),
      priority: formData.value.priority,
      maxRetries: formData.value.maxRetries,
    };

    await createJob(request);
    emit('created');
  } catch (err) {
    saveError.value = `Failed to create job: ${(err as Error).message}`;
  } finally {
    saving.value = false;
  }
};
</script>
