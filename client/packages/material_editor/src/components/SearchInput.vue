<template>
  <div class="form-control">
    <div class="input-group">
      <input
        type="text"
        :placeholder="placeholder"
        :value="modelValue"
        @input="handleInput"
        class="input input-bordered w-full"
      />
      <button class="btn btn-square">
        <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
        </svg>
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';

interface Props {
  modelValue?: string;
  placeholder?: string;
  debounce?: number;
}

const props = withDefaults(defineProps<Props>(), {
  modelValue: '',
  placeholder: 'Search...',
  debounce: 300,
});

const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void;
}>();

let debounceTimer: ReturnType<typeof setTimeout> | null = null;

const handleInput = (event: Event) => {
  const value = (event.target as HTMLInputElement).value;

  if (debounceTimer) {
    clearTimeout(debounceTimer);
  }

  debounceTimer = setTimeout(() => {
    emit('update:modelValue', value);
  }, props.debounce);
};
</script>
