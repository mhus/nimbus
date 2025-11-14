<template>
  <div class="space-y-3">
    <!-- Audio List -->
    <div v-if="localValue.audio && localValue.audio.length > 0" class="space-y-2">
      <div
        v-for="(audioDef, index) in localValue.audio"
        :key="index"
        class="border border-base-300 rounded-lg p-3 space-y-3"
      >
        <!-- Audio Header with Remove Button -->
        <div class="flex items-center justify-between">
          <div class="flex items-center gap-2">
            <svg class="w-4 h-4 text-base-content/70" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15.536 8.464a5 5 0 010 7.072m2.828-9.9a9 9 0 010 12.728M5.586 15H4a1 1 0 01-1-1v-4a1 1 0 011-1h1.586l4.707-4.707C10.923 3.663 12 4.109 12 5v14c0 .891-1.077 1.337-1.707.707L5.586 15z" />
            </svg>
            <span class="text-sm font-semibold">Audio #{{ index + 1 }}</span>
            <span class="badge badge-sm">{{ getAudioTypeName(audioDef.type) }}</span>
          </div>
          <button
            class="btn btn-ghost btn-sm btn-square"
            @click="removeAudio(index)"
            title="Remove audio"
          >
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>

        <!-- Type Selection -->
        <div class="form-control">
          <label class="label py-0">
            <span class="label-text text-xs">Type</span>
          </label>
          <select
            v-model="audioDef.type"
            class="select select-bordered select-sm"
          >
            <option v-for="type in audioTypeOptions" :key="type.value" :value="type.value">
              {{ type.label }}
            </option>
          </select>
        </div>

        <!-- Path Input with Asset Picker -->
        <div class="form-control">
          <label class="label py-0">
            <span class="label-text text-xs">Path</span>
          </label>
          <div class="flex gap-2">
            <input
              v-model="audioDef.path"
              type="text"
              class="input input-bordered input-sm flex-1"
              placeholder="audio/steps/stone.ogg"
            />
            <!-- Asset Picker Button -->
            <button
              class="btn btn-ghost btn-sm btn-square"
              @click="openAssetPicker(index)"
              title="Select from assets"
            >
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 21a4 4 0 01-4-4V5a2 2 0 012-2h4a2 2 0 012 2v12a4 4 0 01-4 4zm0 0h12a2 2 0 002-2v-4a2 2 0 00-2-2h-2.343M11 7.343l1.657-1.657a2 2 0 012.828 0l2.829 2.829a2 2 0 010 2.828l-8.486 8.485M7 17h.01" />
              </svg>
            </button>
          </div>
        </div>

        <!-- Volume, Loop, Enabled Controls -->
        <div class="grid grid-cols-3 gap-2">
          <!-- Volume -->
          <div class="form-control">
            <label class="label py-0">
              <span class="label-text text-xs">Volume</span>
            </label>
            <input
              v-model.number="audioDef.volume"
              type="number"
              step="0.1"
              min="0"
              max="1"
              class="input input-bordered input-sm"
              placeholder="1.0"
            />
          </div>

          <!-- Loop -->
          <div class="form-control">
            <label class="label py-0">
              <span class="label-text text-xs">Loop</span>
            </label>
            <div class="flex items-center h-8">
              <input
                v-model="audioDef.loop"
                type="checkbox"
                class="checkbox checkbox-sm"
              />
            </div>
          </div>

          <!-- Enabled -->
          <div class="form-control">
            <label class="label py-0">
              <span class="label-text text-xs">Enabled</span>
            </label>
            <div class="flex items-center h-8">
              <input
                v-model="audioDef.enabled"
                type="checkbox"
                class="checkbox checkbox-sm"
              />
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Empty State -->
    <div v-else class="text-center py-6 text-base-content/50">
      <svg class="w-12 h-12 mx-auto mb-2 opacity-50" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15.536 8.464a5 5 0 010 7.072m2.828-9.9a9 9 0 010 12.728M5.586 15H4a1 1 0 01-1-1v-4a1 1 0 011-1h1.586l4.707-4.707C10.923 3.663 12 4.109 12 5v14c0 .891-1.077 1.337-1.707.707L5.586 15z" />
      </svg>
      <p class="text-sm">No audio files added</p>
    </div>

    <!-- Add Audio Button -->
    <button
      class="btn btn-outline btn-sm w-full"
      @click="addAudio"
    >
      <svg class="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
      </svg>
      Add Audio
    </button>
  </div>

  <!-- Asset Picker Dialog -->
  <AssetPickerDialog
    v-if="isAssetPickerOpen"
    :world-id="worldId"
    :current-path="getSelectedAudioPath()"
    @close="closeAssetPicker"
    @select="handleAssetSelected"
  />
</template>

<script setup lang="ts">
import { ref, watch } from 'vue';
import type { AudioModifier, AudioDefinition } from '@nimbus/shared';
import { AudioType } from '@nimbus/shared';
import AssetPickerDialog from '@components/AssetPickerDialog.vue';

interface Props {
  modelValue?: AudioModifier;
  worldId?: string;
}

const props = defineProps<Props>();

const emit = defineEmits<{
  (e: 'update:modelValue', value: AudioModifier | undefined): void;
}>();

const localValue = ref<AudioModifier>(
  props.modelValue ? JSON.parse(JSON.stringify(props.modelValue)) : { audio: [] }
);

// Audio type options for dropdown
const audioTypeOptions = [
  { value: AudioType.STEPS, label: 'Steps' },
];

// Asset picker state
const isAssetPickerOpen = ref(false);
const selectedAudioIndex = ref<number | null>(null);

// Get audio type display name
const getAudioTypeName = (type: AudioType): string => {
  const option = audioTypeOptions.find(opt => opt.value === type);
  return option?.label || type;
};

// Add new audio definition
const addAudio = () => {
  if (!localValue.value.audio) {
    localValue.value.audio = [];
  }
  const newAudio: AudioDefinition = {
    type: AudioType.STEPS,
    path: '',
    volume: 1.0,
    loop: false,
    enabled: true,
  };
  localValue.value.audio.push(newAudio);
};

// Remove audio definition
const removeAudio = (index: number) => {
  if (localValue.value.audio) {
    localValue.value.audio.splice(index, 1);
  }
};

// Asset picker functions
const openAssetPicker = (index: number) => {
  selectedAudioIndex.value = index;
  isAssetPickerOpen.value = true;
};

const closeAssetPicker = () => {
  isAssetPickerOpen.value = false;
  selectedAudioIndex.value = null;
};

const getSelectedAudioPath = (): string => {
  if (selectedAudioIndex.value !== null && localValue.value.audio) {
    return localValue.value.audio[selectedAudioIndex.value]?.path || '';
  }
  return '';
};

const handleAssetSelected = (assetPath: string) => {
  if (selectedAudioIndex.value !== null && localValue.value.audio) {
    localValue.value.audio[selectedAudioIndex.value].path = assetPath;
  }
  closeAssetPicker();
};

// Watch for changes and emit
watch(localValue, (newValue) => {
  emit('update:modelValue', newValue);
}, { deep: true });
</script>
