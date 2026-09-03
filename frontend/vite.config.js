import { defineConfig } from 'vite'
import { fileURLToPath, URL } from 'node:url'

export default defineConfig({
  // 使用带模板编译器的 Vue 版本，允许我们直接在 main.js 中写 template。
  resolve: {
    alias: {
      vue: fileURLToPath(new URL('./node_modules/vue/dist/vue.esm-bundler.js', import.meta.url))
    }
  },
  // strictPort 防止端口被占用时自动换成 5176，避免触发后端跨域限制。
  server: { port: 5175, strictPort: true }
})
