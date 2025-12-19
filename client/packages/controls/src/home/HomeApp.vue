<template>
  <div class="min-h-screen flex flex-col bg-gray-50">
    <!-- Header -->
    <header class="bg-blue-600 text-white shadow-lg">
      <div class="container mx-auto px-4 py-6">
        <div class="flex items-center justify-between">
          <div>
            <h1 class="text-3xl font-bold">Nimbus Editors</h1>
            <p class="text-blue-100 mt-2">Admin tools for managing Nimbus game content</p>

            <!-- Session Info -->
            <div v-if="authStatus?.authenticated" class="text-blue-100 text-sm mt-3 flex flex-wrap items-center gap-x-4 gap-y-1">
              <div class="flex items-center gap-2">
                <span class="font-semibold">User:</span>
                <span class="font-mono">{{ authStatus.userId }}</span>
              </div>
              <div v-if="authStatus.actor" class="flex items-center gap-2">
                <span class="font-semibold">Actor:</span>
                <span class="badge badge-sm bg-blue-500 text-white border-none">{{ authStatus.actor }}</span>
              </div>
              <div v-if="authStatus.roles && authStatus.roles.length > 0" class="flex items-center gap-2">
                <span class="font-semibold">Roles:</span>
                <div class="flex gap-1">
                  <span v-for="r in authStatus.roles" :key="r" class="badge badge-sm bg-blue-400 text-white border-none">
                    {{ r }}
                  </span>
                </div>
              </div>
              <div class="flex items-center gap-2">
                <span class="font-semibold">Type:</span>
                <span class="badge badge-sm" :class="authStatus.agent ? 'bg-purple-500' : 'bg-green-500'" style="color: white; border: none;">
                  {{ authStatus.agent ? 'Agent' : 'Session' }}
                </span>
              </div>
              <div v-if="authStatus.worldId" class="flex items-center gap-2">
                <span class="font-semibold">World:</span>
                <span class="font-mono text-xs">{{ authStatus.worldId }}</span>
              </div>
              <div v-if="authStatus.characterId" class="flex items-center gap-2">
                <span class="font-semibold">Character:</span>
                <span class="font-mono text-xs">{{ authStatus.characterId }}</span>
              </div>
              <div v-if="authStatus.sessionId" class="flex items-center gap-2">
                <span class="font-semibold">Session:</span>
                <span class="font-mono text-xs">{{ authStatus.sessionId.substring(0, 20) }}...</span>
              </div>
            </div>

            <!-- Not Authenticated Info -->
            <div v-else-if="authStatus && !authStatus.authenticated" class="text-yellow-100 text-sm mt-3 flex items-center gap-2 bg-yellow-600 bg-opacity-30 px-3 py-2 rounded">
              <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
              </svg>
              <span class="font-semibold">Not authenticated - Please log in to access editors</span>
            </div>
          </div>
          <div class="flex gap-2">
            <a href="./dev-login.html" class="p-2 rounded bg-blue-700 hover:bg-blue-800 transition-colors" title="Login">
              <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 16l-4-4m0 0l4-4m-4 4h14m-5 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h7a3 3 0 013 3v1" />
              </svg>
            </a>
            <a v-if="authStatus?.authenticated" href="./logout.html" class="p-2 rounded bg-red-600 hover:bg-red-700 transition-colors" title="Logout">
              <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1" />
              </svg>
            </a>
          </div>
        </div>
      </div>
    </header>

    <!-- Loading State -->
    <main v-if="loading" class="flex-1 flex items-center justify-center">
      <span class="loading loading-spinner loading-lg"></span>
    </main>

    <!-- Main Content -->
    <main v-else class="flex-1 container mx-auto px-4 py-8">
      <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        <!-- Region Editor -->
        <EditorCard
          v-if="hasAccess('REGION_EDITOR')"
          title="Region Editor"
          description="Manage game regions and maintainers"
          url="./region-editor.html"
        >
          <svg class="w-8 h-8 text-blue-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3.055 11H5a2 2 0 012 2v1a2 2 0 002 2 2 2 0 012 2v2.945M8 3.935V5.5A2.5 2.5 0 0010.5 8h.5a2 2 0 012 2 2 2 0 104 0 2 2 0 012-2h1.064M15 20.488V18a2 2 0 012-2h3.064M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
        </EditorCard>

        <!-- User Editor -->
        <EditorCard
          v-if="hasAccess('USER_EDITOR')"
          title="User Editor"
          description="Manage users, roles, and settings"
          url="./user-editor.html"
        >
          <svg class="w-8 h-8 text-green-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z" />
          </svg>
        </EditorCard>

        <!-- Character Editor -->
        <EditorCard
          v-if="hasAccess('CHARACTER_EDITOR')"
          title="Character Editor"
          description="Manage player characters and skills"
          url="./character-editor.html"
        >
          <svg class="w-8 h-8 text-purple-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
          </svg>
        </EditorCard>

        <!-- World Editor -->
        <EditorCard
          v-if="hasAccess('WORLD_EDITOR')"
          title="World Editor"
          description="Manage game worlds and settings"
          url="./world-editor.html"
        >
          <svg class="w-8 h-8 text-indigo-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3.055 11H5a2 2 0 012 2v1a2 2 0 002 2 2 2 0 012 2v2.945M8 3.935V5.5A2.5 2.5 0 0010.5 8h.5a2 2 0 012 2 2 2 0 104 0 2 2 0 012-2h1.064" />
          </svg>
        </EditorCard>

        <!-- Entity Editor -->
        <EditorCard
          v-if="hasAccess('ENTITY_EDITOR')"
          title="Entity Editor"
          description="Manage game entities and objects"
          url="./entity-editor.html"
        >
          <svg class="w-8 h-8 text-red-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M20 7l-8-4-8 4m16 0l-8 4m8-4v10l-8 4m0-10L4 7m8 4v10M4 7v10l8 4" />
          </svg>
        </EditorCard>

        <!-- Entity Model Editor -->
        <EditorCard
          v-if="hasAccess('ENTITYMODEL_EDITOR')"
          title="Entity Model Editor"
          description="Manage entity templates and models"
          url="./entitymodel-editor.html"
        >
          <svg class="w-8 h-8 text-yellow-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 5a1 1 0 011-1h4a1 1 0 011 1v7a1 1 0 01-1 1H5a1 1 0 01-1-1V5zM14 5a1 1 0 011-1h4a1 1 0 011 1v7a1 1 0 01-1 1h-4a1 1 0 01-1-1V5zM4 16a1 1 0 011-1h4a1 1 0 011 1v3a1 1 0 01-1 1H5a1 1 0 01-1-1v-3z" />
          </svg>
        </EditorCard>

        <!-- Backdrop Editor -->
        <EditorCard
          v-if="hasAccess('BACKDROP_EDITOR')"
          title="Backdrop Editor"
          description="Manage scene backdrops and backgrounds"
          url="./backdrop-editor.html"
        >
          <svg class="w-8 h-8 text-pink-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z" />
          </svg>
        </EditorCard>

        <!-- Material Editor -->
        <EditorCard
          v-if="hasAccess('MATERIAL_EDITOR')"
          title="Material Editor"
          description="Manage materials and textures"
          url="./material-editor.html"
        >
          <svg class="w-8 h-8 text-teal-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 21a4 4 0 01-4-4V5a2 2 0 012-2h4a2 2 0 012 2v12a4 4 0 01-4 4zm0 0h12a2 2 0 002-2v-4a2 2 0 00-2-2h-2.343M11 7.343l1.657-1.657a2 2 0 012.828 0l2.829 2.829a2 2 0 010 2.828l-8.486 8.485M7 17h.01" />
          </svg>
        </EditorCard>

        <!-- Block Type Editor -->
        <EditorCard
          v-if="hasAccess('BLOCKTYPE_EDITOR')"
          title="BlockType Editor"
          description="Manage block types and definitions"
          url="./blocktype-editor.html"
        >
          <svg class="w-8 h-8 text-cyan-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" />
          </svg>
        </EditorCard>

        <!-- Asset Editor -->
        <EditorCard
          v-if="hasAccess('ASSET_EDITOR')"
          title="Asset Editor"
          description="Manage game assets and resources"
          url="./asset-editor.html"
        >
          <svg class="w-8 h-8 text-orange-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
          </svg>
        </EditorCard>

        <!-- Layer Editor -->
        <EditorCard
          v-if="hasAccess('LAYER_EDITOR')"
          title="Layer Editor"
          description="Manage rendering layers"
          url="./layer-editor.html"
        >
          <svg class="w-8 h-8 text-lime-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" />
          </svg>
        </EditorCard>

        <!-- Item Editor -->
        <EditorCard
          v-if="hasAccess('ITEM_EDITOR')"
          title="Item Editor"
          description="Manage game items and inventory"
          url="./item-editor.html"
        >
          <svg class="w-8 h-8 text-amber-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M20 7l-8-4-8 4m16 0l-8 4m8-4v10l-8 4m0-10L4 7m8 4v10M4 7v10l8 4" />
          </svg>
        </EditorCard>

        <!-- ItemType Editor -->
        <EditorCard
          v-if="hasAccess('ITEMTYPE_EDITOR')"
          title="ItemType Editor"
          description="Manage item type definitions"
          url="./itemtype-editor.html"
        >
          <svg class="w-8 h-8 text-emerald-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 7h.01M7 3h5c.512 0 1.024.195 1.414.586l7 7a2 2 0 010 2.828l-7 7a2 2 0 01-2.828 0l-7-7A1.994 1.994 0 013 12V7a4 4 0 014-4z" />
          </svg>
        </EditorCard>

        <!-- Scrawl Editor -->
        <EditorCard
          v-if="hasAccess('SCRAWL_EDITOR')"
          title="Scrawl Script Editor"
          description="Create and edit game scripts"
          url="./scrawl-editor.html"
        >
          <svg class="w-8 h-8 text-violet-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 20l4-16m4 4l4 4-4 4M6 16l-4-4 4-4" />
          </svg>
        </EditorCard>

        <!-- Block Editor -->
        <EditorCard
          v-if="hasAccess('BLOCK_EDITOR')"
          title="Block Instance Editor"
          description="Edit individual block instances"
          url="./block-editor.html"
        >
          <svg class="w-8 h-8 text-fuchsia-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 5a1 1 0 011-1h4a1 1 0 011 1v7a1 1 0 01-1 1H5a1 1 0 01-1-1V5zM14 5a1 1 0 011-1h4a1 1 0 011 1v7a1 1 0 01-1 1h-4a1 1 0 01-1-1V5z" />
          </svg>
        </EditorCard>

        <!-- Hex Grid Editor -->
        <EditorCard
          v-if="hasAccess('HEXGRID_EDITOR')"
          title="Hex Grid Editor"
          description="Manage hexagonal world grids"
          url="./hex-editor.html"
        >
          <svg class="w-8 h-8 text-sky-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 21h10a2 2 0 002-2V9.414a1 1 0 00-.293-.707l-5.414-5.414A1 1 0 0012.586 3H7a2 2 0 00-2 2v14a2 2 0 002 2z" />
          </svg>
        </EditorCard>

        <!-- Job Controller -->
        <EditorCard
          v-if="hasAccess('JOB_CONTROLLER')"
          title="Job Controller"
          description="Manage and monitor background jobs"
          url="./job-control.html"
        >
          <svg class="w-8 h-8 text-indigo-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
        </EditorCard>

        <!-- Anything Editor -->
        <EditorCard
          v-if="hasAccess('ANYTHING_EDITOR')"
          title="Anything Editor"
          description="Manage flexible data storage with region/world scoping"
          url="./anything-editor.html"
        >
          <svg class="w-8 h-8 text-slate-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 7v10c0 2.21 3.582 4 8 4s8-1.79 8-4V7M4 7c0 2.21 3.582 4 8 4s8-1.79 8-4M4 7c0-2.21 3.582-4 8-4s8 1.79 8 4m0 5c0 2.21-3.582 4-8 4s-8-1.79-8-4" />
          </svg>
        </EditorCard>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { authService, type AuthStatus } from './services/AuthService';
import EditorCard from './components/EditorCard.vue';

const loading = ref(true);
const authStatus = ref<AuthStatus | null>(null);

// Role to editor mapping
const editorRoleMap: Record<string, string[]> = {
  'REGION_EDITOR': ['SECTOR_ADMIN'],
  'USER_EDITOR': ['SECTOR_ADMIN'],
  'CHARACTER_EDITOR': ['SECTOR_ADMIN'],
  'WORLD_EDITOR': ['SECTOR_ADMIN'],
  'ENTITY_EDITOR': ['SECTOR_ADMIN', 'WORLD_EDITOR'],
  'ENTITYMODEL_EDITOR': ['SECTOR_ADMIN', 'ENTITYMODEL_EDITOR', 'WORLD_EDITOR'],
  'BACKDROP_EDITOR': ['SECTOR_ADMIN', 'BACKDROP_EDITOR', 'WORLD_EDITOR'],
  'MATERIAL_EDITOR': ['SECTOR_ADMIN', 'MATERIAL_EDITOR', 'WORLD_EDITOR'],
  'BLOCKTYPE_EDITOR': ['SECTOR_ADMIN', 'BLOCKTYPE_EDITOR', 'WORLD_EDITOR'],
  'ASSET_EDITOR': ['SECTOR_ADMIN', 'ASSET_EDITOR', 'WORLD_EDITOR'],
  'LAYER_EDITOR': ['SECTOR_ADMIN', 'LAYER_EDITOR', 'WORLD_EDITOR'],
  'ITEM_EDITOR': ['SECTOR_ADMIN', 'ITEM_EDITOR', 'WORLD_EDITOR'],
  'ITEMTYPE_EDITOR': ['SECTOR_ADMIN', 'ITEMTYPE_EDITOR', 'WORLD_EDITOR'],
  'SCRAWL_EDITOR': ['SECTOR_ADMIN', 'SCRAWL_EDITOR', 'WORLD_EDITOR'],
  'BLOCK_EDITOR': ['SECTOR_ADMIN', 'BLOCK_EDITOR', 'WORLD_EDITOR'],
  'HEXGRID_EDITOR': ['SECTOR_ADMIN', 'HEXGRID_EDITOR', 'WORLD_EDITOR'],
  'JOB_CONTROLLER': ['SECTOR_ADMIN', 'JOB_CONTROLLER', 'WORLD_EDITOR'],
  'ANYTHING_EDITOR': ['SECTOR_ADMIN', 'ANYTHING_EDITOR', 'WORLD_EDITOR'],
};

/**
 * Check if user has access to a specific editor
 */
const hasAccess = (editorKey: string): boolean => {
  if (!authStatus.value || !authStatus.value.authenticated) {
    return false;
  }

  const userRoles = authStatus.value.roles || [];

  // If no roles (e.g., agent or serverToServer), allow all editors
  if (userRoles.length === 0) {
    return true;
  }

  // Admin has access to everything
  if (userRoles.includes('ADMIN')) {
    return true;
  }

  // Check if user has any of the required roles for this editor
  const requiredRoles = editorRoleMap[editorKey] || [];
  return userRoles.some(role => requiredRoles.includes(role));
};

/**
 * Load authentication status
 */
const loadAuthStatus = async () => {
  loading.value = true;

  try {
    authStatus.value = await authService.getStatus();
  } catch (error) {
    console.error('[HomeApp] Failed to load auth status:', error);
  } finally {
    loading.value = false;
  }
};

onMounted(() => {
  loadAuthStatus();
});
</script>
