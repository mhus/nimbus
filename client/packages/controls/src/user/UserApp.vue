<template>
  <div class="min-h-screen flex flex-col">
    <!-- Header -->
    <header class="navbar bg-base-200 shadow-lg">
      <div class="flex-1">
        <a class="btn btn-ghost normal-case text-xl">Nimbus User Editor</a>
      </div>
    </header>

    <!-- Main Content -->
    <main class="flex-1 container mx-auto px-4 py-6">
      <UserList v-if="!selectedUsername" @select="handleUserSelect" />
      <UserEditor v-else :username="selectedUsername" @back="handleBack" @saved="handleSaved" />
    </main>

    <!-- Footer -->
    <footer class="footer footer-center p-4 bg-base-300 text-base-content">
      <div>
        <p>Nimbus User Editor v1.0.0</p>
      </div>
    </footer>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import UserList from './views/UserList.vue';
import UserEditor from './views/UserEditor.vue';

const selectedUsername = ref<string | null>(null);

const handleUserSelect = (username: string) => {
  selectedUsername.value = username;
};

const handleBack = () => {
  selectedUsername.value = null;
};

const handleSaved = () => {
  selectedUsername.value = null;
};
</script>
