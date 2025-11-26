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
      hmr: {
        overlay: true,
      },
      watch: {
        // Use polling for better reliability with large projects
        usePolling: false,
        // Increase file descriptor limit awareness
        ignored: ['**/node_modules/**', '**/dist/**'],
      },
    },
    optimizeDeps: {
      // Force re-optimization on startup
      force: false,
      // Increase esbuild memory and threads
      esbuildOptions: {
        // More memory for large dependencies
        logLevel: 'info',
      },
    },
  };
});
