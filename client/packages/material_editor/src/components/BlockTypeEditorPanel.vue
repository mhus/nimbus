<template>
  <TransitionRoot :show="true" as="template">
    <Dialog as="div" class="relative z-50" @close="emit('close')">
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
                {{ isCreate ? 'Create Block Type' : `Edit Block Type #${formData.id}` }}
              </DialogTitle>

              <div class="space-y-6 max-h-[70vh] overflow-y-auto">
                <!-- Basic Properties -->
                <div class="form-control">
                  <label class="label">
                    <span class="label-text font-semibold">ID</span>
                  </label>
                  <input
                    v-model.number="formData.id"
                    type="number"
                    class="input input-bordered"
                    :disabled="!isCreate"
                  />
                </div>

                <div class="form-control">
                  <label class="label">
                    <span class="label-text font-semibold">Description</span>
                  </label>
                  <input
                    v-model="formData.description"
                    type="text"
                    class="input input-bordered"
                    placeholder="Enter block type description..."
                  />
                </div>

                <div class="form-control">
                  <label class="label">
                    <span class="label-text font-semibold">Initial Status</span>
                  </label>
                  <input
                    v-model.number="formData.initialStatus"
                    type="number"
                    class="input input-bordered"
                    placeholder="0"
                  />
                </div>

                <!-- Status Modifiers -->
                <div class="divider">Status Modifiers</div>

                <div class="space-y-4">
                  <div v-for="status in statusList" :key="status" class="card bg-base-200">
                    <div class="card-body">
                      <div class="flex items-center justify-between">
                        <h3 class="font-semibold">Status {{ status }}</h3>
                        <button
                          class="btn btn-ghost btn-sm btn-square"
                          @click="removeStatus(status)"
                        >
                          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
                          </svg>
                        </button>
                      </div>
                      <div class="text-sm text-base-content/70 mt-2">
                        <p>Modifier configuration (simplified editor)</p>
                        <p class="mt-1">Keys: {{ Object.keys(formData.modifiers[status] || {}).join(', ') || 'Empty' }}</p>
                      </div>
                    </div>
                  </div>

                  <button class="btn btn-outline btn-sm" @click="addStatus">
                    <svg class="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
                    </svg>
                    Add Status
                  </button>
                </div>

                <!-- JSON Editor (Fallback) -->
                <div class="form-control">
                  <label class="label">
                    <span class="label-text font-semibold">Modifiers (JSON)</span>
                    <span class="label-text-alt">Advanced editing</span>
                  </label>
                  <textarea
                    v-model="modifiersJson"
                    class="textarea textarea-bordered font-mono text-sm h-48"
                    :class="{ 'textarea-error': jsonError }"
                  ></textarea>
                  <label v-if="jsonError" class="label">
                    <span class="label-text-alt text-error">{{ jsonError }}</span>
                  </label>
                </div>
              </div>

              <!-- Actions -->
              <div class="mt-6 flex justify-end gap-2">
                <button class="btn btn-ghost" @click="emit('close')">
                  Cancel
                </button>
                <button class="btn btn-primary" @click="handleSave" :disabled="saving">
                  <span v-if="saving" class="loading loading-spinner loading-sm mr-2"></span>
                  {{ saving ? 'Saving...' : 'Save' }}
                </button>
              </div>
            </DialogPanel>
          </TransitionChild>
        </div>
      </div>
    </Dialog>
  </TransitionRoot>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue';
import { Dialog, DialogPanel, DialogTitle, TransitionRoot, TransitionChild } from '@headlessui/vue';
import type { BlockType, BlockModifier } from '@nimbus/shared';
import { useBlockTypes } from '../composables/useBlockTypes';

interface Props {
  blockType: BlockType | null;
  worldId: string;
}

const props = defineProps<Props>();

const emit = defineEmits<{
  (e: 'close'): void;
  (e: 'saved'): void;
}>();

const { createBlockType, updateBlockType, getNextAvailableId } = useBlockTypes(props.worldId);

const isCreate = computed(() => !props.blockType);
const saving = ref(false);
const jsonError = ref<string | null>(null);

// Form data
const formData = ref<Partial<BlockType>>({
  id: 0,
  description: '',
  initialStatus: 0,
  modifiers: {},
});

// JSON representation of modifiers
const modifiersJson = ref('{}');

// Watch modifiers JSON changes
watch(modifiersJson, (newValue) => {
  try {
    const parsed = JSON.parse(newValue);
    formData.value.modifiers = parsed;
    jsonError.value = null;
  } catch (err) {
    jsonError.value = 'Invalid JSON';
  }
});

// Watch formData.modifiers changes (from UI)
watch(() => formData.value.modifiers, (newValue) => {
  try {
    modifiersJson.value = JSON.stringify(newValue, null, 2);
  } catch (err) {
    // Ignore
  }
}, { deep: true });

// Initialize form
const initializeForm = async () => {
  if (props.blockType) {
    formData.value = JSON.parse(JSON.stringify(props.blockType));
  } else {
    const nextId = await getNextAvailableId();
    formData.value = {
      id: nextId,
      description: '',
      initialStatus: 0,
      modifiers: {
        0: {} as BlockModifier, // Default status
      },
    };
  }
  modifiersJson.value = JSON.stringify(formData.value.modifiers, null, 2);
};

initializeForm();

// Status list
const statusList = computed(() => {
  return Object.keys(formData.value.modifiers || {}).map(Number).sort((a, b) => a - b);
});

// Add status
const addStatus = () => {
  const existingStatuses = statusList.value;
  const nextStatus = existingStatuses.length > 0 ? Math.max(...existingStatuses) + 1 : 0;

  if (!formData.value.modifiers) {
    formData.value.modifiers = {};
  }

  formData.value.modifiers[nextStatus] = {} as BlockModifier;
};

// Remove status
const removeStatus = (status: number) => {
  if (formData.value.modifiers && formData.value.modifiers[status]) {
    delete formData.value.modifiers[status];
  }
};

// Handle save
const handleSave = async () => {
  if (jsonError.value) {
    alert('Please fix JSON errors before saving');
    return;
  }

  if (!formData.value.modifiers || Object.keys(formData.value.modifiers).length === 0) {
    alert('At least one modifier (status 0) is required');
    return;
  }

  saving.value = true;

  try {
    if (isCreate.value) {
      await createBlockType(formData.value);
    } else {
      await updateBlockType(formData.value.id!, formData.value);
    }

    emit('saved');
  } catch (err) {
    alert('Failed to save block type');
  } finally {
    saving.value = false;
  }
};
</script>
