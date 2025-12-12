<template>
  <div class="min-h-screen flex flex-col">
    <!-- Header -->
    <header class="navbar bg-base-200 shadow-lg">
      <div class="flex-1">
        <a class="btn btn-ghost normal-case text-xl">Nimbus Region Editor</a>
      </div>
    </header>

    <!-- Main Content -->
    <main class="flex-1 container mx-auto px-4 py-6">
      <RegionList v-if="!selectedRegionId" @select="handleRegionSelect" @create="handleCreateNew" />
      <RegionEditor v-else :region-id="selectedRegionId" @back="handleBack" @saved="handleSaved" />
    </main>

    <!-- Footer -->
    <footer class="footer footer-center p-4 bg-base-300 text-base-content">
      <div>
        <p>Nimbus Region Editor v1.0.0</p>
      </div>
    </footer>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import RegionList from './views/RegionList.vue';
import RegionEditor from './views/RegionEditor.vue';

const selectedRegionId = ref<string | null>(null);

const handleRegionSelect = (id: string) => {
  selectedRegionId.value = id;
};

const handleCreateNew = () => {
  selectedRegionId.value = 'new';
};

const handleBack = () => {
  selectedRegionId.value = null;
};

const handleSaved = () => {
  selectedRegionId.value = null;
};
</script>
