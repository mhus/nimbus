<template>
  <div class="mc-asset-editor flex flex-col h-full overflow-hidden">
    <!-- Dual Panels (flexible height, no scroll here) -->
    <div class="flex-1 grid grid-cols-2 gap-4 p-4 overflow-hidden">
      <!-- Left Panel -->
      <AssetPanel
        ref="leftPanelRef"
        v-model:world-id="leftWorldId"
        v-model:current-path="leftCurrentPath"
        v-model:selected-files="leftSelectedFiles"
        panel="left"
        @file-dropped="handleFileDrop"
        @asset-moved="handleAssetMoved"
      />

      <!-- Right Panel -->
      <AssetPanel
        ref="rightPanelRef"
        v-model:world-id="rightWorldId"
        v-model:current-path="rightCurrentPath"
        v-model:selected-files="rightSelectedFiles"
        panel="right"
        @file-dropped="handleFileDrop"
        @asset-moved="handleAssetMoved"
      />
    </div>

    <!-- Info Bar (Bottom, fixed height) -->
    <InfoBar
      :selected-asset="selectedAsset"
      :folder-stats="folderStats"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue';
import { useWorld } from '@/composables/useWorld';
import AssetPanel from '@material/components/AssetPanel.vue';
import InfoBar from '@material/components/InfoBar.vue';
import type { Asset } from '@/services/AssetService';

const { currentWorldId, worlds, loadWorlds } = useWorld();

// Panel refs for triggering refresh
const leftPanelRef = ref<any>(null);
const rightPanelRef = ref<any>(null);

// Panel states
const leftWorldId = ref<string>('');
const rightWorldId = ref<string>('');
const leftCurrentPath = ref<string>('');
const rightCurrentPath = ref<string>('');
const leftSelectedFiles = ref<Asset[]>([]);
const rightSelectedFiles = ref<Asset[]>([]);

// Trigger keys to force reload
const leftTrigger = ref(0);
const rightTrigger = ref(0);

// Load main worlds and initialize both panels with first world
onMounted(async () => {
  await loadWorlds('mainOnly');

  // Initialize both panels with first available world
  if (worlds.value.length > 0) {
    const firstWorld = worlds.value[0].worldId;
    leftWorldId.value = firstWorld;
    rightWorldId.value = firstWorld;
  }
});

// Selected asset for info bar (last selected from either panel)
const selectedAsset = computed<Asset | null>(() => {
  if (leftSelectedFiles.value.length > 0) {
    return leftSelectedFiles.value[leftSelectedFiles.value.length - 1];
  }
  if (rightSelectedFiles.value.length > 0) {
    return rightSelectedFiles.value[rightSelectedFiles.value.length - 1];
  }
  return null;
});

// Folder statistics (placeholder)
const folderStats = computed(() => {
  // TODO: Implement folder statistics calculation
  return {
    assetCount: 0,
    totalSize: 0,
  };
});

/**
 * Handle file dropped from browser
 */
const handleFileDrop = async (event: { panel: 'left' | 'right'; files: FileList; targetPath: string }) => {
  console.log('File dropped:', event);

  const worldId = event.panel === 'left' ? leftWorldId.value : rightWorldId.value;

  if (!worldId) {
    alert('No world selected in ' + event.panel + ' panel');
    return;
  }

  try {
    const { assetService } = await import('@/services/AssetService');

    // Upload all dropped files
    for (let i = 0; i < event.files.length; i++) {
      const file = event.files[i];
      const fileName = file.name;

      // Build target path
      const targetPath = event.targetPath
        ? `${event.targetPath}/${fileName}`
        : fileName;

      console.log(`Uploading: ${fileName} -> ${targetPath}`);

      await assetService.uploadAsset(worldId, targetPath, file);
      console.log(`Uploaded: ${targetPath}`);
    }

    // Reload the target panel
    if (event.panel === 'left' && leftPanelRef.value) {
      leftPanelRef.value.reload();
    } else if (event.panel === 'right' && rightPanelRef.value) {
      rightPanelRef.value.reload();
    }

    console.log(`Successfully uploaded ${event.files.length} file(s)`);

  } catch (e) {
    console.error('Failed to upload files:', e);
    alert('Failed to upload files: ' + (e instanceof Error ? e.message : 'Unknown error'));
  }
};

/**
 * Handle asset moved between panels
 */
const handleAssetMoved = (data: { sourcePanel: string; sourceWorldId: string; moved: boolean }) => {
  console.log('Asset moved event:', data);

  // If asset was moved (not copied), refresh the source panel
  if (data.moved) {
    if (data.sourcePanel === 'left' && leftPanelRef.value) {
      leftPanelRef.value.reload();
    } else if (data.sourcePanel === 'right' && rightPanelRef.value) {
      rightPanelRef.value.reload();
    }
  }
};
</script>

<style scoped>
.mc-asset-editor {
  height: 100%;
  display: flex;
  flex-direction: column;
}
</style>
