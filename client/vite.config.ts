import { defineConfig } from "vite"
import vue from "@vitejs/plugin-vue"
import { resolve } from "path"
import fs from "fs"

const uploadsDir = resolve(__dirname, "..", "uploads")

export default defineConfig({
  plugins: [
    vue(),
    {
      name: "serve-uploads",
      configureServer(server) {
        server.middlewares.use("/uploads/", (req, res, next) => {
          const filename = req.url?.replace(/^\//, "") || ""
          const filePath = resolve(uploadsDir, filename)
          if (filePath.startsWith(uploadsDir) && fs.existsSync(filePath)) {
            const ext = filename.split(".").pop()?.toLowerCase() || ""
            const mime = {jpg:"image/jpeg",jpeg:"image/jpeg",png:"image/png",webp:"image/webp",gif:"image/gif",bmp:"image/bmp"}
            res.setHeader("Content-Type", mime[ext] || "application/octet-stream")
            fs.createReadStream(filePath).pipe(res)
          } else {
            next()
          }
        })
      },
    },
  ],
  resolve: { alias: { "@": resolve(__dirname, "src") } },
  server: {
    port: 5173,
    proxy: { "/api": { target: "http://localhost:8080", changeOrigin: true } },
    fs: { allow: [".."] },
  },
})
