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
            <DialogPanel class="w-full max-w-2xl transform overflow-hidden rounded-2xl bg-base-100 p-6 text-left align-middle shadow-xl transition-all">
              <DialogTitle class="text-2xl font-bold mb-4">
                Asset Info
              </DialogTitle>

              <div class="mb-4 p-3 bg-base-200 rounded">
                <p class="text-sm font-mono break-all">{{ assetPath }}</p>
              </div>

              <div class="space-y-4">
                <!-- Description (Required) -->
                <div class="form-control">
                  <label class="label">
                    <span class="label-text font-semibold">Beschreibung *</span>
                  </label>
                  <textarea
                    v-model="localInfo.description"
                    class="textarea textarea-bordered h-24"
                    placeholder="Beschreibung des Assets..."
                  ></textarea>
                </div>

                <!-- Custom Key-Value Pairs -->
                <div class="space-y-2">
                  <label class="label">
                    <span class="label-text font-semibold">Weitere Attribute</span>
                  </label>

                  <div
                    v-for="(value, key) in customFields"
                    :key="key"
                    class="flex gap-2 items-start"
                  >
                    <input
                      v-model="customFieldKeys[key]"
                      type="text"
                      class="input input-bordered input-sm flex-1"
                      placeholder="Key"
                      @blur="handleKeyChange(key, customFieldKeys[key])"
                    />
                    <input
                      v-model="customFields[key]"
                      type="text"
                      class="input input-bordered input-sm flex-1"
                      placeholder="Value"
                    />
                    <button
                      class="btn btn-ghost btn-sm btn-square text-error"
                      @click="removeField(key)"
                    >
                      <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
                      </svg>
                    </button>
                  </div>

                  <button
                    class="btn btn-outline btn-sm"
                    @click="addField"
                  >
                    <svg class="w-4 h-4 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
                    </svg>
                    Attribut hinzufügen
                  </button>
                </div>

                <!-- Loading -->
                <div v-if="loading" class="flex justify-center py-4">
                  <span class="loading loading-spinner loading-lg"></span>
                </div>

                <!-- Error -->
                <div v-if="errorMessage" class="alert alert-error">
                  <svg class="stroke-current flex-shrink-0 h-6 w-6" fill="none" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 14l2-2m0 0l2-2m-2 2l-2-2m2 2l2 2m7-2a9 9 0 11-18 0 9 9 0 0118 0z" />
                  </svg>
                  <span>{{ errorMessage }}</span>
                </div>
              </div>

              <!-- Actions -->
              <div class="mt-6 flex justify-end gap-2">
                <button class="btn btn-ghost" @click="emit('close')" :disabled="loading">
                  Abbrechen
                </button>
                <button
                  class="btn btn-primary"
                  @click="handleSave"
                  :disabled="!localInfo.description.trim() || loading"
                >
                  <span v-if="loading" class="loading loading-spinner loading-sm mr-2"></span>
                  {{ loading ? 'Speichert...' : 'Speichern' }}
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
import { ref, reactive, computed, onMounted } from 'vue';
import { Dialog, DialogPanel, DialogTitle, TransitionRoot, TransitionChild } from '@headlessui/vue';
import { assetInfoService, type AssetInfo } from '@/services/AssetInfoService';

interface Props {
  worldId: string;
  assetPath: string;
}

const props = defineProps<Props>();

const emit = defineEmits<{
  (e: 'close'): void;
  (e: 'saved'): void;
}>();

const localInfo = reactive<AssetInfo>({
  description: '',
});

const customFields = reactive<Record<string, string>>({});
const customFieldKeys = reactive<Record<string, string>>({});
const loading = ref(false);
const errorMessage = ref<string | null>(null);

// Load existing info
onMounted(async () => {
  loading.value = true;
  errorMessage.value = null;

  try {
    const info = await assetInfoService.getAssetInfo(props.worldId, props.assetPath);

    // Set description
    localInfo.description = info.description || '';

    // Extract custom fields
    Object.keys(info).forEach((key) => {
      if (key !== 'description') {
        const value = info[key];
        customFields[key] = String(value);
        customFieldKeys[key] = key;
      }
    });
  } catch (error) {
    errorMessage.value = 'Fehler beim Laden der Info-Datei';
    console.error('Failed to load asset info', error);
  } finally {
    loading.value = false;
  }
});

const addField = () => {
  const newKey = `key${Object.keys(customFields).length + 1}`;
  customFields[newKey] = '';
  customFieldKeys[newKey] = newKey;
};

const removeField = (key: string) => {
  delete customFields[key];
  delete customFieldKeys[key];
};

const handleKeyChange = (oldKey: string, newKey: string) => {
  if (oldKey === newKey) return;
  if (!newKey.trim()) return;

  // Check if new key already exists
  if (newKey in customFields && newKey !== oldKey) {
    errorMessage.value = `Key "${newKey}" existiert bereits`;
    customFieldKeys[oldKey] = oldKey;
    return;
  }

  // Rename key
  const value = customFields[oldKey];
  delete customFields[oldKey];
  customFields[newKey] = value;
  customFieldKeys[newKey] = newKey;
  delete customFieldKeys[oldKey];
};

const handleSave = async () => {
  if (!localInfo.description.trim()) {
    errorMessage.value = 'Beschreibung ist erforderlich';
    return;
  }

  loading.value = true;
  errorMessage.value = null;

  try {
    // Build info object
    const info: AssetInfo = {
      description: localInfo.description.trim(),
    };

    // Add custom fields with proper keys
    Object.keys(customFields).forEach((oldKey) => {
      const actualKey = customFieldKeys[oldKey];
      if (actualKey && actualKey.trim() && customFields[oldKey]) {
        info[actualKey.trim()] = customFields[oldKey];
      }
    });

    await assetInfoService.saveAssetInfo(props.worldId, props.assetPath, info);
    emit('saved');
  } catch (error) {
    errorMessage.value = 'Fehler beim Speichern der Info-Datei';
    console.error('Failed to save asset info', error);
  } finally {
    loading.value = false;
  }
};
</script>
