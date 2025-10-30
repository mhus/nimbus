<template>
  <div class="space-y-3 pt-2">
    <div class="form-control">
      <label class="label cursor-pointer justify-start gap-2">
        <input
          v-model="localValue.forceEgoView"
          type="checkbox"
          class="checkbox checkbox-sm"
        />
        <span class="label-text">Force Ego/First-Person View</span>
      </label>
    </div>

    <!-- Sky Effect (nested) -->
    <div class="card bg-base-300">
      <div class="card-body p-3">
        <h4 class="font-semibold text-sm mb-2">Sky Effect</h4>
        <div class="space-y-2">
          <div class="form-control">
            <label class="label">
              <span class="label-text text-xs">Intensity</span>
            </label>
            <input
              v-model.number="skyEffect.intensity"
              type="number"
              step="0.1"
              min="0"
              max="1"
              class="input input-bordered input-sm"
              placeholder="0.0 - 1.0"
            />
          </div>

          <div class="form-control">
            <label class="label">
              <span class="label-text text-xs">Color</span>
            </label>
            <input
              v-model="skyEffect.color"
              type="text"
              class="input input-bordered input-sm"
              placeholder="#RRGGBB or color name"
            />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue';
import type { EffectsModifier } from '@nimbus/shared';

interface Props {
  modelValue?: EffectsModifier;
}

const props = defineProps<Props>();

const emit = defineEmits<{
  (e: 'update:modelValue', value: EffectsModifier | undefined): void;
}>();

const localValue = ref<EffectsModifier>(props.modelValue || {});

const skyEffect = computed({
  get: () => localValue.value.sky || {},
  set: (value) => {
    if (Object.keys(value).length > 0) {
      localValue.value.sky = value;
    } else {
      localValue.value.sky = undefined;
    }
  }
});

watch(localValue, (newValue) => {
  emit('update:modelValue', newValue);
}, { deep: true });

watch(() => props.modelValue, (newValue) => {
  if (newValue) {
    localValue.value = JSON.parse(JSON.stringify(newValue));
  }
}, { deep: true });
</script>
