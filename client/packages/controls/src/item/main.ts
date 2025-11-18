/**
 * Item Editor - Main entry point
 */

import { createApp } from 'vue';
import ItemApp from './ItemApp.vue';
import '../style.css';

const app = createApp(ItemApp);
app.mount('#app');
