<template>
  <div class="min-h-screen flex flex-col">
    <!-- Header -->
    <header class="navbar bg-base-300 shadow-lg">
      <div class="flex-1">
        <h1 class="text-xl font-bold px-4">Nimbus Block Instance Editor</h1>
      </div>
      <div class="flex-none">
        <span class="text-sm text-base-content/70">World: {{ worldId }}</span>
      </div>
    </header>

    <!-- Main Content -->
    <main class="flex-1 container mx-auto px-4 py-6">
      <BlockInstanceEditor />
    </main>

    <!-- Footer -->
    <footer class="footer footer-center p-4 bg-base-300 text-base-content">
      <div>
        <p>Nimbus Block Editor v2.0.0</p>
      </div>
    </footer>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import BlockInstanceEditor from './views/BlockInstanceEditor.vue';

// Get world ID from URL or env
const params = new URLSearchParams(window.location.search);
const worldId = computed(() => params.get('world') || import.meta.env.VITE_WORLD_ID || 'main');
</script>
