<template>
  <div class="space-y-3 pt-2">
    <div class="form-control">
      <label class="label">
        <span class="label-text">Color</span>
      </label>
      <input
        v-model="localValue.color"
        type="text"
        class="input input-bordered input-sm"
        placeholder="#RRGGBB or color name"
      />
    </div>

    <div class="form-control">
      <label class="label">
        <span class="label-text">Strength</span>
      </label>
      <input
        v-model.number="localValue.strength"
        type="number"
        step="0.1"
        min="0"
        class="input input-bordered input-sm"
        placeholder="Light strength"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue';
import type { IlluminationModifier } from '@nimbus/shared';

interface Props {
  modelValue?: IlluminationModifier;
}

const props = defineProps<Props>();

const emit = defineEmits<{
  (e: 'update:modelValue', value: IlluminationModifier | undefined): void;
}>();

const localValue = ref<IlluminationModifier>(props.modelValue || {});

watch(localValue, (newValue) => {
  emit('update:modelValue', newValue);
}, { deep: true });

watch(() => props.modelValue, (newValue) => {
  if (newValue) {
    localValue.value = JSON.parse(JSON.stringify(newValue));
  }
}, { deep: true });
</script>
