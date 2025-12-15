<template>
  <div class="space-y-6">
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
      <h2 class="text-2xl font-bold">Edit User</h2>
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

    <!-- Edit Form -->
    <div v-else class="space-y-6">
      <!-- Basic Info Card -->
      <div class="card bg-base-100 shadow-xl">
        <div class="card-body">
          <h3 class="card-title">Basic Information</h3>
          <form @submit.prevent="handleSave" class="space-y-4">
            <!-- Username (readonly) -->
            <div class="form-control">
              <label class="label">
                <span class="label-text font-medium">Username</span>
              </label>
              <input
                :value="user?.username"
                type="text"
                class="input input-bordered w-full"
                disabled
              />
            </div>

            <!-- Email -->
            <div class="form-control">
              <label class="label">
                <span class="label-text font-medium">Email</span>
              </label>
              <input
                v-model="formData.email"
                type="email"
                placeholder="Enter email"
                class="input input-bordered w-full"
                required
              />
            </div>

            <!-- Sector Roles -->
            <div class="form-control">
              <label class="label">
                <span class="label-text font-medium">Sector Roles</span>
              </label>
              <input
                v-model="formData.sectorRolesRaw"
                type="text"
                placeholder="Enter roles separated by commas (e.g., PLAYER, ADMIN)"
                class="input input-bordered w-full"
              />
              <label class="label">
                <span class="label-text-alt">Comma-separated list of sector roles</span>
              </label>
            </div>

            <!-- Current Roles Display -->
            <div v-if="user?.sectorRoles && user.sectorRoles.length > 0" class="form-control">
              <label class="label">
                <span class="label-text font-medium">Current Roles</span>
              </label>
              <div class="flex flex-wrap gap-2">
                <span
                  v-for="role in user.sectorRoles"
                  :key="role"
                  class="badge badge-lg badge-outline"
                >
                  {{ role }}
                </span>
              </div>
            </div>

            <!-- Action Buttons -->
            <div class="card-actions justify-end mt-6">
              <button type="button" class="btn btn-ghost" @click="handleBack">
                Cancel
              </button>
              <button type="submit" class="btn btn-primary" :disabled="saving">
                <span v-if="saving" class="loading loading-spinner loading-sm"></span>
                <span v-else>Save</span>
              </button>
            </div>
          </form>
        </div>
      </div>

      <!-- User Settings Card -->
      <div class="card bg-base-100 shadow-xl">
        <div class="card-body">
          <h3 class="card-title">User Settings</h3>

          <!-- Add New Setting -->
          <div class="flex gap-2 mb-4">
            <input
              v-model="newSettingClientType"
              type="text"
              placeholder="Client Type (e.g., web, mobile)"
              class="input input-bordered flex-1"
            />
            <button
              type="button"
              class="btn btn-secondary"
              @click="handleAddSetting"
              :disabled="!newSettingClientType.trim()"
            >
              Add Setting
            </button>
          </div>

          <!-- Existing Settings -->
          <div v-if="user?.userSettings && Object.keys(user.userSettings).length > 0" class="space-y-4">
            <div
              v-for="(settings, clientType) in user.userSettings"
              :key="clientType"
              class="border border-base-300 rounded-lg p-4"
            >
              <div class="flex items-center justify-between mb-2">
                <h4 class="font-bold">{{ clientType }}</h4>
                <div class="flex gap-2">
                  <button
                    type="button"
                    class="btn btn-sm btn-ghost"
                    @click="toggleSettingEdit(clientType)"
                  >
                    {{ editingSettings[clientType] ? 'Cancel' : 'Edit' }}
                  </button>
                  <button
                    type="button"
                    class="btn btn-sm btn-error"
                    @click="handleDeleteSetting(clientType)"
                  >
                    Delete
                  </button>
                </div>
              </div>

              <!-- Display Mode -->
              <div v-if="!editingSettings[clientType]">
                <pre class="bg-base-200 p-2 rounded text-sm overflow-x-auto">{{ JSON.stringify(settings, null, 2) }}</pre>
              </div>

              <!-- Edit Mode -->
              <div v-else>
                <textarea
                  v-model="settingsEditors[clientType]"
                  class="textarea textarea-bordered w-full font-mono text-sm"
                  rows="10"
                  placeholder="Enter JSON settings"
                ></textarea>
                <div class="flex justify-end gap-2 mt-2">
                  <button
                    type="button"
                    class="btn btn-sm btn-primary"
                    @click="handleSaveSetting(clientType)"
                    :disabled="saving"
                  >
                    <span v-if="saving" class="loading loading-spinner loading-xs"></span>
                    <span v-else>Save</span>
                  </button>
                </div>
              </div>
            </div>
          </div>

          <!-- Empty State -->
          <div v-else class="text-center py-8">
            <p class="text-base-content/70">No settings configured</p>
            <p class="text-base-content/50 text-sm mt-2">Add a setting by entering a client type above</p>
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
import { ref, reactive, onMounted } from 'vue';
import { userService, type User, type Settings } from '../services/UserService';

const props = defineProps<{
  username: string;
}>();

const emit = defineEmits<{
  back: [];
  saved: [];
}>();

const user = ref<User | null>(null);
const loading = ref(false);
const saving = ref(false);
const error = ref<string | null>(null);
const successMessage = ref<string | null>(null);

const formData = ref({
  email: '',
  sectorRolesRaw: '',
});

const newSettingClientType = ref('');
const editingSettings = reactive<Record<string, boolean>>({});
const settingsEditors = reactive<Record<string, string>>({});

const loadUser = async () => {
  loading.value = true;
  error.value = null;

  try {
    user.value = await userService.getUser(props.username);
    formData.value = {
      email: user.value.email,
      sectorRolesRaw: user.value.sectorRoles.join(', '),
    };
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Failed to load user';
    console.error('Failed to load user:', e);
  } finally {
    loading.value = false;
  }
};

const handleSave = async () => {
  saving.value = true;
  error.value = null;
  successMessage.value = null;

  try {
    await userService.updateUser(props.username, {
      username: props.username,
      email: formData.value.email,
      sectorRolesRaw: formData.value.sectorRolesRaw,
    });
    successMessage.value = 'User updated successfully';
    await loadUser();
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Failed to save user';
    console.error('Failed to save user:', e);
  } finally {
    saving.value = false;
  }
};

const handleAddSetting = () => {
  if (!newSettingClientType.value.trim()) {
    return;
  }

  const clientType = newSettingClientType.value.trim();
  editingSettings[clientType] = true;
  settingsEditors[clientType] = JSON.stringify({}, null, 2);
  newSettingClientType.value = '';
};

const toggleSettingEdit = (clientType: string) => {
  if (editingSettings[clientType]) {
    delete editingSettings[clientType];
    delete settingsEditors[clientType];
  } else {
    editingSettings[clientType] = true;
    const currentSettings = user.value?.userSettings?.[clientType] || {};
    settingsEditors[clientType] = JSON.stringify(currentSettings, null, 2);
  }
};

const handleSaveSetting = async (clientType: string) => {
  saving.value = true;
  error.value = null;
  successMessage.value = null;

  try {
    const settingsJson = settingsEditors[clientType];
    const settings = JSON.parse(settingsJson);

    await userService.updateSettingsForClientType(props.username, clientType, settings);
    successMessage.value = `Settings for "${clientType}" updated successfully`;

    delete editingSettings[clientType];
    delete settingsEditors[clientType];

    await loadUser();
  } catch (e) {
    if (e instanceof SyntaxError) {
      error.value = 'Invalid JSON format';
    } else {
      error.value = e instanceof Error ? e.message : 'Failed to save settings';
    }
    console.error('Failed to save settings:', e);
  } finally {
    saving.value = false;
  }
};

const handleDeleteSetting = async (clientType: string) => {
  if (!confirm(`Are you sure you want to delete settings for "${clientType}"?`)) {
    return;
  }

  saving.value = true;
  error.value = null;
  successMessage.value = null;

  try {
    await userService.deleteSettingsForClientType(props.username, clientType);
    successMessage.value = `Settings for "${clientType}" deleted successfully`;
    await loadUser();
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Failed to delete settings';
    console.error('Failed to delete settings:', e);
  } finally {
    saving.value = false;
  }
};

const handleBack = () => {
  emit('back');
};

onMounted(() => {
  loadUser();
});
</script>
