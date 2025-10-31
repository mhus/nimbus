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

    <!-- Offsets -->
    <div class="divider text-sm">Geometry Offsets</div>
    <OffsetsEditor
      v-model="localValue.offsets"
      :shape="localValue.shape"
    />

    <!-- Textures -->
    <div class="divider text-sm">Textures</div>
    <div class="space-y-3">
      <div v-for="(name, key) in textureKeyOptions" :key="key" class="space-y-2">
        <!-- Texture Row -->
        <div class="flex items-center gap-2">
          <span class="text-sm w-24 text-base-content/70">{{ name }}:</span>
          <input
            :value="getTexturePathValue(parseInt(key))"
            @input="setTexturePath(parseInt(key), ($event.target as HTMLInputElement).value)"
            type="text"
            class="input input-bordered input-sm flex-1"
            :placeholder="`textures/block/my_${name}.png`"
          />
          <!-- Asset Picker Button -->
          <button
            class="btn btn-ghost btn-sm btn-square"
            @click="openAssetPicker(parseInt(key))"
            title="Select from assets"
          >
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 21a4 4 0 01-4-4V5a2 2 0 012-2h4a2 2 0 012 2v12a4 4 0 01-4 4zm0 0h12a2 2 0 002-2v-4a2 2 0 00-2-2h-2.343M11 7.343l1.657-1.657a2 2 0 012.828 0l2.829 2.829a2 2 0 010 2.828l-8.486 8.485M7 17h.01" />
            </svg>
          </button>
          <!-- Expand/Collapse Button -->
          <button
            v-if="getTexturePathValue(parseInt(key))"
            class="btn btn-ghost btn-sm btn-square"
            @click="toggleTextureExpansion(parseInt(key))"
            :title="isTextureExpanded(parseInt(key)) ? 'Hide advanced settings' : 'Show advanced settings'"
          >
            <svg
              class="w-4 h-4 transition-transform"
              :class="{ 'rotate-180': isTextureExpanded(parseInt(key)) }"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7" />
            </svg>
          </button>
          <!-- Remove Button -->
          <button
            v-if="getTexturePathValue(parseInt(key))"
            class="btn btn-ghost btn-sm btn-square"
            @click="removeTexture(parseInt(key))"
            title="Remove texture"
          >
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>

        <!-- Expanded Settings -->
        <div
          v-if="isTextureExpanded(parseInt(key))"
          class="ml-28 pl-4 border-l-2 border-base-300 space-y-3"
        >
          <!-- Atlas Extraction -->
          <div>
            <div class="text-xs font-semibold text-base-content/70 mb-2">Atlas Extraction (Source Region)</div>
            <div class="grid grid-cols-4 gap-2">
              <div class="form-control">
                <label class="label py-0">
                  <span class="label-text text-xs">X (px)</span>
                </label>
                <input
                  :value="getTextureDefValue(parseInt(key), 'uvMapping.x')"
                  @input="setTextureDefValue(parseInt(key), 'uvMapping.x', parseFloat(($event.target as HTMLInputElement).value))"
                  type="number"
                  class="input input-bordered input-xs"
                  placeholder="0"
                />
              </div>
              <div class="form-control">
                <label class="label py-0">
                  <span class="label-text text-xs">Y (px)</span>
                </label>
                <input
                  :value="getTextureDefValue(parseInt(key), 'uvMapping.y')"
                  @input="setTextureDefValue(parseInt(key), 'uvMapping.y', parseFloat(($event.target as HTMLInputElement).value))"
                  type="number"
                  class="input input-bordered input-xs"
                  placeholder="0"
                />
              </div>
              <div class="form-control">
                <label class="label py-0">
                  <span class="label-text text-xs">W (px)</span>
                </label>
                <input
                  :value="getTextureDefValue(parseInt(key), 'uvMapping.w')"
                  @input="setTextureDefValue(parseInt(key), 'uvMapping.w', parseFloat(($event.target as HTMLInputElement).value))"
                  type="number"
                  class="input input-bordered input-xs"
                  placeholder="16"
                />
              </div>
              <div class="form-control">
                <label class="label py-0">
                  <span class="label-text text-xs">H (px)</span>
                </label>
                <input
                  :value="getTextureDefValue(parseInt(key), 'uvMapping.h')"
                  @input="setTextureDefValue(parseInt(key), 'uvMapping.h', parseFloat(($event.target as HTMLInputElement).value))"
                  type="number"
                  class="input input-bordered input-xs"
                  placeholder="16"
                />
              </div>
            </div>
          </div>

          <!-- UV Transformation - Tiling -->
          <div>
            <div class="text-xs font-semibold text-base-content/70 mb-2">UV Tiling (Scale)</div>
            <div class="grid grid-cols-2 gap-2">
              <div class="form-control">
                <label class="label py-0">
                  <span class="label-text text-xs">uScale</span>
                </label>
                <input
                  :value="getTextureDefValue(parseInt(key), 'uvMapping.uScale')"
                  @input="setTextureDefValue(parseInt(key), 'uvMapping.uScale', parseFloat(($event.target as HTMLInputElement).value))"
                  type="number"
                  step="0.1"
                  class="input input-bordered input-xs"
                  placeholder="1.0"
                />
              </div>
              <div class="form-control">
                <label class="label py-0">
                  <span class="label-text text-xs">vScale</span>
                </label>
                <input
                  :value="getTextureDefValue(parseInt(key), 'uvMapping.vScale')"
                  @input="setTextureDefValue(parseInt(key), 'uvMapping.vScale', parseFloat(($event.target as HTMLInputElement).value))"
                  type="number"
                  step="0.1"
                  class="input input-bordered input-xs"
                  placeholder="1.0"
                />
              </div>
            </div>
          </div>

          <!-- UV Transformation - Offset -->
          <div>
            <div class="text-xs font-semibold text-base-content/70 mb-2">UV Offset</div>
            <div class="grid grid-cols-2 gap-2">
              <div class="form-control">
                <label class="label py-0">
                  <span class="label-text text-xs">uOffset</span>
                </label>
                <input
                  :value="getTextureDefValue(parseInt(key), 'uvMapping.uOffset')"
                  @input="setTextureDefValue(parseInt(key), 'uvMapping.uOffset', parseFloat(($event.target as HTMLInputElement).value))"
                  type="number"
                  step="0.1"
                  class="input input-bordered input-xs"
                  placeholder="0.0"
                />
              </div>
              <div class="form-control">
                <label class="label py-0">
                  <span class="label-text text-xs">vOffset</span>
                </label>
                <input
                  :value="getTextureDefValue(parseInt(key), 'uvMapping.vOffset')"
                  @input="setTextureDefValue(parseInt(key), 'uvMapping.vOffset', parseFloat(($event.target as HTMLInputElement).value))"
                  type="number"
                  step="0.1"
                  class="input input-bordered input-xs"
                  placeholder="0.0"
                />
              </div>
            </div>
          </div>

          <!-- UV Transformation - Wrap Mode -->
          <div>
            <div class="text-xs font-semibold text-base-content/70 mb-2">Wrap Mode</div>
            <div class="grid grid-cols-2 gap-2">
              <div class="form-control">
                <label class="label py-0">
                  <span class="label-text text-xs">wrapU</span>
                </label>
                <select
                  :value="getTextureDefValue(parseInt(key), 'uvMapping.wrapU') ?? 1"
                  @change="setTextureDefValue(parseInt(key), 'uvMapping.wrapU', parseInt(($event.target as HTMLSelectElement).value))"
                  class="select select-bordered select-xs"
                >
                  <option :value="0">CLAMP</option>
                  <option :value="1">REPEAT</option>
                  <option :value="2">MIRROR</option>
                </select>
              </div>
              <div class="form-control">
                <label class="label py-0">
                  <span class="label-text text-xs">wrapV</span>
                </label>
                <select
                  :value="getTextureDefValue(parseInt(key), 'uvMapping.wrapV') ?? 1"
                  @change="setTextureDefValue(parseInt(key), 'uvMapping.wrapV', parseInt(($event.target as HTMLSelectElement).value))"
                  class="select select-bordered select-xs"
                >
                  <option :value="0">CLAMP</option>
                  <option :value="1">REPEAT</option>
                  <option :value="2">MIRROR</option>
                </select>
              </div>
            </div>
          </div>

          <!-- UV Transformation - Rotation Center -->
          <div>
            <div class="text-xs font-semibold text-base-content/70 mb-2">Rotation Center</div>
            <div class="grid grid-cols-2 gap-2">
              <div class="form-control">
                <label class="label py-0">
                  <span class="label-text text-xs">uRotationCenter</span>
                </label>
                <input
                  :value="getTextureDefValue(parseInt(key), 'uvMapping.uRotationCenter')"
                  @input="setTextureDefValue(parseInt(key), 'uvMapping.uRotationCenter', parseFloat(($event.target as HTMLInputElement).value))"
                  type="number"
                  step="0.1"
                  class="input input-bordered input-xs"
                  placeholder="0.5"
                />
              </div>
              <div class="form-control">
                <label class="label py-0">
                  <span class="label-text text-xs">vRotationCenter</span>
                </label>
                <input
                  :value="getTextureDefValue(parseInt(key), 'uvMapping.vRotationCenter')"
                  @input="setTextureDefValue(parseInt(key), 'uvMapping.vRotationCenter', parseFloat(($event.target as HTMLInputElement).value))"
                  type="number"
                  step="0.1"
                  class="input input-bordered input-xs"
                  placeholder="0.5"
                />
              </div>
            </div>
          </div>

          <!-- Texture Rotation -->
          <div>
            <div class="text-xs font-semibold text-base-content/70 mb-2">Texture Rotation</div>
            <select
              :value="getTextureDefValue(parseInt(key), 'rotation') ?? 0"
              @change="setTextureDefValue(parseInt(key), 'rotation', parseInt(($event.target as HTMLSelectElement).value))"
              class="select select-bordered select-xs w-full"
            >
              <option :value="0">0°</option>
              <option :value="1">90°</option>
              <option :value="2">180°</option>
              <option :value="3">270°</option>
              <option :value="4">Flip 0°</option>
              <option :value="5">Flip 90°</option>
              <option :value="6">Flip 180°</option>
              <option :value="7">Flip 270°</option>
            </select>
          </div>

          <!-- Sampling Mode -->
          <div>
            <div class="text-xs font-semibold text-base-content/70 mb-2">Sampling Mode</div>
            <select
              :value="getTextureDefValue(parseInt(key), 'samplingMode') ?? 0"
              @change="setTextureDefValue(parseInt(key), 'samplingMode', parseInt(($event.target as HTMLSelectElement).value))"
              class="select select-bordered select-xs w-full"
            >
              <option :value="0">NEAREST</option>
              <option :value="1">LINEAR</option>
              <option :value="2">MIPMAP</option>
            </select>
          </div>

          <!-- Transparency Mode -->
          <div>
            <div class="text-xs font-semibold text-base-content/70 mb-2">Transparency Mode</div>
            <select
              :value="getTextureDefValue(parseInt(key), 'transparencyMode') ?? 0"
              @change="setTextureDefValue(parseInt(key), 'transparencyMode', parseInt(($event.target as HTMLSelectElement).value))"
              class="select select-bordered select-xs w-full"
            >
              <option :value="0">NONE</option>
              <option :value="1">HAS_ALPHA</option>
              <option :value="2">ALPHA_FROM_RGB</option>
            </select>
          </div>

          <!-- Opacity -->
          <div>
            <div class="text-xs font-semibold text-base-content/70 mb-2">Opacity</div>
            <input
              :value="getTextureDefValue(parseInt(key), 'opacity')"
              @input="setTextureDefValue(parseInt(key), 'opacity', parseFloat(($event.target as HTMLInputElement).value))"
              type="number"
              step="0.1"
              min="0"
              max="1"
              class="input input-bordered input-xs w-full"
              placeholder="1.0"
            />
          </div>

          <!-- Color -->
          <div>
            <div class="text-xs font-semibold text-base-content/70 mb-2">Tint Color</div>
            <input
              :value="getTextureDefValue(parseInt(key), 'color')"
              @input="setTextureDefValue(parseInt(key), 'color', ($event.target as HTMLInputElement).value)"
              type="text"
              class="input input-bordered input-xs w-full"
              placeholder="#ffffff"
            />
          </div>
        </div>
      </div>
    </div>
  </div>

  <!-- Asset Picker Dialog -->
  <AssetPickerDialog
    v-if="isAssetPickerOpen"
    :world-id="worldId"
    :current-path="getTexturePathValue(selectedTextureKey)"
    @close="closeAssetPicker"
    @select="handleAssetSelected"
  />
</template>

<script setup lang="ts">
import { ref, watch } from 'vue';
import type { VisibilityModifier, TextureDefinition } from '@nimbus/shared';
import { Shape, ShapeNames, TextureKey, TextureKeyNames } from '@nimbus/shared';
import AssetPickerDialog from '@components/AssetPickerDialog.vue';
import OffsetsEditor from './OffsetsEditor.vue';

interface Props {
  modelValue?: VisibilityModifier;
  worldId?: string;
}

const props = defineProps<Props>();

const emit = defineEmits<{
  (e: 'update:modelValue', value: VisibilityModifier | undefined): void;
}>();

const localValue = ref<VisibilityModifier>(
  props.modelValue ? JSON.parse(JSON.stringify(props.modelValue)) : { shape: 1, textures: {} }
);

// Track which textures are expanded
const expandedTextures = ref<Set<number>>(new Set());

// Only watch localValue changes to emit updates (one-way)
watch(localValue, (newValue) => {
  emit('update:modelValue', newValue);
}, { deep: true });

// Shape options
const shapeOptions = ShapeNames;

// Texture key options
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

// ============================================
// Texture Management Functions
// ============================================

const isTextureExpanded = (key: number): boolean => {
  return expandedTextures.value.has(key);
};

const toggleTextureExpansion = (key: number) => {
  if (expandedTextures.value.has(key)) {
    // Collapse: Convert to simple string
    const texture = localValue.value.textures?.[key];
    if (texture && typeof texture !== 'string') {
      localValue.value.textures![key] = texture.path;
    }
    expandedTextures.value.delete(key);
  } else {
    // Expand: Convert to TextureDefinition if needed
    const texture = localValue.value.textures?.[key];
    if (texture && typeof texture === 'string') {
      localValue.value.textures![key] = { path: texture };
    }
    expandedTextures.value.add(key);
  }
};

const getTexturePathValue = (key: number): string => {
  if (!localValue.value.textures) return '';
  const texture = localValue.value.textures[key];
  if (!texture) return '';
  return typeof texture === 'string' ? texture : texture.path;
};

const setTexturePath = (key: number, path: string) => {
  if (!localValue.value.textures) {
    localValue.value.textures = {};
  }

  if (path.trim()) {
    // Keep as TextureDefinition if expanded, otherwise string
    if (expandedTextures.value.has(key)) {
      const existing = localValue.value.textures[key];
      if (typeof existing === 'string') {
        localValue.value.textures[key] = { path: path.trim() };
      } else if (existing) {
        existing.path = path.trim();
      } else {
        localValue.value.textures[key] = { path: path.trim() };
      }
    } else {
      localValue.value.textures[key] = path.trim();
    }
  } else {
    delete localValue.value.textures[key];
    expandedTextures.value.delete(key);
  }
};

const removeTexture = (key: number) => {
  if (localValue.value.textures) {
    delete localValue.value.textures[key];
    expandedTextures.value.delete(key);
  }
};

// Get nested value from TextureDefinition
const getTextureDefValue = (key: number, path: string): any => {
  const texture = localValue.value.textures?.[key];
  if (!texture || typeof texture === 'string') return undefined;

  const parts = path.split('.');
  let value: any = texture;
  for (const part of parts) {
    value = value?.[part];
  }
  return value;
};

// Set nested value in TextureDefinition
const setTextureDefValue = (key: number, path: string, value: any) => {
  if (!localValue.value.textures) {
    localValue.value.textures = {};
  }

  let texture = localValue.value.textures[key];

  // Ensure it's a TextureDefinition
  if (typeof texture === 'string') {
    texture = { path: texture };
    localValue.value.textures[key] = texture;
  } else if (!texture) {
    texture = { path: '' };
    localValue.value.textures[key] = texture;
  }

  // Set nested value
  const parts = path.split('.');
  let obj: any = texture;

  for (let i = 0; i < parts.length - 1; i++) {
    const part = parts[i];
    if (!obj[part]) {
      obj[part] = {};
    }
    obj = obj[part];
  }

  const lastPart = parts[parts.length - 1];

  // Only set if value is not empty/undefined
  if (value !== undefined && value !== null && value !== '') {
    obj[lastPart] = value;
  } else {
    delete obj[lastPart];
  }
};

// ============================================
// Asset Picker
// ============================================

const isAssetPickerOpen = ref(false);
const selectedTextureKey = ref<number>(0);

const openAssetPicker = (key: number) => {
  selectedTextureKey.value = key;
  isAssetPickerOpen.value = true;
};

const closeAssetPicker = () => {
  isAssetPickerOpen.value = false;
};

const handleAssetSelected = (path: string) => {
  setTexturePath(selectedTextureKey.value, path);
  closeAssetPicker();
};
</script>
