/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_INSTITUTION_NAME?: string;
  readonly VITE_INSTITUTION_EMAIL_DOMAIN?: string;
  readonly VITE_SHOW_DEMO_ACCOUNTS?: string;
  readonly VITE_API_BASE_URL?: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
