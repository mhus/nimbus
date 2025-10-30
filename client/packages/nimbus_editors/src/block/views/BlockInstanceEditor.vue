<template>
  <div class="space-y-4">
    <div class="alert alert-info">
      <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
      </svg>
      <div>
        <h3 class="font-bold">Block Instance Editor</h3>
        <div class="text-sm">
          URL Parameters: <code>?world=main&block=10,64,5</code>
        </div>
      </div>
    </div>

    <div v-if="!blockCoordinates" class="text-center py-12">
      <p class="text-base-content/70 text-lg">No block coordinates specified</p>
      <p class="text-base-content/50 text-sm mt-2">
        Add <code>?block=x,y,z</code> to the URL
      </p>
    </div>

    <div v-else class="card bg-base-100 shadow-xl">
      <div class="card-body">
        <h2 class="card-title">
          Block at ({{ blockCoordinates.x }}, {{ blockCoordinates.y }}, {{ blockCoordinates.z }})
        </h2>
        <p class="text-base-content/70">
          Coming soon: Block metadata editor with ModifierEditorDialog integration
        </p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';

// Parse block coordinates from URL
const params = new URLSearchParams(window.location.search);
const blockParam = params.get('block');

const blockCoordinates = computed(() => {
  if (!blockParam) return null;

  const parts = blockParam.split(',').map(Number);
  if (parts.length !== 3 || parts.some(isNaN)) return null;

  return { x: parts[0], y: parts[1], z: parts[2] };
});
</script>
