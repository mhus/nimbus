<template>
  <div class="space-y-3">
    <div class="text-sm font-semibold opacity-70">Repeat Step</div>

    <!-- Repeat Mode Selection -->
    <div class="form-control">
      <label class="label py-1">
        <span class="label-text text-xs">Repeat Mode</span>
      </label>
      <select
        :value="repeatMode"
        class="select select-bordered select-sm"
        @change="changeRepeatMode($event)"
      >
        <option value="times">N Times</option>
        <option value="until">Until Event</option>
      </select>
    </div>

    <!-- Times Input (if mode = times) -->
    <div v-if="repeatMode === 'times'" class="form-control">
      <label class="label py-1">
        <span class="label-text text-xs">Times</span>
      </label>
      <input
        :value="modelValue.times || 1"
        type="number"
        class="input input-bordered input-sm"
        placeholder="3"
        min="1"
        @input="updateTimes($event)"
      />
      <label class="label py-0">
        <span class="label-text-alt text-xs opacity-60">
          How many times to repeat
        </span>
      </label>
    </div>

    <!-- Until Event Input (if mode = until) -->
    <div v-if="repeatMode === 'until'" class="form-control">
      <label class="label py-1">
        <span class="label-text text-xs">Until Event</span>
      </label>
      <input
        :value="modelValue.untilEvent || ''"
        type="text"
        class="input input-bordered input-sm"
        placeholder="stop_event"
        @input="updateUntilEvent($event)"
      />
      <label class="label py-0">
        <span class="label-text-alt text-xs opacity-60">
          Event name to stop repeating
        </span>
      </label>
    </div>

    <!-- Repeated Step -->
    <div class="divider text-xs">Repeated Step</div>
    <div class="ml-4 pl-4 border-l-2 border-success">
      <StepEditor
        :model-value="modelValue.step"
        @update:model-value="updateStep"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import type { ScrawlStep } from '@nimbus/shared';
import StepEditor from '../StepEditor.vue';

const props = defineProps<{
  modelValue: ScrawlStep & { kind: 'Repeat' };
}>();

const emit = defineEmits<{
  'update:modelValue': [step: ScrawlStep];
}>();

const repeatMode = computed(() => {
  if (props.modelValue.times != null) return 'times';
  if (props.modelValue.untilEvent) return 'until';
  return 'times';
});

function changeRepeatMode(event: Event) {
  const target = event.target as HTMLSelectElement;
  const mode = target.value;

  const updated: any = {
    ...props.modelValue,
  };

  if (mode === 'times') {
    updated.times = 3;
    delete updated.untilEvent;
  } else {
    updated.untilEvent = 'stop';
    delete updated.times;
  }

  emit('update:modelValue', updated);
}

function updateTimes(event: Event) {
  const target = event.target as HTMLInputElement;
  const times = parseInt(target.value) || 1;

  emit('update:modelValue', {
    ...props.modelValue,
    times,
    untilEvent: undefined,
  } as any);
}

function updateUntilEvent(event: Event) {
  const target = event.target as HTMLInputElement;
  const untilEvent = target.value;

  emit('update:modelValue', {
    ...props.modelValue,
    untilEvent,
    times: undefined,
  } as any);
}

function updateStep(step: ScrawlStep) {
  emit('update:modelValue', {
    ...props.modelValue,
    step,
  });
}
</script>
