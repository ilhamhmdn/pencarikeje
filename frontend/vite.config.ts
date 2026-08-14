import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    port: 5173,
    // The API's CORS allow-list names this origin explicitly, so the dev server
    // must stay on it.
    strictPort: true,
  },
})
