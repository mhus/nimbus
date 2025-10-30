<template>
  <div class="min-h-screen flex flex-col">
    <!-- Header -->
    <AppHeader />

    <!-- Main Content -->
    <main class="flex-1 container mx-auto px-4 py-6">
      <TabNavigation />
    </main>

    <!-- Footer (optional) -->
    <footer class="footer footer-center p-4 bg-base-300 text-base-content">
      <div>
        <p>Nimbus Material Editor v2.0.0</p>
      </div>
    </footer>
  </div>
</template>

<script setup lang="ts">
import AppHeader from '@material/components/AppHeader.vue';
import TabNavigation from '@material/components/TabNavigation.vue';
</script>
