<template>
  <div class="bg-white rounded-lg shadow-md hover:shadow-lg transition-shadow">
    <a :href="url" class="block p-6">
      <div class="flex items-center gap-3 mb-3">
        <div :class="`text-${iconColor}-600`">
          <slot />
        </div>
        <h2 class="text-xl font-bold text-gray-800">{{ title }}</h2>
      </div>
      <p class="text-gray-600 text-sm mb-4">{{ description }}</p>
      <div class="btn btn-primary w-full inline-block text-center" :class="`bg-${iconColor}-600 hover:bg-${iconColor}-700`">
        Open Editor
      </div>
    </a>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  title: string;
  description: string;
  iconColor: string;
  url: string;
}>();
</script>
