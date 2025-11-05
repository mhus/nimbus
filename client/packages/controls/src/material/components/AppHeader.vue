<template>
  <header class="navbar bg-base-300 shadow-lg">
    <div class="flex-1">
      <h1 class="text-xl font-bold px-4">Nimbus Material Editor</h1>
    </div>
    <div class="flex-none">
      <!-- World Selector -->
      <WorldSelector />
    </div>
  </header>
</template>

<script setup lang="ts">
import WorldSelector from './WorldSelector.vue';
</script>
