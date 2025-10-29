import { defineConfig } from 'vite';
import { resolve } from 'path';

export default defineConfig(({ mode }) => {
  const isEditor = mode === 'editor';
  const isViewer = mode === 'viewer' || mode === 'development';

  return {
    root: '.',
    build: {
      outDir: isEditor ? 'dist/editor' : 'dist/viewer',
      emptyOutDir: true,
      chunkSizeWarningLimit: 2000, // Increase to 2MB for BabylonJS
      rollupOptions: {
        input: {
          main: resolve(__dirname, 'index.html'),
        },
      },
    },
    define: {
      // Global constants for conditional compilation
      __EDITOR__: JSON.stringify(isEditor),
      __VIEWER__: JSON.stringify(isViewer),
      __BUILD_MODE__: JSON.stringify(mode),
    },
    resolve: {
      alias: {
        '@': resolve(__dirname, './src'),
        '@nimbus/shared': resolve(__dirname, '../shared/src'),
      },
    },
    server: {
      port: 3001,
      open: true,
    },
  };
});
