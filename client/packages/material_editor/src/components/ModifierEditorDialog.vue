<template>
  <!-- Custom Modal (not Headless UI Dialog to avoid nesting issues) -->
  <Teleport to="body">
    <div class="fixed inset-0 z-[60] flex items-center justify-center p-4" @click.self="emit('close')">
      <!-- Backdrop -->
      <div class="absolute inset-0 bg-black bg-opacity-50" @click="emit('close')"></div>

      <!-- Modal Content -->
      <div class="relative w-full max-w-5xl bg-base-100 rounded-2xl shadow-2xl p-6 max-h-[90vh] overflow-hidden flex flex-col">
        <h2 class="text-2xl font-bold mb-4">
          Edit Modifier - Status {{ statusNumber }}
        </h2>

        <div class="space-y-4 overflow-y-auto pr-2 flex-1">
                <!-- Visibility Section -->
                <CollapsibleSection
                  title="Visibility"
                  :model-value="!!modifierData.visibility"
                  @update:model-value="toggleVisibility"
                  :default-open="true"
                >
                  <VisibilityEditor v-model="modifierData.visibility" :world-id="worldId" />
                </CollapsibleSection>

                <!-- Physics Section -->
                <CollapsibleSection
                  title="Physics"
                  :model-value="!!modifierData.physics"
                  @update:model-value="togglePhysics"
                >
                  <PhysicsEditor v-model="modifierData.physics" />
                </CollapsibleSection>

                <!-- Wind Section -->
                <CollapsibleSection
                  title="Wind"
                  :model-value="!!modifierData.wind"
                  @update:model-value="toggleWind"
                >
                  <WindEditor v-model="modifierData.wind" />
                </CollapsibleSection>

                <!-- Effects Section -->
                <CollapsibleSection
                  title="Effects"
                  :model-value="!!modifierData.effects"
                  @update:model-value="toggleEffects"
                >
                  <EffectsEditor v-model="modifierData.effects" />
                </CollapsibleSection>

                <!-- Illumination Section -->
                <CollapsibleSection
                  title="Illumination"
                  :model-value="!!modifierData.illumination"
                  @update:model-value="toggleIllumination"
                >
                  <IlluminationEditor v-model="modifierData.illumination" />
                </CollapsibleSection>

                <!-- Sound Section -->
                <CollapsibleSection
                  title="Sound"
                  :model-value="!!modifierData.sound"
                  @update:model-value="toggleSound"
                >
                  <SoundEditor v-model="modifierData.sound" />
                </CollapsibleSection>

                <!-- Simple Fields -->
                <div class="grid grid-cols-2 gap-4">
                  <div class="form-control">
                    <label class="label">
                      <span class="label-text">Sprite Count</span>
                    </label>
                    <input
                      v-model.number="modifierData.spriteCount"
                      type="number"
                      class="input input-bordered input-sm"
                      placeholder="Optional"
                    />
                  </div>

                  <div class="form-control">
                    <label class="label">
                      <span class="label-text">Alpha</span>
                    </label>
                    <input
                      v-model.number="modifierData.alpha"
                      type="number"
                      step="0.01"
                      min="0"
                      max="1"
                      class="input input-bordered input-sm"
                      placeholder="0.0 - 1.0"
                    />
                  </div>
                </div>
              </div>

        <!-- Actions -->
        <div class="mt-6 flex justify-end gap-2 border-t pt-4">
          <button class="btn btn-ghost" @click="emit('close')">
            Cancel
          </button>
          <button class="btn btn-primary" @click="handleSave">
            Save
          </button>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue';
import type { BlockModifier } from '@nimbus/shared';
import CollapsibleSection from './CollapsibleSection.vue';
import VisibilityEditor from './editors/VisibilityEditor.vue';
import PhysicsEditor from './editors/PhysicsEditor.vue';
import WindEditor from './editors/WindEditor.vue';
import EffectsEditor from './editors/EffectsEditor.vue';
import IlluminationEditor from './editors/IlluminationEditor.vue';
import SoundEditor from './editors/SoundEditor.vue';

interface Props {
  modifier: BlockModifier;
  statusNumber: number;
  worldId: string;
}

const props = defineProps<Props>();

const emit = defineEmits<{
  (e: 'close'): void;
  (e: 'save', modifier: BlockModifier): void;
}>();

// Working copy of modifier
const modifierData = ref<BlockModifier>(JSON.parse(JSON.stringify(props.modifier)));

// Toggle functions for each section
const toggleVisibility = (enabled: boolean) => {
  if (!enabled) {
    modifierData.value.visibility = undefined;
  } else if (!modifierData.value.visibility) {
    modifierData.value.visibility = { shape: 1, textures: {} };
  }
};

const togglePhysics = (enabled: boolean) => {
  if (!enabled) {
    modifierData.value.physics = undefined;
  } else if (!modifierData.value.physics) {
    modifierData.value.physics = {};
  }
};

const toggleWind = (enabled: boolean) => {
  if (!enabled) {
    modifierData.value.wind = undefined;
  } else if (!modifierData.value.wind) {
    modifierData.value.wind = {};
  }
};

const toggleEffects = (enabled: boolean) => {
  if (!enabled) {
    modifierData.value.effects = undefined;
  } else if (!modifierData.value.effects) {
    modifierData.value.effects = {};
  }
};

const toggleIllumination = (enabled: boolean) => {
  if (!enabled) {
    modifierData.value.illumination = undefined;
  } else if (!modifierData.value.illumination) {
    modifierData.value.illumination = {};
  }
};

const toggleSound = (enabled: boolean) => {
  if (!enabled) {
    modifierData.value.sound = undefined;
  } else if (!modifierData.value.sound) {
    modifierData.value.sound = {};
  }
};

const handleSave = () => {
  emit('save', modifierData.value);
};
</script>
