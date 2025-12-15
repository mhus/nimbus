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
      '@blocktype': resolve(__dirname, './src/blocktype'),
      '@asset': resolve(__dirname, './src/asset'),
      '@layer': resolve(__dirname, './src/layer'),
      '@block': resolve(__dirname, './src/block'),
      '@editconfig': resolve(__dirname, './src/editconfig'),
      '@devlogin': resolve(__dirname, './src/devlogin'),
      '@nimbus/shared': resolve(__dirname, '../shared/src'),
    },
  },
  build: {
    outDir: 'dist',
    sourcemap: true,
    rollupOptions: {
      input: {
        'index': resolve(__dirname, 'index.html'),
        'material-editor': resolve(__dirname, 'material-editor.html'),
        'blocktype-editor': resolve(__dirname, 'blocktype-editor.html'),
        'asset-editor': resolve(__dirname, 'asset-editor.html'),
        'layer-editor': resolve(__dirname, 'layer-editor.html'),
        'block-editor': resolve(__dirname, 'block-editor.html'),
        'edit-config': resolve(__dirname, 'edit-config.html'),
        'item-editor': resolve(__dirname, 'item-editor.html'),
        'itemtype-editor': resolve(__dirname, 'itemtype-editor.html'),
        'scrawl-editor': resolve(__dirname, 'scrawl-editor.html'),
        'dev-login': resolve(__dirname, 'dev-login.html'),
        'region-editor': resolve(__dirname, 'region-editor.html'),
        'user-editor': resolve(__dirname, 'user-editor.html'),
        'character-editor': resolve(__dirname, 'character-editor.html'),
        'world-editor': resolve(__dirname, 'world-editor.html'),
        'entity-editor': resolve(__dirname, 'entity-editor.html'),
        'entitymodel-editor': resolve(__dirname, 'entitymodel-editor.html'),
        'backdrop-editor': resolve(__dirname, 'backdrop-editor.html'),
      },
    },
  },
});
