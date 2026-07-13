<template>
  <div class="worker-page">
    <div class="page-header">
      <h2>AI 养护咨询</h2>
      <p class="page-desc">咨询道路养护方案、病害处理建议</p>
    </div>

    <div class="chat-container">
      <div class="chat-messages" ref="msgContainer">
        <div v-for="(msg, i) in messages" :key="i" class="chat-msg" :class="msg.role">
          <div class="msg-avatar">
            <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="12" cy="12" r="10"/><path d="M12 16v-4"/><path d="M12 8h.01"/>
            </svg>
          </div>
          <div class="msg-bubble">{{ msg.content }}</div>
        </div>
      </div>
      <div class="chat-input-bar">
        <el-input v-model="inputText" placeholder="输入养护问题..." @keyup.enter="sendMessage" />
        <el-button type="primary" @click="sendMessage" :disabled="!inputText.trim()">发送</el-button>
      </div>
    </div>

    <div class="quick-questions">
      <span class="qq-label">快速提问：</span>
      <button v-for="q in quickQs" :key="q" class="qq-btn" @click="askQuick(q)">{{ q }}</button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick } from "vue"

const messages = ref([
  { role: "ai", content: "您好！我是道路养护 AI 助手，可以帮您分析病害、提供养护建议。请问有什么可以帮助您的？" },
])
const inputText = ref("")
const msgContainer = ref<HTMLElement | null>(null)
const quickQs = ["横向裂缝如何处理？", "坑槽修补标准是什么？", "路面老化养护建议"]

async function sendMessage() {
  const text = inputText.value.trim()
  if (!text) return
  messages.value.push({ role: "user", content: text })
  inputText.value = ""
  await nextTick()
  scrollToBottom()
  // Mock AI response
  setTimeout(() => {
    messages.value.push({ role: "ai", content: `收到您的问题「${text}」。建议：请先到现场核实病害情况，拍摄照片上传至系统。具体处理方案需要根据病害类型和严重程度确定，建议查阅《道路养护技术规范》。如需紧急处理，请立即派单。` })
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
.chat-input-bar { display: flex; gap: 8px; padding: 12px 16px; border-top: 1px solid #eef0f4; }
.quick-questions { margin-top: 16px; display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.qq-label { font-size: 12px; color: #94a3b8; }
.qq-btn { padding: 6px 14px; border: 1px solid #e2e8f0; background: #fff; border-radius: 20px; font-size: 12px; color: #4a5568; cursor: pointer; transition: all 0.15s; }
.qq-btn:hover { border-color: #4361ee; color: #4361ee; }
</style>
