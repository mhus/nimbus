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

              <div class="space-y-6 max-h-[70vh] overflow-y-auto pr-2">
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
                    placeholder="0 = auto-generate"
                  />
                  <label v-if="isCreate" class="label">
                    <span class="label-text-alt">Leave as 0 to auto-generate, or enter a specific ID</span>
                  </label>
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

                <div class="space-y-3">
                  <div
                    v-for="status in statusList"
                    :key="status"
                    class="card bg-base-200 hover:shadow-md transition-shadow"
                  >
                    <div class="card-body p-4">
                      <div class="flex items-center justify-between">
                        <div class="flex items-center gap-3">
                          <div
                            class="badge badge-primary cursor-pointer hover:badge-secondary"
                            @click="changeStatusId(status)"
                            title="Click to change status ID"
                          >
                            Status {{ status }}
                          </div>
                          <div class="text-sm text-base-content/70">
                            {{ getModifierSummary(status) }}
                          </div>
                        </div>
                        <div class="flex gap-2">
                          <button
                            class="btn btn-sm btn-outline"
                            @click="editModifier(status)"
                          >
                            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
                            </svg>
                            Edit
                          </button>
                          <button
                            class="btn btn-sm btn-ghost btn-square text-error"
                            @click="removeStatus(status)"
                            :disabled="status === 0"
                          >
                            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
                            </svg>
                          </button>
                        </div>
                      </div>
                    </div>
                  </div>

                  <button class="btn btn-outline btn-sm w-full" @click="addStatus">
                    <svg class="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
                    </svg>
                    Add Status
                  </button>
                </div>
              </div>

              <!-- Actions -->
              <div class="mt-6 flex justify-between gap-2">
                <div class="flex gap-2">
                  <button class="btn btn-outline btn-sm" @click="showJsonEditor = true">
                    <svg class="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 20l4-16m4 4l4 4-4 4M6 16l-4-4 4-4" />
                    </svg>
                    Source
                  </button>
                  <button
                    v-if="!isCreate"
                    class="btn btn-outline btn-sm btn-info"
                    @click="openDuplicateDialog"
                    :disabled="saving"
                    title="Save a copy with a new ID"
                  >
                    <svg class="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 16H6a2 2 0 01-2-2V6a2 2 0 012-2h8a2 2 0 012 2v2m-6 12h8a2 2 0 002-2v-8a2 2 0 00-2-2h-8a2 2 0 00-2 2v8a2 2 0 002 2z" />
                    </svg>
                    Save as Copy
                  </button>
                </div>
                <div class="flex gap-2">
                  <button class="btn btn-ghost" @click="emit('close')">
                    Cancel
                  </button>
                  <button class="btn btn-primary" @click="handleSave" :disabled="saving">
                    <span v-if="saving" class="loading loading-spinner loading-sm mr-2"></span>
                    {{ saving ? 'Saving...' : 'Save' }}
                  </button>
                </div>
              </div>
            </DialogPanel>
          </TransitionChild>
        </div>
      </div>
    </Dialog>
  </TransitionRoot>

  <!-- JSON Editor Dialog -->
  <JsonEditorDialog
    v-model:is-open="showJsonEditor"
    :model-value="formData"
    @apply="handleJsonApply"
  />

  <!-- Duplicate BlockType Dialog -->
  <Dialog v-if="showDuplicateDialog" as="div" class="relative z-50" @close="closeDuplicateDialog">
    <div class="fixed inset-0 bg-black bg-opacity-25" />
    <div class="fixed inset-0 overflow-y-auto">
      <div class="flex min-h-full items-center justify-center p-4">
        <DialogPanel class="w-full max-w-md transform overflow-hidden rounded-2xl bg-base-100 p-6 text-left align-middle shadow-xl transition-all">
          <DialogTitle class="text-lg font-bold mb-4">
            Save as Copy
          </DialogTitle>

          <p class="text-sm text-base-content/70 mb-4">
            Create a copy of this BlockType with a new ID.
          </p>

          <div class="form-control">
            <label class="label">
              <span class="label-text font-semibold">New BlockType ID</span>
              <span class="label-text-alt text-error" v-if="!newBlockTypeId">Required</span>
            </label>
            <input
              v-model="newBlockTypeId"
              type="text"
              class="input input-bordered"
              placeholder="e.g., custom:my-block or w/123"
              @keyup.enter="handleDuplicate"
            />
            <label class="label">
              <span class="label-text-alt">Use format: group:name or group/name</span>
            </label>
          </div>

          <div v-if="duplicateError" class="alert alert-error mt-4">
            <svg xmlns="http://www.w3.org/2000/svg" class="stroke-current shrink-0 h-6 w-6" fill="none" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 14l2-2m0 0l2-2m-2 2l-2-2m2 2l2 2m7-2a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
            <span>{{ duplicateError }}</span>
          </div>

          <div class="mt-6 flex justify-end gap-2">
            <button class="btn btn-ghost" @click="closeDuplicateDialog" :disabled="duplicating">
              Cancel
            </button>
            <button
              class="btn btn-primary"
              @click="handleDuplicate"
              :disabled="!newBlockTypeId || duplicating"
            >
              <span v-if="duplicating" class="loading loading-spinner loading-sm mr-2"></span>
              {{ duplicating ? 'Duplicating...' : 'Save Copy' }}
            </button>
          </div>
        </DialogPanel>
      </div>
    </div>
  </Dialog>

  <!-- Input Dialog -->
  <InputDialog
    v-model:is-open="showInputDialog"
    :title="inputDialogTitle"
    :message="inputDialogMessage"
    :default-value="inputDialogDefaultValue"
    @ok="handleInputOk"
    @cancel="handleInputCancel"
  />
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { Dialog, DialogPanel, DialogTitle, TransitionRoot, TransitionChild } from '@headlessui/vue';
import type { BlockType, BlockModifier } from '@nimbus/shared';
import { useBlockTypes } from '@/composables/useBlockTypes';
import JsonEditorDialog from '@components/JsonEditorDialog.vue';
import InputDialog from '@components/InputDialog.vue';

interface Props {
  blockType: BlockType | null;
  worldId: string;
}

const props = defineProps<Props>();

const emit = defineEmits<{
  (e: 'close'): void;
  (e: 'saved'): void;
  (e: 'edit-modifier', data: { blockType: BlockType; status: number; modifier: BlockModifier }): void;
}>();

const { createBlockType, updateBlockType, getNextAvailableId } = useBlockTypes(props.worldId);

const isCreate = computed(() => !props.blockType);
const saving = ref(false);
const showJsonEditor = ref(false);

// Duplicate dialog state
const showDuplicateDialog = ref(false);
const newBlockTypeId = ref('');
const duplicating = ref(false);
const duplicateError = ref<string | null>(null);

// Input dialog state
const showInputDialog = ref(false);
const inputDialogTitle = ref('');
const inputDialogMessage = ref('');
const inputDialogDefaultValue = ref('');
const inputDialogCallback = ref<((value: string | null) => void) | null>(null);

// Helper to show input dialog (replaces prompt)
const showInput = (title: string, message: string, defaultValue: string): Promise<string | null> => {
  return new Promise((resolve) => {
    inputDialogTitle.value = title;
    inputDialogMessage.value = message;
    inputDialogDefaultValue.value = defaultValue;
    inputDialogCallback.value = resolve;
    showInputDialog.value = true;
  });
};

const handleInputOk = (value: string) => {
  if (inputDialogCallback.value) {
    inputDialogCallback.value(value);
    inputDialogCallback.value = null;
  }
};

const handleInputCancel = () => {
  if (inputDialogCallback.value) {
    inputDialogCallback.value(null);
    inputDialogCallback.value = null;
  }
};

// Form data
const formData = ref<Partial<BlockType>>({
  id: 0,
  description: '',
  initialStatus: 0,
  modifiers: {},
});

// Expose method to update modifier from parent
const updateModifier = (status: number, modifier: BlockModifier) => {
  if (formData.value.modifiers) {
    formData.value.modifiers[status] = modifier;
  }
};

// Expose for parent access
defineExpose({
  updateModifier,
  formData
});

// Initialize form
const initializeForm = async () => {
  if (props.blockType) {
    formData.value = JSON.parse(JSON.stringify(props.blockType));
  } else {
    formData.value = {
      id: 0, // 0 means auto-generate
      description: '',
      initialStatus: 0,
      modifiers: {
        0: { visibility: { shape: 1, textures: {} } }, // Default status with CUBE shape
      },
    };
  }
};

onMounted(() => {
  initializeForm();
});

// Status list
const statusList = computed(() => {
  return Object.keys(formData.value.modifiers || {}).map(Number).sort((a, b) => a - b);
});

// Get modifier summary
const getModifierSummary = (status: number): string => {
  const modifier = formData.value.modifiers?.[status];
  if (!modifier) return 'Empty';

  const parts: string[] = [];
  if (modifier.visibility) parts.push('visibility');
  if (modifier.physics) parts.push('physics');
  if (modifier.wind) parts.push('wind');
  if (modifier.effects) parts.push('effects');
  if (modifier.illumination) parts.push('illumination');
  if (modifier.audio) parts.push('audio');

  return parts.length > 0 ? parts.join(', ') : 'Empty';
};

// Add status
const addStatus = async () => {
  const existingStatuses = statusList.value;
  const nextStatus = existingStatuses.length > 0 ? Math.max(...existingStatuses) + 1 : 1;

  const statusId = await showInput(
    'Add Status',
    `Enter status ID (default: ${nextStatus}):`,
    nextStatus.toString()
  );

  if (statusId === null) return; // Cancelled

  const newStatusId = parseInt(statusId, 10);

  if (isNaN(newStatusId)) {
    return;
  }

  if (formData.value.modifiers && formData.value.modifiers[newStatusId]) {
    return;
  }

  if (!formData.value.modifiers) {
    formData.value.modifiers = {};
  }

  formData.value.modifiers[newStatusId] = {
    visibility: { shape: 1, textures: {} }
  };
};

// Change status ID
const changeStatusId = async (oldStatus: number) => {
  if (oldStatus === 0) {
    return;
  }

  const newStatusIdStr = await showInput(
    'Change Status ID',
    `Change status ID from ${oldStatus} to:`,
    oldStatus.toString()
  );

  if (newStatusIdStr === null) return; // Cancelled

  const newStatusId = parseInt(newStatusIdStr, 10);

  if (isNaN(newStatusId)) {
    return;
  }

  if (newStatusId === oldStatus) return; // No change

  if (formData.value.modifiers && formData.value.modifiers[newStatusId]) {
    return;
  }

  if (!formData.value.modifiers || !formData.value.modifiers[oldStatus]) {
    return;
  }

  // Copy modifier to new status ID
  formData.value.modifiers[newStatusId] = formData.value.modifiers[oldStatus];

  // Delete old status
  delete formData.value.modifiers[oldStatus];
};

// Remove status
const removeStatus = (status: number) => {
  if (status === 0) {
    alert('Cannot remove status 0 (default status)');
    return;
  }

  if (formData.value.modifiers && formData.value.modifiers[status]) {
    delete formData.value.modifiers[status];
  }
};

// Edit modifier - emit event to parent
const editModifier = (status: number) => {
  if (!formData.value.modifiers) return;

  emit('edit-modifier', {
    blockType: formData.value as BlockType,
    status,
    modifier: formData.value.modifiers[status]
  });
};

// Handle save
const handleSave = async () => {
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
  } catch (err: any) {
    // Extract error message from server response
    let errorMessage = 'Failed to save block type';
    if (err?.response?.data?.error) {
      errorMessage = err.response.data.error;
    } else if (err?.message) {
      errorMessage = err.message;
    }
    alert(errorMessage);
  } finally {
    saving.value = false;
  }
};

// Handle JSON apply from JSON editor
const handleJsonApply = (jsonData: any) => {
  formData.value = jsonData;
};

// Duplicate BlockType functionality
const openDuplicateDialog = () => {
  newBlockTypeId.value = '';
  duplicateError.value = null;
  showDuplicateDialog.value = true;
};

const closeDuplicateDialog = () => {
  showDuplicateDialog.value = false;
  newBlockTypeId.value = '';
  duplicateError.value = null;
};

const handleDuplicate = async () => {
  if (!newBlockTypeId.value || duplicating.value || !props.blockType?.id) {
    return;
  }

  duplicating.value = true;
  duplicateError.value = null;

  try {
    const apiUrl = import.meta.env.VITE_CONTROL_API_URL || 'http://localhost:9043';
    const sourceBlockId = props.blockType.id;
    const url = `${apiUrl}/api/worlds/${props.worldId}/blocktypes/duplicate/${encodeURIComponent(sourceBlockId)}`;

    const response = await fetch(url, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        newBlockId: newBlockTypeId.value,
      }),
    });

    if (!response.ok) {
      const errorData = await response.json().catch(() => ({ error: response.statusText }));
      duplicateError.value = errorData.error || `Failed to duplicate BlockType: ${response.statusText}`;
      return;
    }

    const result = await response.json();

    // Close dialog
    closeDuplicateDialog();

    // Show success message
    alert(`BlockType duplicated successfully!\n\nNew ID: ${result.blockId}\n\nThe page will reload to show the updated list.`);

    // Emit saved event to refresh the list
    emit('saved');

    // Close the editor
    emit('close');
  } catch (err) {
    duplicateError.value = err instanceof Error ? err.message : 'Unknown error occurred';
  } finally {
    duplicating.value = false;
  }
};
</script>
