<template>
  <div class="space-y-3">
    <!-- Type-specific Editor -->
    <component
      :is="getEditorComponent(modelValue.kind)"
      :model-value="modelValue"
      @update:model-value="emit('update:modelValue', $event)"
    />
  </div>
</template>

<script setup lang="ts">
import { defineAsyncComponent } from 'vue';
import type { ScrawlStep } from '@nimbus/shared';
import WaitStepEditor from './editors/WaitStepEditor.vue';
import CmdStepEditor from './editors/CmdStepEditor.vue';
import PlayStepEditor from './editors/PlayStepEditor.vue';
import SequenceStepEditor from './editors/SequenceStepEditor.vue';

const props = defineProps<{
  modelValue: ScrawlStep;
}>();

const emit = defineEmits<{
  'update:modelValue': [step: ScrawlStep];
}>();

// Map step kinds to editor components
function getEditorComponent(kind: string) {
  switch (kind) {
    case 'Wait':
      return WaitStepEditor;
    case 'Cmd':
      return CmdStepEditor;
    case 'Play':
      return PlayStepEditor;
    case 'Sequence':
      return SequenceStepEditor;
    // More editors will be added here
    case 'Parallel':
    case 'Repeat':
    case 'ForEach':
    case 'If':
    case 'Call':
    case 'SetVar':
    case 'EmitEvent':
    case 'WaitEvent':
    case 'LodSwitch':
    default:
      // Fallback: JSON editor for unimplemented types
      return defineAsyncComponent(() => import('./editors/GenericStepEditor.vue'));
  }
}
</script>
