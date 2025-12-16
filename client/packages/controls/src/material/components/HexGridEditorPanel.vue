<template>
  <div class="modal modal-open">
    <div class="modal-box max-w-3xl">
      <h3 class="font-bold text-lg mb-4">
        {{ isEditMode ? 'Edit Hex Grid' : 'Create Hex Grid' }}
      </h3>

      <!-- Form -->
      <form @submit.prevent="handleSave" class="space-y-4">
        <!-- Position -->
        <div class="grid grid-cols-2 gap-4">
          <div class="form-control">
            <label class="label">
              <span class="label-text">Position Q</span>
            </label>
            <input
              v-model.number="formData.position.q"
              type="number"
              class="input input-bordered"
              :disabled="isEditMode"
              required
            />
          </div>
          <div class="form-control">
            <label class="label">
              <span class="label-text">Position R</span>
            </label>
            <input
              v-model.number="formData.position.r"
              type="number"
              class="input input-bordered"
              :disabled="isEditMode"
              required
            />
          </div>
        </div>

        <!-- Name -->
        <div class="form-control">
          <label class="label">
            <span class="label-text">Name</span>
          </label>
          <input
            v-model="formData.name"
            type="text"
            class="input input-bordered"
            placeholder="Enter hex grid name"
            required
          />
        </div>

        <!-- Description -->
        <div class="form-control">
          <label class="label">
            <span class="label-text">Description</span>
          </label>
          <textarea
            v-model="formData.description"
            class="textarea textarea-bordered"
            rows="3"
            placeholder="Enter description"
            required
          />
        </div>

        <!-- Icon -->
        <div class="form-control">
          <label class="label">
            <span class="label-text">Icon (optional)</span>
          </label>
          <input
            v-model="formData.icon"
            type="text"
            class="input input-bordered"
            placeholder="Enter icon (e.g., emoji or identifier)"
          />
        </div>

        <!-- Generator Parameters -->
        <div class="form-control">
          <label class="label">
            <span class="label-text">Generator Parameters (JSON)</span>
          </label>
          <textarea
            v-model="generatorParamsJson"
            class="textarea textarea-bordered font-mono text-sm"
            rows="6"
            placeholder='{"seed": "12345", "waterLevel": "62", ...}'
          />
          <label v-if="generatorParamsError" class="label">
            <span class="label-text-alt text-error">{{ generatorParamsError }}</span>
          </label>
          <label class="label">
            <span class="label-text-alt">Enter generator parameters as JSON object. These will be passed to the terrain generator.</span>
          </label>
        </div>

        <!-- Entry Point -->
        <div class="form-control">
          <label class="label">
            <span class="label-text">Entry Point (optional, JSON)</span>
          </label>
          <textarea
            v-model="entryPointJson"
            class="textarea textarea-bordered font-mono text-sm"
            rows="3"
            placeholder='{"position": {"x": 0, "y": 0, "z": 0}, "size": {"x": 10, "y": 10, "z": 10}}'
          />
          <label v-if="entryPointError" class="label">
            <span class="label-text-alt text-error">{{ entryPointError }}</span>
          </label>
          <label class="label">
            <span class="label-text-alt">Define the entry point area where players spawn.</span>
          </label>
        </div>

        <!-- Enabled -->
        <div class="form-control">
          <label class="label cursor-pointer justify-start gap-4">
            <input
              v-model="formData.enabled"
              type="checkbox"
              class="checkbox"
            />
            <span class="label-text">Enabled</span>
          </label>
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
            {{ saving ? 'Saving...' : 'Save' }}
          </button>
        </div>
      </form>
    </div>
    <div class="modal-backdrop" @click="$emit('close')"></div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue';
import { useHexGrids, type HexGridWithId } from '@/composables/useHexGrids';
import ErrorAlert from '@components/ErrorAlert.vue';

const props = defineProps<{
  hexGrid: HexGridWithId | null;
  worldId: string;
}>();

const emit = defineEmits<{
  close: [];
  saved: [];
}>();

const { createHexGrid, updateHexGrid } = useHexGrids(props.worldId);

const isEditMode = computed(() => props.hexGrid !== null);

// Form data
const formData = ref({
  position: { q: 0, r: 0 },
  name: '',
  description: '',
  icon: '',
  enabled: true,
});

const generatorParamsJson = ref('');
const generatorParamsError = ref<string | null>(null);
const entryPointJson = ref('');
const entryPointError = ref<string | null>(null);
const saving = ref(false);
const saveError = ref<string | null>(null);

// Initialize form with existing data
watch(() => props.hexGrid, (hexGrid) => {
  if (hexGrid) {
    formData.value = {
      position: { ...hexGrid.position },
      name: hexGrid.name || '',
      description: hexGrid.description || '',
      icon: hexGrid.icon || '',
      enabled: hexGrid.enabled ?? true,
    };

    // Initialize generator parameters
    if (hexGrid.generatorParameters) {
      generatorParamsJson.value = JSON.stringify(hexGrid.generatorParameters, null, 2);
    } else {
      generatorParamsJson.value = '';
    }

    // Initialize entry point
    if (hexGrid.entryPoint) {
      entryPointJson.value = JSON.stringify(hexGrid.entryPoint, null, 2);
    } else {
      entryPointJson.value = '';
    }
  } else {
    // Reset for create mode
    formData.value = {
      position: { q: 0, r: 0 },
      name: '',
      description: '',
      icon: '',
      enabled: true,
    };
    generatorParamsJson.value = '';
    entryPointJson.value = '';
  }
}, { immediate: true });

/**
 * Validate form
 */
const isFormValid = computed(() => {
  return (
    formData.value.name.trim() !== '' &&
    formData.value.description.trim() !== '' &&
    !generatorParamsError.value &&
    !entryPointError.value
  );
});

/**
 * Parse generator parameters JSON
 */
const parseGeneratorParams = (): Record<string, string> | null => {
  generatorParamsError.value = null;

  if (!generatorParamsJson.value.trim()) {
    return null;
  }

  try {
    const parsed = JSON.parse(generatorParamsJson.value);

    if (typeof parsed !== 'object' || parsed === null || Array.isArray(parsed)) {
      generatorParamsError.value = 'Generator parameters must be a JSON object';
      return null;
    }

    // Convert all values to strings
    const result: Record<string, string> = {};
    for (const [key, value] of Object.entries(parsed)) {
      result[key] = String(value);
    }

    return result;
  } catch (err) {
    generatorParamsError.value = `Invalid JSON: ${(err as Error).message}`;
    return null;
  }
};

/**
 * Parse entry point JSON
 */
const parseEntryPoint = (): any | null => {
  entryPointError.value = null;

  if (!entryPointJson.value.trim()) {
    return null;
  }

  try {
    const parsed = JSON.parse(entryPointJson.value);
    return parsed;
  } catch (err) {
    entryPointError.value = `Invalid JSON: ${(err as Error).message}`;
    return null;
  }
};

/**
 * Handle save
 */
const handleSave = async () => {
  if (!isFormValid.value) {
    return;
  }

  // Parse and validate generator parameters
  const generatorParams = parseGeneratorParams();
  if (generatorParamsJson.value.trim() && !generatorParams) {
    return; // Error is already set
  }

  // Parse and validate entry point
  const entryPoint = parseEntryPoint();
  if (entryPointJson.value.trim() && !entryPoint) {
    return; // Error is already set
  }

  saving.value = true;
  saveError.value = null;

  try {
    const payload: Partial<HexGridWithId> = {
      position: formData.value.position,
      name: formData.value.name,
      description: formData.value.description,
      icon: formData.value.icon || undefined,
      enabled: formData.value.enabled,
      generatorParameters: generatorParams || undefined,
      entryPoint: entryPoint || undefined,
    };

    if (isEditMode.value) {
      await updateHexGrid(
        formData.value.position.q,
        formData.value.position.r,
        payload
      );
    } else {
      await createHexGrid(payload);
    }

    emit('saved');
  } catch (err) {
    saveError.value = `Failed to save hex grid: ${(err as Error).message}`;
  } finally {
    saving.value = false;
  }
};
</script>
