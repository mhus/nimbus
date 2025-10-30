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
import { ref, computed, onMounted } from 'vue';
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
  (e: 'edit-modifier', data: { blockType: BlockType; status: number; modifier: BlockModifier }): void;
}>();

const { createBlockType, updateBlockType, getNextAvailableId } = useBlockTypes(props.worldId);

const isCreate = computed(() => !props.blockType);
const saving = ref(false);

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
    const nextId = await getNextAvailableId();
    formData.value = {
      id: nextId,
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
  if (modifier.sound) parts.push('sound');
  if (modifier.spriteCount) parts.push(`sprites: ${modifier.spriteCount}`);
  if (modifier.alpha !== undefined) parts.push(`alpha: ${modifier.alpha}`);

  return parts.length > 0 ? parts.join(', ') : 'Empty';
};

// Add status
const addStatus = () => {
  const existingStatuses = statusList.value;
  const nextStatus = existingStatuses.length > 0 ? Math.max(...existingStatuses) + 1 : 1;

  const statusId = prompt(`Enter status ID (default: ${nextStatus}):`, nextStatus.toString());

  if (statusId === null) return; // Cancelled

  const newStatusId = parseInt(statusId, 10);

  if (isNaN(newStatusId)) {
    alert('Invalid status ID');
    return;
  }

  if (formData.value.modifiers && formData.value.modifiers[newStatusId]) {
    alert(`Status ${newStatusId} already exists`);
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
const changeStatusId = (oldStatus: number) => {
  if (oldStatus === 0) {
    alert('Cannot change status 0 (default status)');
    return;
  }

  const newStatusIdStr = prompt(`Change status ID from ${oldStatus} to:`, oldStatus.toString());

  if (newStatusIdStr === null) return; // Cancelled

  const newStatusId = parseInt(newStatusIdStr, 10);

  if (isNaN(newStatusId)) {
    alert('Invalid status ID');
    return;
  }

  if (newStatusId === oldStatus) return; // No change

  if (formData.value.modifiers && formData.value.modifiers[newStatusId]) {
    alert(`Status ${newStatusId} already exists`);
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
  } catch (err) {
    alert('Failed to save block type');
  } finally {
    saving.value = false;
  }
};
</script>
