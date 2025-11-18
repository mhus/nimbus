<template>
  <div class="p-6 space-y-6">
    <!-- Loading State -->
    <div v-if="loading" class="flex justify-center py-8">
      <span class="loading loading-spinner loading-lg"></span>
    </div>

    <!-- Error State -->
    <div v-else-if="error" class="alert alert-error">
      <span>{{ error }}</span>
    </div>

    <!-- Editor Form -->
    <div v-else-if="localItem" class="space-y-6">
      <!-- Item ID -->
      <div class="form-control">
        <label class="label">
          <span class="label-text font-semibold">Item ID</span>
          <span v-if="!isNew" class="label-text-alt text-xs opacity-50">Read-only</span>
        </label>
        <input
          v-model="editableItemId"
          type="text"
          class="input input-bordered"
          :disabled="!isNew"
          placeholder="item_id"
        />
      </div>

      <!-- Description -->
      <div class="form-control">
        <label class="label">
          <span class="label-text font-semibold">Description</span>
        </label>
        <textarea
          v-model="localItem.description"
          class="textarea textarea-bordered"
          rows="2"
          placeholder="Item description"
        ></textarea>
      </div>

      <!-- Pose -->
      <div class="form-control">
        <label class="label">
          <span class="label-text font-semibold">Pose</span>
          <span class="label-text-alt text-xs">Animation when using item</span>
        </label>
        <select v-model="localItem.pose" class="select select-bordered">
          <option value="">None</option>
          <option value="use">Use</option>
          <option value="attack">Attack</option>
          <option value="place">Place</option>
          <option value="drink">Drink</option>
          <option value="eat">Eat</option>
        </select>
      </div>

      <!-- Wait / Duration -->
      <div class="grid grid-cols-2 gap-4">
        <div class="form-control">
          <label class="label">
            <span class="label-text font-semibold">Wait (ms)</span>
            <span class="label-text-alt text-xs">Delay before activation</span>
          </label>
          <input
            v-model.number="localItem.wait"
            type="number"
            class="input input-bordered"
            placeholder="0"
            min="0"
          />
        </div>

        <div class="form-control">
          <label class="label">
            <span class="label-text font-semibold">Duration (ms)</span>
            <span class="label-text-alt text-xs">Pose duration</span>
          </label>
          <input
            v-model.number="localItem.duration"
            type="number"
            class="input input-bordered"
            placeholder="1000"
            min="0"
          />
        </div>
      </div>

      <!-- OnUseEffect -->
      <div class="divider">Scrawl Effect</div>
      <ScriptActionEditor
        v-model="localItem.onUseEffect"
      />

      <!-- Block Data -->
      <div class="divider">Block Data</div>
      <div v-if="localItem.block" class="space-y-4">
        <div class="form-control">
          <label class="label">
            <span class="label-text font-semibold">Block Type ID</span>
          </label>
          <input
            v-model.number="localItem.block.blockTypeId"
            type="number"
            class="input input-bordered"
            placeholder="1000"
          />
        </div>

        <div class="form-control">
          <label class="label">
            <span class="label-text font-semibold">Display Name</span>
          </label>
          <input
            v-model="displayName"
            type="text"
            class="input input-bordered"
            placeholder="Item display name"
          />
        </div>
      </div>

      <!-- Actions -->
      <div class="flex gap-2 pt-4">
        <button class="btn btn-primary" @click="save">
          <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
          </svg>
          Save
        </button>
        <button class="btn btn-ghost" @click="$emit('close')">
          Cancel
        </button>
        <div class="flex-1"></div>
        <button v-if="!isNew" class="btn btn-error" @click="confirmDelete">
          <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
          </svg>
          Delete
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue';
import type { ItemData } from '@nimbus/shared';
import { ItemApiService } from '../services/itemApiService';
import ScriptActionEditor from '../components/ScriptActionEditor.vue';

const props = defineProps<{
  itemId: string;
  isNew: boolean;
}>();

const emit = defineEmits<{
  save: [];
  close: [];
  delete: [];
}>();

const loading = ref(false);
const error = ref<string | null>(null);
const localItem = ref<ItemData | null>(null);
const editableItemId = ref(props.itemId);

const displayName = computed({
  get: () => localItem.value?.block.metadata?.displayName || '',
  set: (value: string) => {
    if (localItem.value) {
      if (!localItem.value.block.metadata) {
        localItem.value.block.metadata = {};
      }
      localItem.value.block.metadata.displayName = value;
    }
  },
});

async function loadItem() {
  if (props.isNew) {
    // Create new item template
    localItem.value = {
      block: {
        position: { x: 0, y: 0, z: 0 },
        blockTypeId: 1000,
        status: 0,
        metadata: {
          displayName: 'New Item',
        },
      },
      description: '',
      pose: 'use',
      wait: 0,
      duration: 1000,
    };
    return;
  }

  loading.value = true;
  error.value = null;

  try {
    const itemData = await ItemApiService.getItem(props.itemId);
    if (!itemData) {
      error.value = 'Item not found';
      return;
    }
    localItem.value = itemData;
  } catch (e: any) {
    error.value = e.message || 'Failed to load item';
    console.error('Failed to load item:', e);
  } finally {
    loading.value = false;
  }
}

async function save() {
  if (!localItem.value) return;

  loading.value = true;
  error.value = null;

  try {
    if (props.isNew) {
      await ItemApiService.createItem(editableItemId.value, localItem.value);
    } else {
      await ItemApiService.updateItem(props.itemId, localItem.value);
    }
    emit('save');
  } catch (e: any) {
    error.value = e.message || 'Failed to save item';
    console.error('Failed to save item:', e);
  } finally {
    loading.value = false;
  }
}

async function confirmDelete() {
  if (!confirm(`Delete item "${props.itemId}"?`)) {
    return;
  }

  loading.value = true;
  error.value = null;

  try {
    await ItemApiService.deleteItem(props.itemId);
    emit('delete');
  } catch (e: any) {
    error.value = e.message || 'Failed to delete item';
    console.error('Failed to delete item:', e);
  } finally {
    loading.value = false;
  }
}

watch(() => props.itemId, () => {
  loadItem();
});

onMounted(() => {
  loadItem();
});
</script>
