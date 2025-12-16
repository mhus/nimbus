<template>
  <div class="modal modal-open">
    <div class="modal-box max-w-3xl">
      <h3 class="font-bold text-lg mb-4">Create New Job</h3>

      <!-- Form -->
      <form @submit.prevent="handleCreate" class="space-y-4">
        <!-- Executor -->
        <div class="form-control">
          <label class="label">
            <span class="label-text">Executor</span>
          </label>
          <select
            v-model="formData.executor"
            class="select select-bordered"
            required
          >
            <option value="">Select executor...</option>
            <option value="flat-world-generator">Flat World Generator</option>
            <option value="normal-world-generator">Normal World Generator</option>
            <option value="hilly-world-generator">Hilly World Generator</option>
          </select>
          <label class="label">
            <span class="label-text-alt">The job executor to run</span>
          </label>
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
          <label class="label">
            <span class="label-text-alt">Category or type of the job</span>
          </label>
        </div>

        <!-- Priority -->
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
          <label class="label">
            <span class="label-text-alt">Higher priority jobs are processed first</span>
          </label>
        </div>

        <!-- Max Retries -->
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
          <label class="label">
            <span class="label-text-alt">Number of times to retry on failure</span>
          </label>
        </div>

        <!-- Parameters -->
        <div class="form-control">
          <label class="label">
            <span class="label-text">Parameters (JSON)</span>
          </label>
          <textarea
            v-model="parametersJson"
            class="textarea textarea-bordered font-mono text-sm"
            rows="10"
            placeholder='{"grid": "0:0", "layer": "terrain", "seed": "12345", ...}'
            required
          />
          <label v-if="parametersError" class="label">
            <span class="label-text-alt text-error">{{ parametersError }}</span>
          </label>
          <label class="label">
            <span class="label-text-alt">Job parameters as JSON object (all values must be strings)</span>
          </label>
        </div>

        <!-- Presets -->
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

        <!-- Error Display -->
        <ErrorAlert v-if="saveError" :message="saveError" />

        <!-- Actions -->
        <div class="modal-action">
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
    </div>
    <div class="modal-backdrop" @click="$emit('close')"></div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
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

const parametersJson = ref('');
const parametersError = ref<string | null>(null);
const saving = ref(false);
const saveError = ref<string | null>(null);

/**
 * Validate form
 */
const isFormValid = computed(() => {
  return (
    formData.value.executor !== '' &&
    formData.value.type.trim() !== '' &&
    !parametersError.value &&
    parametersJson.value.trim() !== ''
  );
});

/**
 * Parse parameters JSON
 */
const parseParameters = (): Record<string, string> | null => {
  parametersError.value = null;

  if (!parametersJson.value.trim()) {
    parametersError.value = 'Parameters are required';
    return null;
  }

  try {
    const parsed = JSON.parse(parametersJson.value);

    if (typeof parsed !== 'object' || parsed === null || Array.isArray(parsed)) {
      parametersError.value = 'Parameters must be a JSON object';
      return null;
    }

    // Convert all values to strings
    const result: Record<string, string> = {};
    for (const [key, value] of Object.entries(parsed)) {
      result[key] = String(value);
    }

    return result;
  } catch (err) {
    parametersError.value = `Invalid JSON: ${(err as Error).message}`;
    return null;
  }
};

/**
 * Load preset
 */
const loadPreset = (preset: string) => {
  switch (preset) {
    case 'flat':
      formData.value.executor = 'flat-world-generator';
      formData.value.type = 'terrain-generation';
      parametersJson.value = JSON.stringify({
        grid: '0:0',
        layer: 'terrain',
        groundLevel: '64',
        layerCount: '2',
        groundBlockTypeId: 'w/310'
      }, null, 2);
      break;

    case 'normal':
      formData.value.executor = 'normal-world-generator';
      formData.value.type = 'terrain-generation';
      parametersJson.value = JSON.stringify({
        grid: '0:0',
        layer: 'terrain',
        waterLevel: '62',
        baseHeight: '64',
        heightVariation: '32',
        seed: String(Date.now()),
        grassBlockTypeId: 'w/310',
        dirtBlockTypeId: 'w/279',
        sandBlockTypeId: 'w/520',
        waterBlockTypeId: 'w/5000'
      }, null, 2);
      break;

    case 'hilly':
      formData.value.executor = 'hilly-world-generator';
      formData.value.type = 'terrain-generation';
      parametersJson.value = JSON.stringify({
        grid: '0:0',
        layer: 'terrain',
        waterLevel: '62',
        baseHeight: '64',
        hillHeight: '64',
        seed: String(Date.now()),
        grassBlockTypeId: 'w/310',
        dirtBlockTypeId: 'w/279',
        sandBlockTypeId: 'w/520',
        waterBlockTypeId: 'w/5000'
      }, null, 2);
      break;
  }
};

/**
 * Handle create
 */
const handleCreate = async () => {
  if (!isFormValid.value) {
    return;
  }

  // Parse and validate parameters
  const parameters = parseParameters();
  if (!parameters) {
    return; // Error is already set
  }

  saving.value = true;
  saveError.value = null;

  try {
    const request: JobCreateRequest = {
      executor: formData.value.executor,
      type: formData.value.type,
      parameters,
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
