<template>
  <div class="card bg-base-100 shadow-xl">
    <div class="card-body">
      <TabGroup>
        <TabList class="tabs tabs-boxed">
          <Tab v-slot="{ selected }" class="tab">
            <span :class="{ 'tab-active': selected }">Layers</span>
          </Tab>
        </TabList>

        <TabPanels class="mt-6">
          <TabPanel>
            <LayerEditor />
          </TabPanel>
        </TabPanels>
      </TabGroup>
    </div>
  </div>
</template>

<script setup lang="ts">
import { TabGroup, TabList, Tab, TabPanels, TabPanel } from '@headlessui/vue';
import LayerEditor from '../views/LayerEditor.vue';
</script>
