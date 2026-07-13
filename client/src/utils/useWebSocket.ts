import { ref } from "vue"
import type { AlertMessageResponse } from "@/types"

/**
 * WebSocket connection manager for real-time alerts and updates.
 * Connects to the Spring WebSocket STOMP endpoint at /ws.
 */
export function useWebSocket() {
  const connected = ref(false)
  const alerts = ref<AlertMessageResponse[]>([])
  let ws: WebSocket | null = null
  let subId = 0
  let heartbeatTimer: ReturnType<typeof setInterval> | null = null

  function connect(): Promise<void> {
    return new Promise((resolve, reject) => {
      try {
        const proto = window.location.protocol === "https:" ? "wss:" : "ws:"
        ws = new WebSocket(proto + "//" + window.location.host + "/ws")
        ws.binaryType = "arraybuffer"

        ws.onopen = () => {
          sendFrame("CONNECT", { "accept-version": "1.2", host: "localhost" })
          connected.value = true
          heartbeatTimer = setInterval(() => ws?.send("\n"), 15000)
          resolve()
        }

        ws.onmessage = (e) => {
          const raw = typeof e.data === "string" ? e.data : new TextDecoder().decode(e.data)
          if (raw.startsWith("CONNECTED")) {
            connected.value = true
          } else if (raw.startsWith("MESSAGE")) {
            const hdr: Record<string, string> = {}
            const lines = raw.split("\n")
            let bodyStart = false
            let body = ""
            for (const line of lines) {
              if (bodyStart) { body += line; continue }
              if (line === "") { bodyStart = true; continue }
              const sep = line.indexOf(":")
              if (sep > 0) hdr[line.slice(0, sep).trim().toLowerCase()] = line.slice(sep + 1).trim()
            }
            const destination = hdr["destination"] || ""
            try {
              const payload = JSON.parse(body.replace(/\0$/, "").trim())
              if (destination.includes("alert") || destination.includes("alerts")) {
                alerts.value.push(payload as AlertMessageResponse)
              }
            } catch { /* skip unparseable frames */ }
          }
        }

        ws.onerror = () => { connected.value = false; reject(new Error("WebSocket error")) }
        ws.onclose = () => { connected.value = false; stopHeartbeat() }
      } catch (e) { reject(e) }
    })
  }

  function subscribe(destination: string) {
    subId++
    sendFrame("SUBSCRIBE", { id: "sub-" + subId, destination })
    return subId
  }

  function unsubscribe(id: number) {
    sendFrame("UNSUBSCRIBE", { id: "sub-" + id })
  }

  function sendFrame(command: string, headers: Record<string, string>, body = "") {
    if (!ws || ws.readyState !== WebSocket.OPEN) return
    let frame = command + "\n"
    for (const [k, v] of Object.entries(headers)) frame += k + ":" + v + "\n"
    frame += "\n" + body + "\0"
    ws.send(frame)
  }

  function disconnect() {
    if (ws) { sendFrame("DISCONNECT", {}); ws.close(); ws = null }
    connected.value = false
    stopHeartbeat()
    alerts.value = []
  }

  function stopHeartbeat() {
    if (heartbeatTimer) { clearInterval(heartbeatTimer); heartbeatTimer = null }
  }

  return { connected, alerts, connect, disconnect, subscribe, unsubscribe }
}
