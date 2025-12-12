<template>
  <div class="space-y-6">
    <!-- Header -->
    <div class="flex items-center justify-between">
      <button class="btn btn-ghost gap-2" @click="handleBack">
        <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" />
        </svg>
        Back to List
      </button>
      <h2 class="text-2xl font-bold">
        {{ isNew ? 'Create New World' : 'Edit World' }}
      </h2>
    </div>

    <!-- Error State -->
    <div v-if="error" class="alert alert-error">
      <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
      </svg>
      <span>{{ error }}</span>
    </div>

    <!-- Edit Form -->
    <div class="space-y-6">
      <!-- Basic Info Card -->
      <div class="card bg-base-100 shadow-xl">
        <div class="card-body">
          <h3 class="card-title">Basic Information</h3>
          <form @submit.prevent="handleSave" class="space-y-4">
            <!-- World ID -->
            <div class="form-control">
              <label class="label">
                <span class="label-text font-medium">World ID</span>
              </label>
              <input
                v-model="formData.worldId"
                type="text"
                placeholder="Enter unique world ID (e.g., main-world-1)"
                class="input input-bordered w-full"
                :disabled="!isNew"
                required
              />
              <label class="label">
                <span class="label-text-alt">Unique identifier for this world</span>
              </label>
            </div>

            <!-- Name -->
            <div class="form-control">
              <label class="label">
                <span class="label-text font-medium">Name</span>
              </label>
              <input
                v-model="formData.name"
                type="text"
                placeholder="Enter world name"
                class="input input-bordered w-full"
                required
              />
            </div>

            <!-- Description -->
            <div class="form-control">
              <label class="label">
                <span class="label-text font-medium">Description</span>
              </label>
              <textarea
                v-model="formData.description"
                placeholder="Enter world description"
                class="textarea textarea-bordered w-full"
                rows="3"
              ></textarea>
            </div>

            <!-- Enabled Status -->
            <div class="form-control">
              <label class="label cursor-pointer justify-start gap-4">
                <span class="label-text font-medium">Enabled</span>
                <input
                  v-model="formData.enabled"
                  type="checkbox"
                  class="toggle toggle-success"
                />
              </label>
            </div>

            <!-- Public Flag -->
            <div class="form-control">
              <label class="label cursor-pointer justify-start gap-4">
                <span class="label-text font-medium">Public</span>
                <input
                  v-model="formData.publicFlag"
                  type="checkbox"
                  class="toggle toggle-info"
                />
              </label>
              <label class="label">
                <span class="label-text-alt">Allow public access to this world</span>
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

      <!-- Generation Settings Card -->
      <div class="card bg-base-100 shadow-xl">
        <div class="card-body">
          <h3 class="card-title">World Generation Settings</h3>
          <div class="space-y-4">
            <!-- Ground Level -->
            <div class="form-control">
              <label class="label">
                <span class="label-text font-medium">Ground Level (Y)</span>
              </label>
              <input
                v-model.number="formData.groundLevel"
                type="number"
                placeholder="0"
                class="input input-bordered w-full"
              />
              <label class="label">
                <span class="label-text-alt">Default Y coordinate for ground generation</span>
              </label>
            </div>

            <!-- Water Level -->
            <div class="form-control">
              <label class="label">
                <span class="label-text font-medium">Water Level (Y)</span>
              </label>
              <input
                v-model.number="formData.waterLevel"
                type="number"
                placeholder="Optional"
                class="input input-bordered w-full"
              />
              <label class="label">
                <span class="label-text-alt">Y coordinate for water surface (optional)</span>
              </label>
            </div>

            <!-- Ground Block Type -->
            <div class="form-control">
              <label class="label">
                <span class="label-text font-medium">Ground Block Type</span>
              </label>
              <input
                v-model="formData.groundBlockType"
                type="text"
                placeholder="r/grass"
                class="input input-bordered w-full"
              />
            </div>

            <!-- Water Block Type -->
            <div class="form-control">
              <label class="label">
                <span class="label-text font-medium">Water Block Type</span>
              </label>
              <input
                v-model="formData.waterBlockType"
                type="text"
                placeholder="r/ocean"
                class="input input-bordered w-full"
              />
            </div>
          </div>
        </div>
      </div>

      <!-- Hierarchy Card -->
      <div class="card bg-base-100 shadow-xl">
        <div class="card-body">
          <h3 class="card-title">Hierarchy</h3>
          <div class="space-y-4">
            <!-- Parent -->
            <div class="form-control">
              <label class="label">
                <span class="label-text font-medium">Parent World</span>
              </label>
              <input
                v-model="formData.parent"
                type="text"
                placeholder="Optional parent world ID"
                class="input input-bordered w-full"
              />
            </div>

            <!-- Branch -->
            <div class="form-control">
              <label class="label">
                <span class="label-text font-medium">Branch</span>
              </label>
              <input
                v-model="formData.branch"
                type="text"
                placeholder="Optional branch name"
                class="input input-bordered w-full"
              />
            </div>
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
import { useRegion } from '@/composables/useRegion';
import { worldServiceFrontend, type World } from '../services/WorldServiceFrontend';

const props = defineProps<{
  world: World | 'new';
}>();

const emit = defineEmits<{
  back: [];
  saved: [];
}>();

const { currentRegionId } = useRegion();

const isNew = computed(() => props.world === 'new');

const saving = ref(false);
const error = ref<string | null>(null);
const successMessage = ref<string | null>(null);

const formData = ref({
  worldId: '',
  name: '',
  description: '',
  enabled: true,
  publicFlag: false,
  parent: '',
  branch: '',
  groundLevel: 0,
  waterLevel: null as number | null,
  groundBlockType: 'r/grass',
  waterBlockType: 'r/ocean',
});

const loadWorld = () => {
  if (isNew.value) {
    formData.value = {
      worldId: '',
      name: '',
      description: '',
      enabled: true,
      publicFlag: false,
      parent: '',
      branch: '',
      groundLevel: 0,
      waterLevel: null,
      groundBlockType: 'r/grass',
      waterBlockType: 'r/ocean',
    };
    return;
  }

  // Load from props
  const world = props.world as World;
  formData.value = {
    worldId: world.worldId,
    name: world.name,
    description: world.description || '',
    enabled: world.enabled,
    publicFlag: world.publicFlag,
    parent: world.parent || '',
    branch: world.branch || '',
    groundLevel: world.groundLevel,
    waterLevel: world.waterLevel,
    groundBlockType: world.groundBlockType,
    waterBlockType: world.waterBlockType,
  };
};

const handleSave = async () => {
  if (!currentRegionId.value) {
    error.value = 'No region selected';
    return;
  }

  saving.value = true;
  error.value = null;
  successMessage.value = null;

  try {
    const request = {
      worldId: formData.value.worldId,
      name: formData.value.name,
      description: formData.value.description,
      enabled: formData.value.enabled,
      parent: formData.value.parent,
      branch: formData.value.branch,
      groundLevel: formData.value.groundLevel,
      waterLevel: formData.value.waterLevel ?? undefined,
      groundBlockType: formData.value.groundBlockType,
      waterBlockType: formData.value.waterBlockType,
    };

    if (isNew.value) {
      await worldServiceFrontend.createWorld(currentRegionId.value, request);
      successMessage.value = 'World created successfully';
    } else {
      const world = props.world as World;
      await worldServiceFrontend.updateWorld(currentRegionId.value, world.worldId, request);
      successMessage.value = 'World updated successfully';
    }

    setTimeout(() => {
      emit('saved');
    }, 1000);
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Failed to save world';
    console.error('[WorldEditor] Failed to save world:', e);
  } finally {
    saving.value = false;
  }
};

const handleBack = () => {
  emit('back');
};

onMounted(() => {
  loadWorld();
});
</script>
