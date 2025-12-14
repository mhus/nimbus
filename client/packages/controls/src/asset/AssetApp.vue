<template>
  <div class="min-h-screen flex flex-col">
    <!-- Header -->
    <AppHeader />

    <!-- Main Content -->
    <main class="flex-1 container mx-auto px-4 py-6">
      <div class="card bg-base-100 shadow-xl">
        <div class="card-body">
          <AssetEditor />
        </div>
      </div>
    </main>

    <!-- Footer -->
    <footer class="footer footer-center p-4 bg-base-300 text-base-content">
      <div>
        <p>Nimbus Asset Editor v2.0.0</p>
      </div>
    </footer>
  </div>
</template>

<script setup lang="ts">
import AppHeader from '@material/components/AppHeader.vue';
import AssetEditor from '@material/views/AssetEditor.vue';
</script>
