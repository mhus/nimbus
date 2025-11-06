<template>
  <div class="space-y-3 pt-2">
    <!-- No offsets for INVISIBLE -->
    <div v-if="shape === 0" class="text-sm text-base-content/60">
      No offsets for invisible blocks
    </div>

    <!-- CUBE, HASH, CROSS: 8 corners (24 values) -->
    <div v-else-if="isCubeType" class="space-y-3">
      <p class="text-sm text-base-content/70 mb-2">8 corners × XYZ (supports float values)</p>
      <div v-for="(corner, index) in cubeCorners" :key="index" class="grid grid-cols-4 gap-2 items-center">
        <span class="text-xs text-base-content/70">{{ corner }}:</span>
        <input
          v-model.number="offsets[index * 3]"
          type="number"
          step="0.1"
          class="input input-bordered input-sm"
          placeholder="X"
        />
        <input
          v-model.number="offsets[index * 3 + 1]"
          type="number"
          step="0.1"
          class="input input-bordered input-sm"
          placeholder="Y"
        />
        <input
          v-model.number="offsets[index * 3 + 2]"
          type="number"
          step="0.1"
          class="input input-bordered input-sm"
          placeholder="Z"
        />
      </div>
    </div>

    <!-- COLUMN: 4 points (special meaning) -->
    <div v-else-if="shape === 9" class="space-y-3">
      <p class="text-sm text-base-content/70 mb-2">Column offsets (supports float values)</p>

      <!-- Point 1: Radius offset top (XZ) -->
      <div class="grid grid-cols-3 gap-2 items-center">
        <span class="text-xs text-base-content/70">Radius Top:</span>
        <input
          v-model.number="offsets[0]"
          type="number"
          step="0.1"
          class="input input-bordered input-sm"
          placeholder="X"
        />
        <input
          v-model.number="offsets[2]"
          type="number"
          step="0.1"
          class="input input-bordered input-sm"
          placeholder="Z"
        />
      </div>

      <!-- Point 2: Radius offset bottom (XZ) -->
      <div class="grid grid-cols-3 gap-2 items-center">
        <span class="text-xs text-base-content/70">Radius Bottom:</span>
        <input
          v-model.number="offsets[3]"
          type="number"
          step="0.1"
          class="input input-bordered input-sm"
          placeholder="X"
        />
        <input
          v-model.number="offsets[5]"
          type="number"
          step="0.1"
          class="input input-bordered input-sm"
          placeholder="Z"
        />
      </div>

      <!-- Point 3: Displacement top (XYZ) -->
      <div class="grid grid-cols-4 gap-2 items-center">
        <span class="text-xs text-base-content/70">Disp. Top:</span>
        <input
          v-model.number="offsets[6]"
          type="number"
          step="0.1"
          class="input input-bordered input-sm"
          placeholder="X"
        />
        <input
          v-model.number="offsets[7]"
          type="number"
          step="0.1"
          class="input input-bordered input-sm"
          placeholder="Y"
        />
        <input
          v-model.number="offsets[8]"
          type="number"
          step="0.1"
          class="input input-bordered input-sm"
          placeholder="Z"
        />
      </div>

      <!-- Point 4: Displacement bottom (XYZ) -->
      <div class="grid grid-cols-4 gap-2 items-center">
        <span class="text-xs text-base-content/70">Disp. Bottom:</span>
        <input
          v-model.number="offsets[9]"
          type="number"
          step="0.1"
          class="input input-bordered input-sm"
          placeholder="X"
        />
        <input
          v-model.number="offsets[10]"
          type="number"
          step="0.1"
          class="input input-bordered input-sm"
          placeholder="Y"
        />
        <input
          v-model.number="offsets[11]"
          type="number"
          step="0.1"
          class="input input-bordered input-sm"
          placeholder="Z"
        />
      </div>
    </div>

    <!-- SPHERE: 2 points -->
    <div v-else-if="shape === 8" class="space-y-3">
      <p class="text-sm text-base-content/70 mb-2">Sphere offsets (supports float values)</p>

      <!-- Point 1: Radius offset (XYZ) -->
      <div class="grid grid-cols-4 gap-2 items-center">
        <span class="text-xs text-base-content/70">Radius Offset:</span>
        <input
          v-model.number="offsets[0]"
          type="number"
          step="0.1"
          class="input input-bordered input-sm"
          placeholder="X"
        />
        <input
          v-model.number="offsets[1]"
          type="number"
          step="0.1"
          class="input input-bordered input-sm"
          placeholder="Y"
        />
        <input
          v-model.number="offsets[2]"
          type="number"
          step="0.1"
          class="input input-bordered input-sm"
          placeholder="Z"
        />
      </div>

      <!-- Point 2: Displacement (XYZ) -->
      <div class="grid grid-cols-4 gap-2 items-center">
        <span class="text-xs text-base-content/70">Displacement:</span>
        <input
          v-model.number="offsets[3]"
          type="number"
          step="0.1"
          class="input input-bordered input-sm"
          placeholder="X"
        />
        <input
          v-model.number="offsets[4]"
          type="number"
          step="0.1"
          class="input input-bordered input-sm"
          placeholder="Y"
        />
        <input
          v-model.number="offsets[5]"
          type="number"
          step="0.1"
          class="input input-bordered input-sm"
          placeholder="Z"
        />
      </div>
    </div>

    <!-- FLAT, GLASS_FLAT: 4 corners (12 values) -->
    <div v-else-if="shape === 7 || shape === 6" class="space-y-3">
      <p class="text-sm text-base-content/70 mb-2">4 corners × XYZ (supports float values)</p>
      <div v-for="(corner, index) in flatCorners" :key="index" class="grid grid-cols-4 gap-2 items-center">
        <span class="text-xs text-base-content/70">{{ corner }}:</span>
        <input
          v-model.number="offsets[index * 3]"
          type="number"
          step="0.1"
          class="input input-bordered input-sm"
          placeholder="X"
        />
        <input
          v-model.number="offsets[index * 3 + 1]"
          type="number"
          step="0.1"
          class="input input-bordered input-sm"
          placeholder="Y"
        />
        <input
          v-model.number="offsets[index * 3 + 2]"
          type="number"
          step="0.1"
          class="input input-bordered input-sm"
          placeholder="Z"
        />
      </div>
    </div>

    <!-- THIN_INSTANCES: Single offset (XYZ) -->
    <div v-else-if="shape === 25" class="space-y-3">
      <p class="text-sm text-base-content/70 mb-2">Position offset (supports float values)</p>
      <div class="grid grid-cols-4 gap-2 items-center">
        <span class="text-xs text-base-content/70">Offset:</span>
        <input
          v-model.number="offsets[0]"
          type="number"
          step="0.1"
          class="input input-bordered input-sm"
          placeholder="X"
        />
        <input
          v-model.number="offsets[1]"
          type="number"
          step="0.1"
          class="input input-bordered input-sm"
          placeholder="Y"
        />
        <input
          v-model.number="offsets[2]"
          type="number"
          step="0.1"
          class="input input-bordered input-sm"
          placeholder="Z"
        />
      </div>
    </div>

    <!-- Other shapes: No offsets -->
    <div v-else class="text-sm text-base-content/60">
      No offset configuration for this shape type
    </div>

    <!-- Reset Button -->
    <button
      v-if="hasOffsets"
      class="btn btn-ghost btn-sm btn-outline"
      @click="resetOffsets"
    >
      Reset Offsets
    </button>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue';

interface Props {
  modelValue?: number[];
  shape?: number;
}

const props = withDefaults(defineProps<Props>(), {
  shape: 1, // Default: CUBE
});

const emit = defineEmits<{
  (e: 'update:modelValue', value: number[] | undefined): void;
}>();

// Local copy of offsets
const offsets = ref<number[]>(props.modelValue ? [...props.modelValue] : []);

// Watch offsets and emit changes
watch(offsets, (newValue) => {
  // Clean up trailing zeros
  const trimmed = trimTrailingZeros(newValue);
  emit('update:modelValue', trimmed.length > 0 ? trimmed : undefined);
}, { deep: true });

// Helper to trim trailing zeros
const trimTrailingZeros = (arr: number[]): number[] => {
  let lastNonZero = -1;
  for (let i = arr.length - 1; i >= 0; i--) {
    if (arr[i] !== 0 && arr[i] !== undefined) {
      lastNonZero = i;
      break;
    }
  }
  return lastNonZero >= 0 ? arr.slice(0, lastNonZero + 1) : [];
};

// Check if cube-type shape (CUBE=1, HASH=3, CROSS=2)
const isCubeType = computed(() => {
  return props.shape === 1 || props.shape === 2 || props.shape === 3;
});

// Cube corner labels
const cubeCorners = [
  'bottom front left',
  'bottom front right',
  'bottom back left',
  'bottom back right',
  'top front left',
  'top front right',
  'top back left',
  'top back right',
];

// Flat corner labels
const flatCorners = [
  'front left',
  'front right',
  'back left',
  'back right',
];

const hasOffsets = computed(() => {
  return offsets.value.length > 0 && offsets.value.some(v => v !== 0);
});

const resetOffsets = () => {
  offsets.value = [];
  emit('update:modelValue', undefined);
};

// Watch shape changes to reset offsets if needed
watch(() => props.shape, (newShape, oldShape) => {
  if (newShape !== oldShape) {
    // Shape changed - optionally reset offsets
    // For now, keep them but user can manually reset
  }
});
</script>
