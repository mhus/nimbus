<template>
  <div class="fixed inset-0 bg-black/50 flex items-center justify-center z-50" @click.self="handleClose">
    <div class="bg-base-100 rounded-lg shadow-xl w-full h-full max-w-7xl max-h-[90vh] flex flex-col">
      <!-- Header -->
      <div class="p-4 border-b border-base-300 flex items-center justify-between">
        <div>
          <h2 class="text-xl font-bold">Block Grid Editor</h2>
          <p class="text-sm text-base-content/70">
            {{ sourceType === 'terrain' ? 'WLayerTerrain' : 'WLayerModel' }} -
            {{ layerName }} {{ modelName ? `/ ${modelName}` : '' }}
          </p>
        </div>
        <button class="btn btn-sm btn-ghost" @click="handleClose">Close</button>
      </div>

      <!-- Content -->
      <div class="flex-1 overflow-hidden flex">
        <!-- Main Grid View -->
        <div class="flex-1 overflow-auto p-4 bg-base-200">
          <div v-if="loading" class="flex items-center justify-center h-full">
            <span class="loading loading-spinner loading-lg"></span>
          </div>

          <div v-else-if="error" class="alert alert-error">
            <span>{{ error }}</span>
          </div>

          <div v-else class="flex flex-col items-center gap-4">
            <!-- Isometric Grid Canvas -->
            <canvas
              ref="canvasRef"
              :width="canvasWidth"
              :height="canvasHeight"
              class="border border-base-300 bg-white cursor-crosshair"
              @click="handleCanvasClick"
              @mousemove="handleCanvasHover"
            />

            <!-- Navigation Component -->
            <NavigateSelectedBlockComponent
              :selected-block="selectedBlock"
              :step="1"
              :size="200"
              :show-execute-button="false"
              @navigate="handleNavigate"
            />
          </div>
        </div>

        <!-- Sidebar: Block Details -->
        <div class="w-96 border-l border-base-300 p-4 overflow-auto">
          <div v-if="selectedBlock">
            <h3 class="font-bold text-lg mb-4">Block Details</h3>

            <div class="space-y-2 mb-4">
              <div class="flex gap-2">
                <span class="badge badge-error">X: {{ selectedBlock.x }}</span>
                <span class="badge badge-success">Y: {{ selectedBlock.y }}</span>
                <span class="badge badge-info">Z: {{ selectedBlock.z }}</span>
              </div>
            </div>

            <div v-if="loadingBlockDetails" class="flex items-center justify-center py-8">
              <span class="loading loading-spinner loading-md"></span>
            </div>

            <div v-else-if="blockDetails" class="space-y-3">
              <div class="form-control">
                <label class="label">
                  <span class="label-text font-semibold">Block Type ID</span>
                </label>
                <input
                  type="text"
                  :value="blockDetails.block?.blockTypeId || 'air'"
                  class="input input-bordered input-sm"
                  readonly
                />
              </div>

              <div class="form-control">
                <label class="label">
                  <span class="label-text font-semibold">Status</span>
                </label>
                <input
                  type="number"
                  :value="blockDetails.block?.status || 0"
                  class="input input-bordered input-sm"
                  readonly
                />
              </div>

              <div v-if="blockDetails.group !== undefined" class="form-control">
                <label class="label">
                  <span class="label-text font-semibold">Group</span>
                </label>
                <input
                  type="number"
                  :value="blockDetails.group"
                  class="input input-bordered input-sm"
                  readonly
                />
              </div>

              <div v-if="blockDetails.weight !== undefined" class="form-control">
                <label class="label">
                  <span class="label-text font-semibold">Weight</span>
                </label>
                <input
                  type="number"
                  :value="blockDetails.weight"
                  class="input input-bordered input-sm"
                  readonly
                />
              </div>

              <div v-if="blockDetails.override !== undefined" class="form-control">
                <label class="label cursor-pointer justify-start gap-2">
                  <input
                    type="checkbox"
                    :checked="blockDetails.override"
                    class="checkbox checkbox-sm"
                    disabled
                  />
                  <span class="label-text">Override</span>
                </label>
              </div>

              <!-- Raw JSON Display -->
              <div class="collapse collapse-arrow bg-base-200 mt-4">
                <input type="checkbox" />
                <div class="collapse-title text-sm font-medium">
                  Raw JSON
                </div>
                <div class="collapse-content">
                  <pre class="text-xs overflow-auto">{{ JSON.stringify(blockDetails, null, 2) }}</pre>
                </div>
              </div>
            </div>

            <div v-else class="text-center py-8 text-base-content/50">
              No block at this position
            </div>
          </div>

          <div v-else class="text-center py-8 text-base-content/50">
            Click on a block to view details
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed, watch } from 'vue';
import NavigateSelectedBlockComponent from '@/components/NavigateSelectedBlockComponent.vue';

interface Props {
  worldId: string;
  layerId: string;
  layerName: string;
  sourceType: 'terrain' | 'model';
  modelId?: string;
  modelName?: string;
}

const props = defineProps<Props>();
const emit = defineEmits<{
  close: [];
}>();

// State
const loading = ref(true);
const error = ref<string | null>(null);
const blockCoordinates = ref<Array<{ x: number; y: number; z: number; color?: string }>>([]);
const selectedBlock = ref<{ x: number; y: number; z: number } | null>(null);
const loadingBlockDetails = ref(false);
const blockDetails = ref<any>(null);

// Canvas
const canvasRef = ref<HTMLCanvasElement | null>(null);
const canvasWidth = ref(1200);
const canvasHeight = ref(900);

// Isometric projection settings
// Smaller tiles for 32x32x32 grid
const tileWidth = 16;  // Reduced from 64 to 16
const tileHeight = 8;  // Reduced from 32 to 8
const offsetX = canvasWidth.value / 2;
const offsetY = 150;  // Increased top padding

// Grid bounds (dynamically calculated from blocks)
const gridBounds = computed(() => {
  if (blockCoordinates.value.length === 0) {
    return { minX: 0, maxX: 0, minY: 0, maxY: 0, minZ: 0, maxZ: 0 };
  }

  const minX = Math.min(...blockCoordinates.value.map(b => b.x));
  const maxX = Math.max(...blockCoordinates.value.map(b => b.x));
  const minY = Math.min(...blockCoordinates.value.map(b => b.y));
  const maxY = Math.max(...blockCoordinates.value.map(b => b.y));
  const minZ = Math.min(...blockCoordinates.value.map(b => b.z));
  const maxZ = Math.max(...blockCoordinates.value.map(b => b.z));

  return { minX, maxX, minY, maxY, minZ, maxZ };
});

// Convert 3D world coordinates to 2D isometric screen coordinates
function worldToScreen(x: number, y: number, z: number): { x: number; y: number } {
  const isoX = (x - z) * (tileWidth / 2) + offsetX;
  const isoY = (x + z) * (tileHeight / 2) - y * tileHeight + offsetY;
  return { x: isoX, y: isoY };
}

// Convert 2D screen coordinates to approximate 3D world coordinates
// This is an approximation - for accurate picking, we need ray casting
function screenToWorld(screenX: number, screenY: number, y: number = 0): { x: number; y: number; z: number } {
  // Inverse isometric projection
  const relX = screenX - offsetX;
  const relY = screenY - offsetY + y * tileHeight;

  const x = (relX / (tileWidth / 2) + relY / (tileHeight / 2)) / 2;
  const z = (relY / (tileHeight / 2) - relX / (tileWidth / 2)) / 2;

  return { x: Math.round(x), y, z: Math.round(z) };
}

// Draw the isometric grid
function drawGrid() {
  const canvas = canvasRef.value;
  if (!canvas) return;

  const ctx = canvas.getContext('2d');
  if (!ctx) return;

  // Clear canvas
  ctx.clearRect(0, 0, canvasWidth.value, canvasHeight.value);

  // Sort blocks by depth for proper rendering (back to front)
  const sortedBlocks = [...blockCoordinates.value].sort((a, b) => {
    // Sort by Y first (lower Y = further back), then by X+Z (isometric depth)
    if (a.y !== b.y) return a.y - b.y;
    return (a.x + a.z) - (b.x + b.z);
  });

  // Draw each block as a wireframe cube
  for (const block of sortedBlocks) {
    const { x, y, z } = block;
    const color = block.color || '#3b82f6';
    const isSelected = selectedBlock.value?.x === x && selectedBlock.value?.y === y && selectedBlock.value?.z === z;

    // Calculate 8 corner points of the cube
    const corners = [
      worldToScreen(x, y, z),         // Bottom-front-left
      worldToScreen(x + 1, y, z),     // Bottom-front-right
      worldToScreen(x + 1, y, z + 1), // Bottom-back-right
      worldToScreen(x, y, z + 1),     // Bottom-back-left
      worldToScreen(x, y + 1, z),     // Top-front-left
      worldToScreen(x + 1, y + 1, z), // Top-front-right
      worldToScreen(x + 1, y + 1, z + 1), // Top-back-right
      worldToScreen(x, y + 1, z + 1), // Top-back-left
    ];

    // Draw wireframe edges
    ctx.strokeStyle = isSelected ? '#ff0000' : color;
    ctx.lineWidth = isSelected ? 2 : 1;  // Thinner lines for smaller blocks
    ctx.globalAlpha = 0.7;

    // Bottom face
    ctx.beginPath();
    ctx.moveTo(corners[0].x, corners[0].y);
    ctx.lineTo(corners[1].x, corners[1].y);
    ctx.lineTo(corners[2].x, corners[2].y);
    ctx.lineTo(corners[3].x, corners[3].y);
    ctx.closePath();
    ctx.stroke();

    // Top face
    ctx.beginPath();
    ctx.moveTo(corners[4].x, corners[4].y);
    ctx.lineTo(corners[5].x, corners[5].y);
    ctx.lineTo(corners[6].x, corners[6].y);
    ctx.lineTo(corners[7].x, corners[7].y);
    ctx.closePath();
    ctx.stroke();

    // Vertical edges
    ctx.beginPath();
    ctx.moveTo(corners[0].x, corners[0].y);
    ctx.lineTo(corners[4].x, corners[4].y);
    ctx.stroke();

    ctx.beginPath();
    ctx.moveTo(corners[1].x, corners[1].y);
    ctx.lineTo(corners[5].x, corners[5].y);
    ctx.stroke();

    ctx.beginPath();
    ctx.moveTo(corners[2].x, corners[2].y);
    ctx.lineTo(corners[6].x, corners[6].y);
    ctx.stroke();

    ctx.beginPath();
    ctx.moveTo(corners[3].x, corners[3].y);
    ctx.lineTo(corners[7].x, corners[7].y);
    ctx.stroke();

    ctx.globalAlpha = 1.0;
  }
}

// Load block coordinates from backend
async function loadBlockCoordinates() {
  loading.value = true;
  error.value = null;

  try {
    const apiUrl = import.meta.env.VITE_CONTROL_API_URL;
    let url: string;

    if (props.sourceType === 'terrain') {
      url = `${apiUrl}/control/worlds/${props.worldId}/layers/${props.layerId}/terrain/blocks`;
    } else {
      url = `${apiUrl}/control/worlds/${props.worldId}/layers/${props.layerId}/models/${props.modelId}/blocks`;
    }

    const response = await fetch(url, {
      credentials: 'include',
      headers: {
        'Accept': 'application/json',
      },
    });

    if (!response.ok) {
      throw new Error(`Failed to load blocks: ${response.statusText}`);
    }

    const data = await response.json();
    blockCoordinates.value = data.blocks || [];

    // Redraw grid after loading
    setTimeout(() => drawGrid(), 50);
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'Failed to load blocks';
    console.error('Failed to load block coordinates:', err);
  } finally {
    loading.value = false;
  }
}

// Load details for a specific block
async function loadBlockDetails(x: number, y: number, z: number) {
  loadingBlockDetails.value = true;
  blockDetails.value = null;

  try {
    const apiUrl = import.meta.env.VITE_CONTROL_API_URL;
    let url: string;

    if (props.sourceType === 'terrain') {
      url = `${apiUrl}/control/worlds/${props.worldId}/layers/${props.layerId}/terrain/block/${x}/${y}/${z}`;
    } else {
      url = `${apiUrl}/control/worlds/${props.worldId}/layers/${props.layerId}/models/${props.modelId}/block/${x}/${y}/${z}`;
    }

    const response = await fetch(url, {
      credentials: 'include',
      headers: {
        'Accept': 'application/json',
      },
    });

    if (response.status === 404) {
      blockDetails.value = null;
      return;
    }

    if (!response.ok) {
      throw new Error(`Failed to load block details: ${response.statusText}`);
    }

    blockDetails.value = await response.json();
  } catch (err) {
    console.error('Failed to load block details:', err);
    blockDetails.value = null;
  } finally {
    loadingBlockDetails.value = false;
  }
}

// Handle canvas click for block selection
function handleCanvasClick(event: MouseEvent) {
  const canvas = canvasRef.value;
  if (!canvas) return;

  const rect = canvas.getBoundingClientRect();
  const clickX = event.clientX - rect.left;
  const clickY = event.clientY - rect.top;

  // Find clicked block using reverse rendering order (front to back)
  const sortedBlocks = [...blockCoordinates.value].sort((a, b) => {
    if (a.y !== b.y) return b.y - a.y; // Reverse Y order
    return (b.x + b.z) - (a.x + a.z); // Reverse depth order
  });

  for (const block of sortedBlocks) {
    const { x, y, z } = block;

    // Check if click is inside the block's bounds (simple bounding box test)
    const corners = [
      worldToScreen(x, y, z),
      worldToScreen(x + 1, y, z),
      worldToScreen(x + 1, y, z + 1),
      worldToScreen(x, y, z + 1),
      worldToScreen(x, y + 1, z),
      worldToScreen(x + 1, y + 1, z),
      worldToScreen(x + 1, y + 1, z + 1),
      worldToScreen(x, y + 1, z + 1),
    ];

    const minX = Math.min(...corners.map(c => c.x));
    const maxX = Math.max(...corners.map(c => c.x));
    const minY = Math.min(...corners.map(c => c.y));
    const maxY = Math.max(...corners.map(c => c.y));

    if (clickX >= minX && clickX <= maxX && clickY >= minY && clickY <= maxY) {
      selectedBlock.value = { x, y, z };
      loadBlockDetails(x, y, z);
      drawGrid();
      return;
    }
  }
}

// Handle canvas hover (optional - for highlighting)
function handleCanvasHover(event: MouseEvent) {
  // Could implement hover highlighting here
}

// Handle navigation from NavigateSelectedBlockComponent
function handleNavigate(position: { x: number; y: number; z: number }) {
  selectedBlock.value = position;

  // Check if block exists at this position
  const blockExists = blockCoordinates.value.some(
    b => b.x === position.x && b.y === position.y && b.z === position.z
  );

  if (blockExists) {
    loadBlockDetails(position.x, position.y, position.z);
  } else {
    blockDetails.value = null;
  }

  drawGrid();
}

// Handle close
function handleClose() {
  emit('close');
}

// Watch for canvas size changes
watch([canvasWidth, canvasHeight], () => {
  setTimeout(() => drawGrid(), 50);
});

// Watch for block coordinates changes
watch(blockCoordinates, () => {
  drawGrid();
});

// Lifecycle
onMounted(async () => {
  await loadBlockCoordinates();
});
</script>
