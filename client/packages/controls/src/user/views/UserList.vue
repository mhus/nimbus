<template>
  <div class="space-y-4">
    <!-- Header with Search -->
    <div class="flex flex-col sm:flex-row gap-4 items-stretch sm:items-center justify-between">
      <div class="flex-1">
        <input
          v-model="searchQuery"
          type="text"
          placeholder="Search users by username or email..."
          class="input input-bordered w-full"
          @input="handleSearch"
        />
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
    <div v-else-if="!loading && filteredUsers.length === 0" class="text-center py-12">
      <p class="text-base-content/70 text-lg">No users found</p>
    </div>

    <!-- Users Table -->
    <div v-else class="overflow-x-auto">
      <table class="table table-zebra w-full">
        <thead>
          <tr>
            <th>Username</th>
            <th>Email</th>
            <th>Status</th>
            <th>Sector Roles</th>
            <th>Settings</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="user in filteredUsers" :key="user.id" class="hover">
            <td class="font-medium">{{ user.username }}</td>
            <td>{{ user.email }}</td>
            <td>
              <span
                class="badge"
                :class="user.enabled ? 'badge-success' : 'badge-error'"
              >
                {{ user.enabled ? 'Enabled' : 'Disabled' }}
              </span>
            </td>
            <td>
              <div class="flex flex-wrap gap-1">
                <span
                  v-for="role in user.sectorRoles"
                  :key="role"
                  class="badge badge-outline badge-sm"
                >
                  {{ role }}
                </span>
                <span v-if="user.sectorRoles.length === 0" class="text-base-content/50 text-sm">
                  None
                </span>
              </div>
            </td>
            <td>
              <span class="badge badge-info badge-sm">
                {{ Object.keys(user.userSettings || {}).length }} types
              </span>
            </td>
            <td>
              <div class="flex gap-2">
                <button
                  class="btn btn-sm btn-primary"
                  @click="handleSelect(user.username)"
                >
                  Edit
                </button>
                <button
                  class="btn btn-sm btn-error"
                  @click="handleDelete(user.username)"
                >
                  Disable
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
import { userService, type User } from '../services/UserService';

const emit = defineEmits<{
  select: [username: string];
}>();

const users = ref<User[]>([]);
const loading = ref(false);
const error = ref<string | null>(null);
const searchQuery = ref('');
const filterEnabled = ref(false);

const filteredUsers = computed(() => {
  let result = users.value;

  if (searchQuery.value) {
    const query = searchQuery.value.toLowerCase();
    result = result.filter(u =>
      u.username.toLowerCase().includes(query) ||
      u.email.toLowerCase().includes(query)
    );
  }

  if (filterEnabled.value) {
    result = result.filter(u => u.enabled);
  }

  return result;
});

const loadUsers = async () => {
  loading.value = true;
  error.value = null;

  try {
    users.value = await userService.listUsers();
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Failed to load users';
    console.error('Failed to load users:', e);
  } finally {
    loading.value = false;
  }
};

const handleSearch = () => {
  // Search is reactive via computed property
};

const handleSelect = (username: string) => {
  emit('select', username);
};

const handleDelete = async (username: string) => {
  if (!confirm(`Are you sure you want to disable user "${username}"?`)) {
    return;
  }

  try {
    await userService.deleteUser(username);
    await loadUsers();
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Failed to disable user';
    console.error('Failed to disable user:', e);
  }
};

onMounted(() => {
  loadUsers();
});
</script>
