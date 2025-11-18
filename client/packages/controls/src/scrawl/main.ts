/**
 * Scrawl Script Editor - Main entry point
 */

import { createApp } from 'vue';
import ScrawlApp from './ScrawlApp.vue';
import '../style.css';

const app = createApp(ScrawlApp);
app.mount('#app');
