<template>
  <div class="space-y-4 pt-2">
    <!-- Shape Selection -->
    <div class="form-control">
      <label class="label">
        <span class="label-text font-semibold">Shape</span>
      </label>
      <select v-model.number="localValue.shape" class="select select-bordered select-sm">
        <option v-for="(name, value) in shapeOptions" :key="value" :value="parseInt(value)">
          {{ name }} ({{ value }})
        </option>
      </select>
    </div>

    <!-- Model Path (for MODEL shape) -->
    <div v-if="localValue.shape === 4" class="form-control">
      <label class="label">
        <span class="label-text">Model Path</span>
      </label>
      <input
        v-model="localValue.path"
        type="text"
        class="input input-bordered input-sm"
        placeholder="models/custom/my_model.obj"
      />
    </div>

    <!-- Scaling -->
    <div class="grid grid-cols-3 gap-2">
      <div class="form-control">
        <label class="label">
          <span class="label-text text-xs">Scale X</span>
        </label>
        <input
          v-model.number="localValue.scalingX"
          type="number"
          step="0.1"
          class="input input-bordered input-sm"
          placeholder="1.0"
        />
      </div>
      <div class="form-control">
        <label class="label">
          <span class="label-text text-xs">Scale Y</span>
        </label>
        <input
          v-model.number="localValue.scalingY"
          type="number"
          step="0.1"
          class="input input-bordered input-sm"
          placeholder="1.0"
        />
      </div>
      <div class="form-control">
        <label class="label">
          <span class="label-text text-xs">Scale Z</span>
        </label>
        <input
          v-model.number="localValue.scalingZ"
          type="number"
          step="0.1"
          class="input input-bordered input-sm"
          placeholder="1.0"
        />
      </div>
    </div>

    <!-- Rotation -->
    <div class="grid grid-cols-2 gap-2">
      <div class="form-control">
        <label class="label">
          <span class="label-text text-xs">Rotation X (degrees)</span>
        </label>
        <input
          v-model.number="localValue.rotationX"
          type="number"
          class="input input-bordered input-sm"
          placeholder="0"
        />
      </div>
      <div class="form-control">
        <label class="label">
          <span class="label-text text-xs">Rotation Y (degrees)</span>
        </label>
        <input
          v-model.number="localValue.rotationY"
          type="number"
          class="input input-bordered input-sm"
          placeholder="0"
        />
      </div>
    </div>

    <!-- Textures -->
    <div class="divider text-sm">Textures</div>
    <div class="space-y-2">
      <div v-for="(name, key) in textureKeyOptions" :key="key" class="flex items-center gap-2">
        <span class="text-sm w-24 text-base-content/70">{{ name }}:</span>
        <input
          :value="getTextureValue(parseInt(key))"
          @input="setTextureValue(parseInt(key), ($event.target as HTMLInputElement).value)"
          type="text"
          class="input input-bordered input-sm flex-1"
          :placeholder="`textures/block/my_${name}.png`"
        />
        <button
          v-if="getTextureValue(parseInt(key))"
          class="btn btn-ghost btn-sm btn-square"
          @click="removeTexture(parseInt(key))"
        >
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
          </svg>
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue';
import type { VisibilityModifier } from '@nimbus/shared';
import { Shape, ShapeNames, TextureKey, TextureKeyNames } from '@nimbus/shared';
import CollapsibleSection from '../CollapsibleSection.vue';

interface Props {
  modelValue?: VisibilityModifier;
}

const props = defineProps<Props>();

const emit = defineEmits<{
  (e: 'update:modelValue', value: VisibilityModifier | undefined): void;
}>();

const localValue = ref<VisibilityModifier>(
  props.modelValue ? JSON.parse(JSON.stringify(props.modelValue)) : { shape: 1, textures: {} }
);

// Only watch localValue changes to emit updates (one-way)
watch(localValue, (newValue) => {
  emit('update:modelValue', newValue);
}, { deep: true });

// Shape options
const shapeOptions = ShapeNames;

// Texture key options (only common ones for UI)
const textureKeyOptions: Record<number, string> = {
  [TextureKey.ALL]: 'all',
  [TextureKey.TOP]: 'top',
  [TextureKey.BOTTOM]: 'bottom',
  [TextureKey.LEFT]: 'left',
  [TextureKey.RIGHT]: 'right',
  [TextureKey.FRONT]: 'front',
  [TextureKey.BACK]: 'back',
  [TextureKey.SIDE]: 'side',
  [TextureKey.DIFFUSE]: 'diffuse',
  [TextureKey.DISTORTION]: 'distortion',
  [TextureKey.OPACITY]: 'opacity',
};

const getTextureValue = (key: number): string => {
  if (!localValue.value.textures) return '';
  const texture = localValue.value.textures[key];
  if (!texture) return '';
  return typeof texture === 'string' ? texture : texture.path;
};

const setTextureValue = (key: number, value: string) => {
  if (!localValue.value.textures) {
    localValue.value.textures = {};
  }
  if (value.trim()) {
    localValue.value.textures[key] = value.trim();
  } else {
    delete localValue.value.textures[key];
  }
};

const removeTexture = (key: number) => {
  if (localValue.value.textures) {
    delete localValue.value.textures[key];
  }
};
</script>
