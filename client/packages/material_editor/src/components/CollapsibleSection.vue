<template>
  <div class="collapse collapse-arrow bg-base-200 rounded-box">
    <input
      type="checkbox"
      :checked="isOpen && isEnabled"
      @change="toggleOpen"
      :disabled="!isEnabled"
    />
    <div class="collapse-title font-medium flex items-center gap-2">
      <input
        type="checkbox"
        class="checkbox checkbox-sm"
        :checked="isEnabled"
        @change="toggleEnabled"
        @click.stop
      />
      <span :class="{ 'text-base-content/40': !isEnabled }">{{ title }}</span>
      <span v-if="!isEnabled" class="badge badge-ghost badge-sm">disabled</span>
    </div>
    <div class="collapse-content" v-if="isEnabled">
      <slot />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, toRef } from 'vue';

interface Props {
  title: string;
  modelValue?: boolean; // Is this section enabled?
  defaultOpen?: boolean;
}

const props = withDefaults(defineProps<Props>(), {
  modelValue: true,
  defaultOpen: false,
});

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void;
}>();

const isEnabled = toRef(props, 'modelValue');
const isOpen = ref(props.defaultOpen);

const toggleEnabled = (event: Event) => {
  const newValue = (event.target as HTMLInputElement).checked;
  emit('update:modelValue', newValue);

  // Auto-open when enabling
  if (newValue && !isOpen.value) {
    isOpen.value = true;
  }
};

const toggleOpen = () => {
  if (isEnabled.value) {
    isOpen.value = !isOpen.value;
  }
};
</script>
