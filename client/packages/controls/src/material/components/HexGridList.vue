<template>
  <div class="overflow-x-auto">
    <table class="table w-full">
      <thead>
        <tr>
          <th>Position (Q:R)</th>
          <th>Name</th>
          <th>Description</th>
          <th>Status</th>
          <th>Created</th>
          <th>Actions</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="hexGrid in hexGrids" :key="`${hexGrid.position.q}:${hexGrid.position.r}`">
          <!-- Position -->
          <td>
            <code class="text-sm font-mono">{{ hexGrid.position.q }}:{{ hexGrid.position.r }}</code>
          </td>

          <!-- Name -->
          <td>
            <div class="font-medium">{{ hexGrid.name || '-' }}</div>
            <div v-if="hexGrid.icon" class="text-xs text-base-content/50">{{ hexGrid.icon }}</div>
          </td>

          <!-- Description -->
          <td>
            <div class="text-sm text-base-content/70 max-w-xs truncate">
              {{ hexGrid.description || '-' }}
            </div>
          </td>

          <!-- Status -->
          <td>
            <span
              class="badge badge-sm"
              :class="hexGrid.enabled ? 'badge-success' : 'badge-error'"
            >
              {{ hexGrid.enabled ? 'Enabled' : 'Disabled' }}
            </span>
          </td>

          <!-- Created -->
          <td>
            <div v-if="hexGrid.createdAt" class="text-sm text-base-content/70">
              {{ formatDate(hexGrid.createdAt) }}
            </div>
            <div v-else class="text-sm text-base-content/50">-</div>
          </td>

          <!-- Actions -->
          <td>
            <div class="flex gap-2">
              <button
                class="btn btn-xs btn-ghost"
                @click="$emit('edit', hexGrid)"
                title="Edit"
              >
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
                </svg>
              </button>

              <button
                class="btn btn-xs btn-ghost"
                :class="hexGrid.enabled ? 'text-warning' : 'text-success'"
                @click="$emit('toggle-enabled', hexGrid)"
                :title="hexGrid.enabled ? 'Disable' : 'Enable'"
              >
                <svg v-if="hexGrid.enabled" class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M18.364 18.364A9 9 0 005.636 5.636m12.728 12.728A9 9 0 015.636 5.636m12.728 12.728L5.636 5.636" />
                </svg>
                <svg v-else class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
              </button>

              <button
                class="btn btn-xs btn-ghost text-error"
                @click="$emit('delete', hexGrid)"
                title="Delete"
              >
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                </svg>
              </button>
            </div>
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script setup lang="ts">
import type { HexGridWithId } from '@/composables/useHexGrids';

defineProps<{
  hexGrids: HexGridWithId[];
  loading?: boolean;
}>();

defineEmits<{
  edit: [hexGrid: HexGridWithId];
  delete: [hexGrid: HexGridWithId];
  'toggle-enabled': [hexGrid: HexGridWithId];
}>();

/**
 * Format date for display
 */
const formatDate = (dateString: string): string => {
  const date = new Date(dateString);
  return date.toLocaleString();
};
</script>
