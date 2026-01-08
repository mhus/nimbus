<template>
  <div class="space-y-4">
    <!-- Header -->
    <div class="flex items-center justify-between">
      <div class="flex items-center gap-2">
        <button class="btn btn-ghost gap-2" @click="handleBack">
          <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" />
          </svg>
          Back to List
        </button>
      </div>
      <h2 class="text-2xl font-bold">
        {{ isNew ? 'Create New World' : 'Edit World' }}
      </h2>
    </div>

    <!-- Hierarchy Info -->
    <div class="alert">
      <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
      </svg>
      <div class="text-sm">
        <span class="font-medium">Hierarchy:</span>
        <span class="ml-2">region: {{ currentRegionId || '-' }}</span>
        <span class="mx-1">→</span>
        <span>world: {{ formData.worldId || '-' }}</span>
        <span class="mx-1">→</span>
        <span>zone: -</span>
        <span class="mx-1">→</span>
        <span>instance: -</span>
      </div>
    </div>

    <!-- Error State -->
    <div v-if="error" class="alert alert-error">
      <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
      </svg>
      <span>{{ error }}</span>
    </div>

    <!-- System Information (Read-Only) -->
    <div v-if="!isNew" class="card bg-base-100 shadow-xl">
      <div class="card-body">
        <h3 class="card-title">System Information</h3>
        <div class="grid grid-cols-2 gap-4 text-sm">
          <div><span class="font-medium">Database ID:</span> {{ (world as World).id || 'N/A' }}</div>
          <div><span class="font-medium">Region ID:</span> {{ currentRegionId || 'N/A' }}</div>
          <div><span class="font-medium">Created:</span> {{ formatDate((world as World).createdAt) }}</div>
          <div><span class="font-medium">Updated:</span> {{ formatDate((world as World).updatedAt) }}</div>
        </div>
      </div>
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

            <!-- Parent World -->
            <div class="form-control">
              <label class="label">
                <span class="label-text font-medium">Parent World</span>
              </label>
              <input
                v-model="formData.parent"
                type="text"
                placeholder="Optional parent world reference"
                class="input input-bordered w-full"
              />
              <label class="label">
                <span class="label-text-alt">Optional reference to parent world/group</span>
              </label>
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

            <!-- Instanceable Flag -->
            <div class="form-control">
              <label class="label cursor-pointer justify-start gap-4">
                <span class="label-text font-medium">Instanceable</span>
                <input
                  v-model="formData.instanceable"
                  type="checkbox"
                  class="toggle toggle-warning"
                />
              </label>
              <label class="label">
                <span class="label-text-alt">Allow players to create instances (copies) of this world</span>
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

      <!-- Access Control Card -->
      <div class="card bg-base-100 shadow-xl">
        <div class="card-body">
          <h3 class="card-title">Access Control</h3>
          <div class="space-y-4">

            <!-- Owner -->
            <div class="form-control">
              <label class="label">
                <span class="label-text font-medium">Owners</span>
              </label>
              <div class="flex flex-wrap gap-2 mb-2">
                <span v-for="userId in formData.owner" :key="userId"
                      class="badge badge-lg badge-primary gap-2">
                  {{ userId }}
                  <button type="button" @click="removeFromSet('owner', userId)"
                          class="btn btn-ghost btn-xs">✕</button>
                </span>
              </div>
              <div class="join w-full">
                <input v-model="newOwner" type="text" placeholder="Add user ID..."
                       class="input input-bordered join-item flex-1"
                       @keyup.enter="addToSet('owner')" />
                <button type="button" @click="addToSet('owner')"
                        class="btn btn-primary join-item">Add</button>
              </div>
            </div>

            <!-- Editor -->
            <div class="form-control">
              <label class="label">
                <span class="label-text font-medium">Editors</span>
              </label>
              <div class="flex flex-wrap gap-2 mb-2">
                <span v-for="userId in formData.editor" :key="userId"
                      class="badge badge-lg badge-secondary gap-2">
                  {{ userId }}
                  <button type="button" @click="removeFromSet('editor', userId)"
                          class="btn btn-ghost btn-xs">✕</button>
                </span>
              </div>
              <div class="join w-full">
                <input v-model="newEditor" type="text" placeholder="Add user ID..."
                       class="input input-bordered join-item flex-1"
                       @keyup.enter="addToSet('editor')" />
                <button type="button" @click="addToSet('editor')"
                        class="btn btn-secondary join-item">Add</button>
              </div>
            </div>

            <!-- Supporter -->
            <div class="form-control">
              <label class="label">
                <span class="label-text font-medium">Supporters</span>
              </label>
              <div class="flex flex-wrap gap-2 mb-2">
                <span v-for="userId in formData.supporter" :key="userId"
                      class="badge badge-lg badge-accent gap-2">
                  {{ userId }}
                  <button type="button" @click="removeFromSet('supporter', userId)"
                          class="btn btn-ghost btn-xs">✕</button>
                </span>
              </div>
              <div class="join w-full">
                <input v-model="newSupporter" type="text" placeholder="Add user ID..."
                       class="input input-bordered join-item flex-1"
                       @keyup.enter="addToSet('supporter')" />
                <button type="button" @click="addToSet('supporter')"
                        class="btn btn-accent join-item">Add</button>
              </div>
            </div>

            <!-- Player -->
            <div class="form-control">
              <label class="label">
                <span class="label-text font-medium">Players</span>
              </label>
              <div class="flex flex-wrap gap-2 mb-2">
                <span v-for="userId in formData.player" :key="userId"
                      :class="userId === '*' ? 'badge badge-lg badge-warning gap-2' : 'badge badge-lg badge-info gap-2'">
                  {{ userId === '*' ? 'All Players (*)' : userId }}
                  <button type="button" @click="removeFromSet('player', userId)"
                          class="btn btn-ghost btn-xs">✕</button>
                </span>
              </div>
              <div class="join w-full">
                <input v-model="newPlayer" type="text" placeholder="Add user ID or '*' for all..."
                       class="input input-bordered join-item flex-1"
                       @keyup.enter="addToSet('player')" />
                <button type="button" @click="addToSet('player')"
                        class="btn btn-info join-item">Add</button>
              </div>
              <label class="label">
                <span class="label-text-alt">Use '*' to allow all players</span>
              </label>
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

// Helper refs for tag inputs
const newOwner = ref('');
const newEditor = ref('');
const newSupporter = ref('');
const newPlayer = ref('');

const formData = ref({
  worldId: '',
  name: '',
  description: '',
  enabled: true,
  publicFlag: false,
  instanceable: false,
  parent: '',
  owner: [] as string[],
  editor: [] as string[],
  supporter: [] as string[],
  player: [] as string[],
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
      instanceable: false,
      parent: '',
      owner: [],
      editor: [],
      supporter: [],
      player: [],
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
    instanceable: world.instanceable,
    parent: world.parent || '',
    owner: world.owner ? [...world.owner] : [],
    editor: world.editor ? [...world.editor] : [],
    supporter: world.supporter ? [...world.supporter] : [],
    player: world.player ? [...world.player] : [],
    groundLevel: world.groundLevel,
    waterLevel: world.waterLevel,
    groundBlockType: world.groundBlockType,
    waterBlockType: world.waterBlockType,
  };
};

// Helper method to add user ID to a permission set
const addToSet = (field: 'owner' | 'editor' | 'supporter' | 'player') => {
  const refMap = {
    owner: newOwner,
    editor: newEditor,
    supporter: newSupporter,
    player: newPlayer
  };

  const value = refMap[field].value.trim();
  if (!value) return;

  if (!formData.value[field].includes(value)) {
    formData.value[field].push(value);
  }

  refMap[field].value = '';
};

// Helper method to remove user ID from a permission set
const removeFromSet = (field: 'owner' | 'editor' | 'supporter' | 'player', userId: string) => {
  const index = formData.value[field].indexOf(userId);
  if (index > -1) {
    formData.value[field].splice(index, 1);
  }
};

// Helper method to format date strings
const formatDate = (dateString: string | undefined): string => {
  if (!dateString) return 'N/A';
  try {
    return new Date(dateString).toLocaleString();
  } catch {
    return 'Invalid date';
  }
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
      instanceable: formData.value.instanceable,
      owner: formData.value.owner.length > 0 ? formData.value.owner : undefined,
      editor: formData.value.editor.length > 0 ? formData.value.editor : undefined,
      supporter: formData.value.supporter.length > 0 ? formData.value.supporter : undefined,
      player: formData.value.player.length > 0 ? formData.value.player : undefined,
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
