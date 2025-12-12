<template>
  <div class="space-y-4">
    <!-- Header with Search and Actions -->
    <div class="flex flex-col sm:flex-row gap-4 items-stretch sm:items-center justify-between">
      <div class="flex-1">
        <input
          v-model="searchQuery"
          type="text"
          placeholder="Search regions by name..."
          class="input input-bordered w-full"
          @input="handleSearch"
        />
      </div>
      <div class="flex gap-2">
        <button class="btn btn-primary" @click="handleCreate">
          <svg class="w-5 h-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
          </svg>
          Create Region
        </button>
      </div>
    </div>

    <!-- Filter Controls -->
    <div class="flex gap-2">
      <label class="label cursor-pointer gap-2">
        <input
          v-model="filterEnabled"
          type="checkbox"
          class="checkbox checkbox-sm"
        />
        <span class="label-text">Show only enabled</span>
      </label>
    </div>

    <!-- Loading State -->
    <div v-if="loading" class="flex justify-center py-12">
      <span class="loading loading-spinner loading-lg"></span>
    </div>

    <!-- Error State -->
    <div v-else-if="error" class="alert alert-error">
      <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
      </svg>
      <span>{{ error }}</span>
    </div>

    <!-- Empty State -->
    <div v-else-if="!loading && filteredRegions.length === 0" class="text-center py-12">
      <p class="text-base-content/70 text-lg">No regions found</p>
      <p class="text-base-content/50 text-sm mt-2">Create your first region to get started</p>
    </div>

    <!-- Regions Table -->
    <div v-else class="overflow-x-auto">
      <table class="table table-zebra w-full">
        <thead>
          <tr>
            <th>Name</th>
            <th>Status</th>
            <th>Maintainers</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="region in filteredRegions" :key="region.id" class="hover">
            <td class="font-medium">{{ region.name }}</td>
            <td>
              <span
                class="badge"
                :class="region.enabled ? 'badge-success' : 'badge-error'"
              >
                {{ region.enabled ? 'Enabled' : 'Disabled' }}
              </span>
            </td>
            <td>
              <div class="flex flex-wrap gap-1">
                <span
                  v-for="maintainer in region.maintainers"
                  :key="maintainer"
                  class="badge badge-outline badge-sm"
                >
                  {{ maintainer }}
                </span>
                <span v-if="region.maintainers.length === 0" class="text-base-content/50 text-sm">
                  None
                </span>
              </div>
            </td>
            <td>
              <div class="flex gap-2">
                <button
                  class="btn btn-sm btn-primary"
                  @click="handleSelect(region.id)"
                >
                  Edit
                </button>
                <button
                  class="btn btn-sm btn-error"
                  @click="handleDelete(region.id, region.name)"
                >
                  Delete
                </button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { regionService, type Region } from '../services/RegionService';

const emit = defineEmits<{
  select: [id: string];
  create: [];
}>();

const regions = ref<Region[]>([]);
const loading = ref(false);
const error = ref<string | null>(null);
const searchQuery = ref('');
const filterEnabled = ref(false);

const filteredRegions = computed(() => {
  let result = regions.value;

  if (searchQuery.value) {
    const query = searchQuery.value.toLowerCase();
    result = result.filter(r => r.name.toLowerCase().includes(query));
  }

  if (filterEnabled.value) {
    result = result.filter(r => r.enabled);
  }

  return result;
});

const loadRegions = async () => {
  loading.value = true;
  error.value = null;

  try {
    regions.value = await regionService.listRegions();
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Failed to load regions';
    console.error('Failed to load regions:', e);
  } finally {
    loading.value = false;
  }
};

const handleSearch = () => {
  // Search is reactive via computed property
};

const handleCreate = () => {
  emit('create');
};

const handleSelect = (id: string) => {
  emit('select', id);
};

const handleDelete = async (id: string, name: string) => {
  if (!confirm(`Are you sure you want to delete region "${name}"?`)) {
    return;
  }

  try {
    await regionService.deleteRegion(id);
    await loadRegions();
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Failed to delete region';
    console.error('Failed to delete region:', e);
  }
};

onMounted(() => {
  loadRegions();
});
</script>
