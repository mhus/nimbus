<template>
  <div class="space-y-3 pt-2">
    <!-- Walk Sound -->
    <div class="grid grid-cols-2 gap-2">
      <div class="form-control">
        <label class="label">
          <span class="label-text text-xs">Walk Sound</span>
        </label>
        <input
          v-model="localValue.walk"
          type="text"
          class="input input-bordered input-sm"
          placeholder="sounds/step/stone.wav"
        />
      </div>
      <div class="form-control">
        <label class="label">
          <span class="label-text text-xs">Walk Volume</span>
        </label>
        <input
          v-model.number="localValue.walkVolume"
          type="number"
          step="0.1"
          min="0"
          max="1"
          class="input input-bordered input-sm"
          placeholder="0.0 - 1.0"
        />
      </div>
    </div>

    <!-- Permanent/Ambient Sound -->
    <div class="grid grid-cols-2 gap-2">
      <div class="form-control">
        <label class="label">
          <span class="label-text text-xs">Ambient Sound</span>
        </label>
        <input
          v-model="localValue.permanent"
          type="text"
          class="input input-bordered input-sm"
          placeholder="sounds/ambient/water.wav"
        />
      </div>
      <div class="form-control">
        <label class="label">
          <span class="label-text text-xs">Ambient Volume</span>
        </label>
        <input
          v-model.number="localValue.permanentVolume"
          type="number"
          step="0.1"
          min="0"
          max="1"
          class="input input-bordered input-sm"
          placeholder="0.0 - 1.0"
        />
      </div>
    </div>

    <!-- Status Change Sound -->
    <div class="grid grid-cols-2 gap-2">
      <div class="form-control">
        <label class="label">
          <span class="label-text text-xs">Status Change Sound</span>
        </label>
        <input
          v-model="localValue.changeStatus"
          type="text"
          class="input input-bordered input-sm"
          placeholder="sounds/door/open.wav"
        />
      </div>
      <div class="form-control">
        <label class="label">
          <span class="label-text text-xs">Change Volume</span>
        </label>
        <input
          v-model.number="localValue.changeStatusVolume"
          type="number"
          step="0.1"
          min="0"
          max="1"
          class="input input-bordered input-sm"
          placeholder="0.0 - 1.0"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue';
import type { SoundModifier } from '@nimbus/shared';

interface Props {
  modelValue?: SoundModifier;
}

const props = defineProps<Props>();

const emit = defineEmits<{
  (e: 'update:modelValue', value: SoundModifier | undefined): void;
}>();

const localValue = ref<SoundModifier>(
  props.modelValue ? JSON.parse(JSON.stringify(props.modelValue)) : {}
);

watch(localValue, (newValue) => {
  emit('update:modelValue', newValue);
}, { deep: true });
</script>
