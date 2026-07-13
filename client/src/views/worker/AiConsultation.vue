<template>
  <div class="worker-page">
    <div class="page-header">
      <h2>{{ t("ai.title") }}</h2>
      <p class="page-desc">{{ t("ai.desc") }}</p>
    </div>

    <div class="chat-container">
      <div class="chat-messages" ref="msgContainer">
        <div v-for="(msg, i) in messages" :key="i" class="chat-msg" :class="msg.role">
          <div class="msg-avatar">
            <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="12" cy="12" r="10"/><path d="M12 16v-4"/><path d="M12 8h.01"/>
            </svg>
          </div>
          <div class="msg-bubble" v-html="renderMarkdown(msg.content)"></div>
        </div>
      </div>
      <div class="chat-input-bar">
        <el-input v-model="inputText" :placeholder="t('ai.placeholder')" @keyup.enter="sendMessage" />
        <el-button type="primary" @click="sendMessage" :disabled="!inputText.trim()">{{ t("ai.send") }}</el-button>
      </div>
    </div>

    <div class="quick-questions">
      <span class="qq-label">{{ t("ai.quickQuestion") }}</span>
      <button v-for="q in quickQs" :key="q" class="qq-btn" @click="askQuick(q)">{{ q }}</button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick } from "vue"
import { t } from "@/i18n"
import { renderMarkdown } from "@/utils/markdown"

const messages = ref([
  { role: "ai", content: t("ai.greeting") },
])
const inputText = ref("")
const msgContainer = ref<HTMLElement | null>(null)
const quickQs = [t("ai.quickQ1"), t("ai.quickQ2"), t("ai.quickQ3")]

async function sendMessage() {
  const text = inputText.value.trim()
  if (!text) return
  messages.value.push({ role: "user", content: text })
  inputText.value = ""
  await nextTick()
  scrollToBottom()
  // Mock AI response
  setTimeout(() => {
    messages.value.push({ role: "ai", content: t("ai.mockResponse", { q: text }) })
    nextTick(() => scrollToBottom())
  }, 500)
}

function askQuick(q: string) {
  inputText.value = q
  sendMessage()
}

function scrollToBottom() {
  if (msgContainer.value) {
    msgContainer.value.scrollTop = msgContainer.value.scrollHeight
  }
}
</script>

<style scoped>
.worker-page { max-width: 800px; margin: 0 auto; }
.page-header { margin-bottom: 20px; }
.page-header h2 { font-size: 18px; font-weight: 700; color: #1a202c; margin: 0 0 6px 0; }
.page-desc { font-size: 13px; color: #64748b; margin: 0; }
.chat-container { background: #fff; border: 1px solid #eef0f4; border-radius: 12px; overflow: hidden; }
.chat-messages { height: 360px; overflow-y: auto; padding: 16px; display: flex; flex-direction: column; gap: 12px; }
.chat-msg { display: flex; gap: 8px; max-width: 85%; }
.chat-msg.user { flex-direction: row-reverse; align-self: flex-end; }
.msg-avatar { width: 28px; height: 28px; border-radius: 50%; background: #eef2ff; display: flex; align-items: center; justify-content: center; color: #4361ee; flex-shrink: 0; }
.msg-bubble { padding: 10px 14px; border-radius: 12px; font-size: 13px; line-height: 1.5; }
.chat-msg.ai .msg-bubble { background: #f1f5f9; color: #1a202c; border-bottom-left-radius: 4px; }
.chat-msg.user .msg-bubble { background: #4361ee; color: #fff; border-bottom-right-radius: 4px; }
.msg-bubble ::v-deep strong { font-weight:600; color:#0f172a; }
.chat-msg.user .msg-bubble ::v-deep strong { color:#fff; }
.msg-bubble ::v-deep em { font-style:italic; color:#475569; }
.chat-msg.user .msg-bubble ::v-deep em { color:#dbeafe; }
.msg-bubble ::v-deep code { font-family:ui-monospace,SFMono-Regular,Menlo,Monaco,Consolas,monospace; font-size:12px; background:#e2e8f0; color:#334155; padding:2px 5px; border-radius:4px; }
.chat-msg.user .msg-bubble ::v-deep code { background:rgba(255,255,255,0.2); color:#fff; }
.msg-bubble ::v-deep pre { background:#0f172a; color:#e2e8f0; padding:10px 12px; border-radius:8px; overflow-x:auto; margin:6px 0; }
.msg-bubble ::v-deep pre code { background:transparent; color:inherit; padding:0; }
.msg-bubble ::v-deep blockquote { margin:6px 0; padding:6px 12px; border-left:3px solid #94a3b8; background:#f8fafc; color:#475569; border-radius:0 4px 4px 0; }
.chat-msg.user .msg-bubble ::v-deep blockquote { background:rgba(255,255,255,0.1); border-left-color:rgba(255,255,255,0.4); color:#fff; }
.msg-bubble ::v-deep ul, .msg-bubble ::v-deep ol { margin:6px 0; padding-left:18px; }
.msg-bubble ::v-deep li { margin-bottom:3px; }
.msg-bubble ::v-deep a { color:#4361ee; text-decoration:underline; }
.chat-msg.user .msg-bubble ::v-deep a { color:#bfdbfe; }
.chat-input-bar { display: flex; gap: 8px; padding: 12px 16px; border-top: 1px solid #eef0f4; }
.quick-questions { margin-top: 16px; display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.qq-label { font-size: 12px; color: #94a3b8; }
.qq-btn { padding: 6px 14px; border: 1px solid #e2e8f0; background: #fff; border-radius: 20px; font-size: 12px; color: #4a5568; cursor: pointer; transition: all 0.15s; }
.qq-btn:hover { border-color: #4361ee; color: #4361ee; }
</style>
