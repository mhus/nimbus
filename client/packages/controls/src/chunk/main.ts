import { createApp } from 'vue';
import ChunkApp from './ChunkApp.vue';
import '../style.css';

const app = createApp(ChunkApp);
app.mount('#app');
