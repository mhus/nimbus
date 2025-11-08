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

    <!-- Scaling & Rotation -->
    <div class="grid grid-cols-5 gap-2">
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
      <div class="form-control">
        <label class="label">
          <span class="label-text text-xs">Rotation X</span>
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
          <span class="label-text text-xs">Rotation Y</span>
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
    <CollapsibleSection
      title="Geometry Offsets"
      :model-value="true"
      :default-open="false"
    >
      <OffsetsEditor
        v-model="localValue.offsets"
        :shape="localValue.shape"
      />
    </CollapsibleSection>

    <!-- Textures -->
    <CollapsibleSection
      title="Textures"
      :model-value="true"
      :default-open="true"
    >
      <div class="space-y-3 pt-2">
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
          <!-- Texture Preview Canvas -->
          <div>
            <div class="text-xs font-semibold text-base-content/70 mb-2">Preview</div>
            <div class="relative bg-base-200 rounded-lg p-2 flex items-center justify-center">
              <canvas
                :ref="(el) => setCanvasRef(parseInt(key), el as HTMLCanvasElement | null)"
                width="256"
                height="256"
                class="border border-base-300"
                style="image-rendering: pixelated;"
              />
              <!-- Loading Indicator -->
              <div v-if="isTextureLoading(parseInt(key))" class="absolute inset-0 flex items-center justify-center bg-base-200/80 rounded-lg">
                <span class="loading loading-spinner loading-md"></span>
              </div>
            </div>
          </div>

          <!-- Atlas Extraction -->
          <div>
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

          <!-- UV Transformation - Scale & Offset (all 4 fields in one row) -->
          <div>
            <div class="grid grid-cols-4 gap-2">
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

          <!-- UV Transformation - Wrap Mode & Rotation Center (all 4 fields in one row) -->
          <div>
            <div class="grid grid-cols-4 gap-2">
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
              <div class="form-control">
                <label class="label py-0">
                  <span class="label-text text-xs">uRotCenter</span>
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
                  <span class="label-text text-xs">vRotCenter</span>
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

          <!-- Texture Rotation Angles -->
          <div>
            <div class="grid grid-cols-3 gap-2">
              <div class="form-control">
                <label class="label py-0">
                  <span class="label-text text-xs">wAng (W-Axis)</span>
                </label>
                <input
                  :value="radiansToDegreesDisplay(getTextureDefValue(parseInt(key), 'uvMapping.wAng'))"
                  @input="setTextureDefValue(parseInt(key), 'uvMapping.wAng', degreesToRadians(parseFloat(($event.target as HTMLInputElement).value)))"
                  type="number"
                  step="1"
                  class="input input-bordered input-xs"
                  placeholder="0"
                />
              </div>
              <div class="form-control">
                <label class="label py-0">
                  <span class="label-text text-xs">uAng (U-Axis)</span>
                </label>
                <input
                  :value="radiansToDegreesDisplay(getTextureDefValue(parseInt(key), 'uvMapping.uAng'))"
                  @input="setTextureDefValue(parseInt(key), 'uvMapping.uAng', degreesToRadians(parseFloat(($event.target as HTMLInputElement).value)))"
                  type="number"
                  step="1"
                  class="input input-bordered input-xs"
                  placeholder="0"
                />
              </div>
              <div class="form-control">
                <label class="label py-0">
                  <span class="label-text text-xs">vAng (V-Axis)</span>
                </label>
                <input
                  :value="radiansToDegreesDisplay(getTextureDefValue(parseInt(key), 'uvMapping.vAng'))"
                  @input="setTextureDefValue(parseInt(key), 'uvMapping.vAng', degreesToRadians(parseFloat(($event.target as HTMLInputElement).value)))"
                  type="number"
                  step="1"
                  class="input input-bordered input-xs"
                  placeholder="0"
                />
              </div>
            </div>
          </div>

          <!-- Sampling, Transparency, Opacity & Color (all 4 fields in one row) -->
          <div>
            <div class="grid grid-cols-4 gap-2">
              <div>
                <label class="label py-0">
                  <span class="label-text text-xs">Sampling</span>
                </label>
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
              <div>
                <label class="label py-0">
                  <span class="label-text text-xs">Transparency</span>
                </label>
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
              <div>
                <label class="label py-0">
                  <span class="label-text text-xs">Opacity</span>
                </label>
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
              <div>
                <label class="label py-0">
                  <span class="label-text text-xs">Tint Color</span>
                </label>
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

          <!-- Back Face Culling, Effect & Shader Parameters (Shader Parameters with double width) -->
          <div>
            <div class="grid grid-cols-4 gap-2">
              <div>
                <label class="label py-0">
                  <span class="label-text text-xs">Back Face Culling</span>
                </label>
                <div class="form-control">
                  <label class="label cursor-pointer justify-start gap-2 py-1">
                    <input
                      type="checkbox"
                      :checked="getTextureDefValue(parseInt(key), 'backFaceCulling') ?? true"
                      @change="setTextureDefValue(parseInt(key), 'backFaceCulling', ($event.target as HTMLInputElement).checked)"
                      class="checkbox checkbox-sm"
                    />
                    <span class="label-text text-xs">Enable</span>
                  </label>
                </div>
              </div>
              <div>
                <label class="label py-0">
                  <span class="label-text text-xs">Effect</span>
                </label>
                <select
                  :value="getTextureDefValue(parseInt(key), 'effect') ?? 0"
                  @change="setTextureDefValue(parseInt(key), 'effect', parseInt(($event.target as HTMLSelectElement).value))"
                  class="select select-bordered select-xs w-full"
                >
                  <option :value="0">NONE</option>
                  <option :value="1">WATER</option>
                  <option :value="2">WIND</option>
                  <option :value="4">LAVA</option>
                  <option :value="5">FOG</option>
                </select>
              </div>
              <div>
                <label class="label py-0">
                  <span class="label-text text-xs">Shader</span>
                </label>
                <select
                  :value="getTextureDefValue(parseInt(key), 'shader') ?? 0"
                  @change="setTextureDefValue(parseInt(key), 'shader', parseInt(($event.target as HTMLSelectElement).value))"
                  class="select select-bordered select-xs w-full"
                >
                  <option :value="0">NONE</option>
                  <option :value="1">FLIPBOX</option>
                  <option :value="2">WIND</option>
                </select>
              </div>
              <div>
                <label class="label py-0">
                  <span class="label-text text-xs">Shader Parameters</span>
                </label>
                <input
                  :value="getTextureDefValue(parseInt(key), 'shaderParameters')"
                  @input="setTextureDefValue(parseInt(key), 'shaderParameters', ($event.target as HTMLInputElement).value)"
                  type="text"
                  class="input input-bordered input-xs w-full"
                  placeholder="e.g. 4,100 (frameCount,delayMs)"
                />
              </div>
            </div>
          </div>
        </div>
      </div>
      </div>
    </CollapsibleSection>
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
import { Shape, ShapeNames, TextureKey, TextureKeyNames, BlockEffect, BlockShader } from '@nimbus/shared';
import AssetPickerDialog from '@components/AssetPickerDialog.vue';
import CollapsibleSection from '@components/CollapsibleSection.vue';
import OffsetsEditor from './OffsetsEditor.vue';
import { assetService } from '../services/AssetService';

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

// Canvas References per Texture Key
const previewCanvasRefs = ref<Map<number, HTMLCanvasElement>>(new Map());

// Loading state per texture
const textureLoadingState = ref<Map<number, boolean>>(new Map());

// Image Cache (reuse loaded images across renders)
const textureImageCache = ref<Map<string, HTMLImageElement>>(new Map());

// Error Cache (track failed texture loads to prevent retry loops)
const textureErrorCache = ref<Map<string, Error>>(new Map());

// Helper to set canvas ref
const setCanvasRef = (key: number, el: HTMLCanvasElement | null) => {
  if (el) {
    previewCanvasRefs.value.set(key, el);
    // Render preview when canvas is mounted
    setTimeout(() => renderTexturePreview(key), 100);
  } else {
    previewCanvasRefs.value.delete(key);
  }
};

const isTextureLoading = (key: number): boolean => {
  return textureLoadingState.value.get(key) ?? false;
};

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
    // Collapse: Keep as TextureDefinition object (don't convert back to string)
    // This preserves all settings like samplingMode, backFaceCulling, etc.
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

  const oldPath = getTexturePathValue(key);

  if (path.trim()) {
    const existing = localValue.value.textures[key];

    // If it's already a TextureDefinition object, keep it as an object
    // If it's a string, keep it as a string (unless expanded)
    if (typeof existing === 'object' && existing !== null) {
      // Already an object, just update the path
      existing.path = path.trim();
    } else if (expandedTextures.value.has(key)) {
      // Not yet an object but expanded, convert to object
      localValue.value.textures[key] = { path: path.trim() };
    } else {
      // Simple string, keep as string
      localValue.value.textures[key] = path.trim();
    }

    // Clear error cache when path changes (allow retry)
    if (oldPath !== path.trim()) {
      textureErrorCache.value.delete(path.trim());
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
  // Note: false is a valid value (e.g., for backFaceCulling), so we allow boolean false
  if (value !== undefined && value !== null && value !== '' || typeof value === 'boolean') {
    obj[lastPart] = value;
  } else {
    delete obj[lastPart];
  }
};

// ============================================
// Degrees ↔ Radians Conversion
// ============================================

const degreesToRadians = (degrees: number): number => {
  if (isNaN(degrees)) return 0;
  return degrees * (Math.PI / 180);
};

const radiansToDegreesDisplay = (radians: number | undefined): number | '' => {
  if (radians === undefined || radians === null) return '';
  return Math.round(radians * (180 / Math.PI) * 100) / 100; // Round to 2 decimal places
};

// ============================================
// Texture Preview Rendering
// ============================================

const loadTextureImage = async (texturePath: string): Promise<HTMLImageElement> => {
  // Check success cache first
  if (textureImageCache.value.has(texturePath)) {
    return textureImageCache.value.get(texturePath)!;
  }

  // Check error cache - don't retry failed loads
  if (textureErrorCache.value.has(texturePath)) {
    throw textureErrorCache.value.get(texturePath)!;
  }

  // Load image
  return new Promise((resolve, reject) => {
    const img = new Image();
    img.crossOrigin = 'anonymous';

    img.onload = () => {
      // Cache successful load
      textureImageCache.value.set(texturePath, img);
      resolve(img);
    };

    img.onerror = () => {
      // Cache error to prevent retry loops
      const error = new Error(`Failed to load texture: ${texturePath}`);
      textureErrorCache.value.set(texturePath, error);
      reject(error);
    };

    // Use AssetService to construct correct URL
    if (!props.worldId) {
      const error = new Error('World ID not provided');
      textureErrorCache.value.set(texturePath, error);
      reject(error);
      return;
    }
    const assetUrl = assetService.getAssetUrl(props.worldId, texturePath);
    img.src = assetUrl;
  });
};

const renderTexturePreview = async (key: number) => {
  const canvas = previewCanvasRefs.value.get(key);
  if (!canvas) return;

  const ctx = canvas.getContext('2d');
  if (!ctx) return;

  // Clear canvas with checkerboard pattern
  ctx.clearRect(0, 0, 256, 256);
  drawCheckerboard(ctx, 256, 256);

  // Get texture path
  const texturePath = getTexturePathValue(key);
  if (!texturePath) {
    // Draw placeholder
    ctx.fillStyle = '#666';
    ctx.font = '12px sans-serif';
    ctx.textAlign = 'center';
    ctx.fillText('No texture selected', 128, 128);
    return;
  }

  textureLoadingState.value.set(key, true);

  try {
    // Load image
    const img = await loadTextureImage(texturePath);

    // Get UV parameters
    const uvMapping = {
      x: getTextureDefValue(key, 'uvMapping.x') ?? 0,
      y: getTextureDefValue(key, 'uvMapping.y') ?? 0,
      w: getTextureDefValue(key, 'uvMapping.w') ?? img.width,
      h: getTextureDefValue(key, 'uvMapping.h') ?? img.height,
      uScale: getTextureDefValue(key, 'uvMapping.uScale') ?? 1,
      vScale: getTextureDefValue(key, 'uvMapping.vScale') ?? 1,
      uOffset: getTextureDefValue(key, 'uvMapping.uOffset') ?? 0,
      vOffset: getTextureDefValue(key, 'uvMapping.vOffset') ?? 0,
      wAng: getTextureDefValue(key, 'uvMapping.wAng') ?? 0,
      wrapU: getTextureDefValue(key, 'uvMapping.wrapU') ?? 1,
      wrapV: getTextureDefValue(key, 'uvMapping.wrapV') ?? 1,
      uRotationCenter: getTextureDefValue(key, 'uvMapping.uRotationCenter') ?? 0.5,
      vRotationCenter: getTextureDefValue(key, 'uvMapping.vRotationCenter') ?? 0.5,
    };

    // Apply UV transformations
    applyUVTransformations(ctx, img, uvMapping);

  } catch (error) {
    // Show error on canvas (from cache or new error)
    ctx.fillStyle = '#ff0000';
    ctx.font = '12px sans-serif';
    ctx.textAlign = 'center';
    ctx.fillText('Failed to load texture', 128, 118);
    ctx.font = '10px sans-serif';
    ctx.fillStyle = '#999';
    ctx.fillText('(Check texture path)', 128, 138);
    console.error('Texture preview error:', error);
  } finally {
    textureLoadingState.value.set(key, false);
  }
};

const drawCheckerboard = (ctx: CanvasRenderingContext2D, width: number, height: number) => {
  const squareSize = 8;
  ctx.fillStyle = '#e0e0e0';
  ctx.fillRect(0, 0, width, height);
  ctx.fillStyle = '#c0c0c0';
  for (let y = 0; y < height; y += squareSize) {
    for (let x = 0; x < width; x += squareSize) {
      if ((x / squareSize + y / squareSize) % 2 === 0) {
        ctx.fillRect(x, y, squareSize, squareSize);
      }
    }
  }
};

const applyUVTransformations = (
  ctx: CanvasRenderingContext2D,
  img: HTMLImageElement,
  uv: any
) => {
  ctx.save();

  // Extract source region (Atlas Extraction)
  const sourceX = Math.max(0, Math.min(uv.x, img.width));
  const sourceY = Math.max(0, Math.min(uv.y, img.height));
  const sourceW = Math.max(1, Math.min(uv.w, img.width - sourceX));
  const sourceH = Math.max(1, Math.min(uv.h, img.height - sourceY));

  // Calculate rotation center in canvas space
  const rotCenterX = 128 + (uv.uRotationCenter - 0.5) * 256;
  const rotCenterY = 128 + (uv.vRotationCenter - 0.5) * 256;

  // Move to rotation center
  ctx.translate(rotCenterX, rotCenterY);

  // Apply rotation (wAng)
  if (uv.wAng) {
    ctx.rotate(uv.wAng);
  }

  // Apply scale
  ctx.scale(uv.uScale, uv.vScale);

  // Move back from rotation center
  ctx.translate(-rotCenterX, -rotCenterY);

  // Apply offset (in UV space 0-1, convert to canvas space)
  const offsetX = uv.uOffset * 256;
  const offsetY = uv.vOffset * 256;
  ctx.translate(offsetX, offsetY);

  // Handle wrap modes
  if (uv.wrapU === 1 && uv.wrapV === 1) {
    // REPEAT mode - create pattern
    const tempCanvas = document.createElement('canvas');
    tempCanvas.width = sourceW;
    tempCanvas.height = sourceH;
    const tempCtx = tempCanvas.getContext('2d')!;
    tempCtx.drawImage(img, sourceX, sourceY, sourceW, sourceH, 0, 0, sourceW, sourceH);

    const pattern = ctx.createPattern(tempCanvas, 'repeat');
    if (pattern) {
      ctx.fillStyle = pattern;
      ctx.fillRect(0, 0, 256, 256);
    }
  } else if (uv.wrapU === 2 || uv.wrapV === 2) {
    // MIRROR mode - draw mirrored copies (simplified)
    ctx.drawImage(img, sourceX, sourceY, sourceW, sourceH, 0, 0, 256, 256);
  } else {
    // CLAMP mode - single draw
    ctx.drawImage(img, sourceX, sourceY, sourceW, sourceH, 0, 0, 256, 256);
  }

  ctx.restore();
};

// Watch for texture changes and re-render preview (debounced)
let previewRenderTimeout: number | null = null;
watch(
  () => localValue.value.textures,
  () => {
    // Debounce re-render
    if (previewRenderTimeout) {
      clearTimeout(previewRenderTimeout);
    }
    previewRenderTimeout = window.setTimeout(() => {
      expandedTextures.value.forEach((key) => {
        renderTexturePreview(key);
      });
    }, 300);
  },
  { deep: true }
);

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
