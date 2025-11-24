import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';
import { resolve } from 'path';

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 3002,
    open: '/material-editor.html',
  },
  resolve: {
    alias: {
      '@': resolve(__dirname, './src'),
      '@components': resolve(__dirname, './src/components'),
      '@editors': resolve(__dirname, './src/editors'),
      '@material': resolve(__dirname, './src/material'),
      '@block': resolve(__dirname, './src/block'),
      '@editconfig': resolve(__dirname, './src/editconfig'),
      '@nimbus/shared': resolve(__dirname, '../shared/src'),
    },
  },
  build: {
    outDir: 'dist',
    sourcemap: true,
    rollupOptions: {
      input: {
        'material-editor': resolve(__dirname, 'material-editor.html'),
        'block-editor': resolve(__dirname, 'block-editor.html'),
        'edit-config': resolve(__dirname, 'edit-config.html'),
        'item-editor': resolve(__dirname, 'item-editor.html'),
        'itemtype-editor': resolve(__dirname, 'itemtype-editor.html'),
        'scrawl-editor': resolve(__dirname, 'scrawl-editor.html'),
      },
    },
  },
});
