/// <reference types="vite/client" />

declare module '*.vue' {
  import type { DefineComponent } from 'vue';
  const component: DefineComponent<{}, {}, any>;
  export default component;
}

interface ImportMetaEnv {
  readonly VITE_API_URL: string;
  readonly VITE_WORLD_ID: string;
  readonly VITE_API_USERNAME?: string;
  readonly VITE_API_PASSWORD?: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
