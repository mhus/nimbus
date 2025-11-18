<template>
  <div class="space-y-2">
    <div class="text-sm font-semibold opacity-70">Command Step</div>

    <!-- Command Name -->
    <div class="form-control">
      <label class="label py-1">
        <span class="label-text text-xs">Command</span>
      </label>
      <input
        :value="modelValue.cmd"
        type="text"
        class="input input-bordered input-sm"
        placeholder="notification"
        @input="updateCmd($event)"
      />
    </div>

    <!-- Parameters -->
    <div class="form-control">
      <label class="label py-1">
        <span class="label-text text-xs">Parameters (JSON Array)</span>
      </label>
      <textarea
        :value="parametersJson"
        class="textarea textarea-bordered textarea-sm font-mono text-xs"
        rows="3"
        placeholder='[0, "null", "Message"]'
        @input="updateParameters($event)"
      ></textarea>
      <label v-if="paramError" class="label py-0">
        <span class="label-text-alt text-xs text-error">{{ paramError }}</span>
      </label>
    </div>

    <!-- Common Commands Presets -->
    <div class="collapse collapse-arrow bg-base-200">
      <input type="checkbox" />
      <div class="collapse-title text-xs font-medium">
        Quick Commands
      </div>
      <div class="collapse-content">
        <div class="space-y-1">
          <button
            class="btn btn-xs btn-outline w-full justify-start"
            @click="applyPreset('notification', [0, 'null', 'Message'])"
          >
            notification
          </button>
          <button
            class="btn btn-xs btn-outline w-full justify-start"
            @click="applyPreset('teleport', [0, 64, 0])"
          >
            teleport
          </button>
          <button
            class="btn btn-xs btn-outline w-full justify-start"
            @click="applyPreset('centerText', ['Text', 2000])"
          >
            centerText
          </button>
          <button
            class="btn btn-xs btn-outline w-full justify-start"
            @click="applyPreset('playSound', ['audio/sound.ogg', 1.0])"
          >
            playSound
          </button>
        </div>
      </div>
    </div>

    <!-- Preview -->
    <div class="text-xs opacity-50">
      <code>{{ modelValue.cmd }}({{ (modelValue.parameters || []).join(', ') }})</code>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import type { ScrawlStep } from '@nimbus/shared';

const props = defineProps<{
  modelValue: ScrawlStep & { kind: 'Cmd' };
}>();

const emit = defineEmits<{
  'update:modelValue': [step: ScrawlStep];
}>();

const paramError = ref<string | null>(null);

const parametersJson = computed(() => {
  return JSON.stringify(props.modelValue.parameters || [], null, 0);
});

function updateCmd(event: Event) {
  const target = event.target as HTMLInputElement;
  emit('update:modelValue', {
    ...props.modelValue,
    cmd: target.value,
  });
}

function updateParameters(event: Event) {
  const target = event.target as HTMLTextAreaElement;
  try {
    const parsed = JSON.parse(target.value);
    if (!Array.isArray(parsed)) {
      paramError.value = 'Parameters must be an array';
      return;
    }
    paramError.value = null;
    emit('update:modelValue', {
      ...props.modelValue,
      parameters: parsed,
    });
  } catch (e: any) {
    paramError.value = e.message;
  }
}

function applyPreset(cmd: string, parameters: any[]) {
  emit('update:modelValue', {
    ...props.modelValue,
    cmd,
    parameters,
  });
}
</script>
