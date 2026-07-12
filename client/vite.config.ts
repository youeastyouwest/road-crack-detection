import { defineConfig } from "vite"
import vue from "@vitejs/plugin-vue"
import { resolve } from "path"
import fs from "fs"
import http from "http"

const uploadsDir = resolve(__dirname, "..", "uploads")

export default defineConfig({
  plugins: [
    vue(),
    {
      name: "serve-uploads",
      configureServer(server) {
        server.middlewares.use("/uploads/", (req, res, next) => {
          const rawUrl = req.url || ""
          const filename = decodeURIComponent(rawUrl.replace(/^\//, "").split("?")[0]) || ""
          const filePath = resolve(uploadsDir, filename)
          if (filePath.startsWith(uploadsDir) && fs.existsSync(filePath) && fs.statSync(filePath).isFile()) {
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
    // 手动代理所有 /api 请求，确保 PUT/DELETE/PATCH 的 body 正确转发
    {
      name: "fix-api-proxy-body",
      configureServer(server) {
        server.middlewares.use("/api", (req, res) => {
          console.log("[proxy] intercepted:", req.method, req.url)
          const chunks: Buffer[] = []
          req.on("data", (chunk: Buffer) => chunks.push(chunk))
          req.on("end", () => {
            const body = Buffer.concat(chunks)
            const path = (req.url || "").startsWith("/api") ? req.url : "/api" + (req.url || "")
            const headers: Record<string, string> = {}
            for (const [k, v] of Object.entries(req.headers)) {
              if (v !== undefined) headers[k] = Array.isArray(v) ? v.join(", ") : v
            }
            headers["host"] = "localhost:8080"
            headers["content-length"] = String(body.length)

            const proxyReq = http.request(
              {
                hostname: "localhost",
                port: 8080,
                path: path,
                method: req.method,
                headers,
              },
              (proxyRes) => {
                res.writeHead(proxyRes.statusCode || 200, proxyRes.headers)
                proxyRes.pipe(res)
              }
            )
            proxyReq.on("error", (err) => {
              console.error("[proxy-body] error:", err.message)
              res.statusCode = 502
              res.end("Proxy Error")
            })
            if (body.length > 0) proxyReq.write(body)
            proxyReq.end()
          })
        })
      },
    },
  ],
  resolve: {
    alias: { "@": resolve(__dirname, "src") },
    preserveSymlinks: true,
  },
  server: {
    port: 5173,
    fs: { allow: [".."] },
  },
})
