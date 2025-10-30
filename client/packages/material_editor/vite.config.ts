import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';
import { resolve } from 'path';

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 3002,
    open: true,
  },
  resolve: {
    alias: {
      '@': resolve(__dirname, './src'),
      '@nimbus/shared': resolve(__dirname, '../shared/src'),
    },
  },
  build: {
    outDir: 'dist',
    sourcemap: true,
  },
});
