<template>
  <div class="agent-page">
    <div class="page-head">
      <div>
        <h2 class="page-title">{{ t('agent.title') }}</h2>
        <p class="page-desc">{{ t('agent.desc') }}</p>
      </div>
    </div>
    <div class="chat-card">
      <div class="chat-msgs" ref="msgRef">
        <div v-for="(m,i) in messages" :key="i" :class="['msg-row', m.role === 'user' ? 'user' : 'ai']">
          <div v-if="m.role === 'ai'" class="msg-avatar">
            <div class="avatar-wrap"><img class="avatar-img" src="/avatar-agent.png" alt="AI" /></div>
          </div>
          <div class="msg-content">
            <div class="msg-bubble" v-html="renderMarkdown(m.content)"></div>
            <div v-if="m.role === 'ai' && m.showGenerate" class="msg-actions">
              <button class="gen-report-btn" @click="generateReport(m)">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/><polyline points="10 9 9 9 8 9"/></svg>
                {{ t('agent.generateReport') }}
              </button>
            </div>
            <div class="msg-time">{{ m.time }}</div>
          </div>
        </div>
        <div v-if="typing" class="msg-row ai">
          <div class="msg-avatar">
            <div class="avatar-wrap"><img class="avatar-img" src="/avatar-agent.png" alt="AI" /></div>
          </div>
          <div class="typing-dots"><span></span><span></span><span></span></div>
        </div>
      </div>
      <div class="chat-input-bar">
        <input v-model="input" :placeholder="t('agent.placeholder')" :disabled="typing" @keyup.enter="send" />
        <button class="send-btn" :disabled="!input.trim() || typing" @click="send">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="22" y1="2" x2="11" y2="13"/><polygon points="22 2 15 22 11 13 2 9 22 2"/></svg>
        </button>
      </div>
    </div>

    <!-- Report Generated Modal -->
    <div v-if="showReport" class="modal-overlay" @click.self="showReport=false">
      <div class="modal-card">
        <div class="modal-head">
          <span>📄 {{ t('agent.reportTitle') }}</span>
          <button class="modal-close" @click="showReport=false">✕</button>
        </div>
        <div class="modal-body">
          <div class="report-preview" v-html="reportContent"></div>
        </div>
        <div class="modal-foot">
          <button class="btn-ghost" @click="showReport=false">{{ t('common.close') }}</button>
          <button class="btn-primary" @click="downloadReport">{{ t('agent.downloadReport') }}</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick } from "vue"
import { agentApi } from "@/api/agent"
import { ElMessage } from "element-plus"
import { t } from "@/i18n"
import { renderMarkdown } from "@/utils/markdown"

const msgRef = ref()
const input = ref("")
const typing = ref(false)
const showReport = ref(false)
const reportContent = ref("")

type ChatMessage = {
  role: "user" | "ai"
  content: string
  time: string
  showGenerate?: boolean
  dataSource?: string
}

function getTime() {
  const n = new Date()
  return String(n.getHours()).padStart(2,"0") + ":" + String(n.getMinutes()).padStart(2,"0")
}

const messages = ref<ChatMessage[]>([
  { role: "ai", content: t('agent.welcome'), time: getTime(), showGenerate: false }
])

function scrollBottom() {
  nextTick(() => { if (msgRef.value) msgRef.value.scrollTop = msgRef.value.scrollHeight })
}

const replies: Record<string, string> = {
  "今日病害统计": "📊 <strong>今日检测概览</strong><br>检测里程：<strong>127 km</strong><br>发现裂缝：<strong>43 处</strong><br>严重：12 处 | 中等：21 处 | 轻微：10 处<br>已派工单：8 个",
  "生成检测周报": "📄 <strong>本周检测周报</strong><br>检测总里程：<strong>847 km</strong><br>裂缝总数：<strong>381 处</strong><br>修复完成：236 处<br>待修复：145 处<br>建议：重点关注 G102 国道 K15+300 处纵向裂缝",
  "裂缝趋势预测": "📈 <strong>趋势预测</strong><br>G102 国道：<strong>上升 12%</strong>（需重点关注）<br>G101 国道：<strong>下降 5%</strong>（趋势良好）<br>G105 国道：<strong>上升 3%</strong>（持续观察）",
  "高优先级工单": "🚨 <strong>高优先级工单（8 个）</strong><br>1. G102 K15+300 纵向裂缝（严重）<br>2. G101 K12+500 横向裂缝（严重）<br>3. G103 K17+200 网状裂缝（严重）<br>4. G105 K22+100 路面抛洒（中等）"
}

async function send() {
  const text = input.value.trim()
  if (!text || typing.value) return
  messages.value.push({ role: "user", content: text, time: getTime(), showGenerate: false })
  input.value = ""
  scrollBottom()
  typing.value = true
  try {
    const res = await agentApi.chat({ message: text })
    const data = res.data.data
    messages.value.push({ role: "ai", content: data.answer || "暂无回复", time: getTime(), dataSource: data.dataSource || "ai" })
  } catch {
    messages.value.push({ role: "ai", content: t('agent.unavailable'), time: getTime(), dataSource: "error" })
  }
  typing.value = false
  scrollBottom()
}

function generateReport(msg: ChatMessage) {
  // Optional: generate analysis report from the AI response
  const now = new Date()
  const dateStr = now.getFullYear() + "-" + String(now.getMonth()+1).padStart(2,"0") + "-" + String(now.getDate()).padStart(2,"0")
  reportContent.value = `
    <h3 style="margin-top:0">${t('agent.reportPageTitle')}</h3>
    <p style="color:#64748b;font-size:13px">${t('agent.reportDateLabel')}：${dateStr} | ${t('agent.reportDataSource')}</p>
    <hr style="border:none;border-top:1px solid #eef0f4;margin:12px 0">
    <div style="font-size:13px;line-height:1.8">
      ${renderMarkdown(msg.content)}
      <br><br>
      <strong>${t('agent.reportConclusion')}</strong><br>
      ${t('agent.reportAdvice')}
    </div>
  `
  showReport.value = true
}

function downloadReport() {
  const blob = new Blob([reportContent.value], { type: "text/html" })
  const url = URL.createObjectURL(blob)
  const a = document.createElement("a")
  a.href = url
  a.download = "road_health_report_" + Date.now() + ".html"
  a.click()
  URL.revokeObjectURL(url)
  ElMessage.success(t('agent.reportDownloaded'))
}
</script>

<style scoped>
.agent-page { padding:0; font-family:Inter,-apple-system,BlinkMacSystemFont,sans-serif; }
.page-head { display:flex; align-items:flex-end; justify-content:space-between; margin-bottom:20px; }
.page-title { font-size:20px; font-weight:700; color:#0f172a; margin:0 0 4px; }
.page-desc { font-size:13px; color:#94a3b8; margin:0; }
.chat-card { border:1px solid #f0f2f5; border-radius:12px; background:#fff; height:calc(100vh - 170px); display:flex; flex-direction:column; overflow:hidden; }
.chat-msgs { flex:1; overflow-y:auto; padding:20px; display:flex; flex-direction:column; gap:16px; }
.chat-msgs::-webkit-scrollbar { width:4px; }
.chat-msgs::-webkit-scrollbar-thumb { background:#e2e8f0; border-radius:2px; }
.msg-row { display:flex; gap:10px; align-items:flex-start; max-width:100%; }
.msg-row.user { flex-direction:row-reverse; }
.msg-avatar { flex-shrink:0; }
.avatar-wrap { width:36px; height:36px; border-radius:50%; overflow:hidden; border:2px solid rgba(183,36,255,.3); box-shadow:0 0 8px rgba(183,36,255,.15); }
.avatar-img { width:100%; height:100%; object-fit:cover; display:block; }
.msg-content { max-width:70%; }
.msg-bubble { padding:10px 14px; border-radius:10px; font-size:13px; line-height:1.6; word-break:break-word; }
.msg-row.ai .msg-bubble { background:#f1f5f9; color:#1a202c; border-bottom-left-radius:4px; }
.msg-row.user .msg-bubble { background:#2563eb; color:#fff; border-bottom-right-radius:4px; }
.msg-bubble ::v-deep h1, .msg-bubble ::v-deep h2, .msg-bubble ::v-deep h3, .msg-bubble ::v-deep h4, .msg-bubble ::v-deep h5, .msg-bubble ::v-deep h6, .report-preview ::v-deep h1, .report-preview ::v-deep h2, .report-preview ::v-deep h3, .report-preview ::v-deep h4, .report-preview ::v-deep h5, .report-preview ::v-deep h6 { margin:10px 0 6px; line-height:1.4; color:#0f172a; }
.msg-bubble ::v-deep h1, .report-preview ::v-deep h1 { font-size:18px; }
.msg-bubble ::v-deep h2, .report-preview ::v-deep h2 { font-size:16px; }
.msg-bubble ::v-deep h3, .report-preview ::v-deep h3 { font-size:14px; }
.msg-bubble ::v-deep h4, .report-preview ::v-deep h4 { font-size:13px; }
.msg-bubble ::v-deep h5, .report-preview ::v-deep h5 { font-size:12px; }
.msg-bubble ::v-deep h6, .report-preview ::v-deep h6 { font-size:12px; }
.msg-row.user .msg-bubble ::v-deep h1, .msg-row.user .msg-bubble ::v-deep h2, .msg-row.user .msg-bubble ::v-deep h3, .msg-row.user .msg-bubble ::v-deep h4, .msg-row.user .msg-bubble ::v-deep h5, .msg-row.user .msg-bubble ::v-deep h6 { color:#fff; }
.msg-bubble ::v-deep strong { font-weight:600; color:#0f172a; }
.msg-row.user .msg-bubble ::v-deep strong { color:#fff; }
.msg-bubble ::v-deep em { font-style:italic; color:#475569; }
.msg-row.user .msg-bubble ::v-deep em { color:#dbeafe; }
.msg-bubble ::v-deep code { font-family:ui-monospace,SFMono-Regular,Menlo,Monaco,Consolas,monospace; font-size:12px; background:#e2e8f0; color:#334155; padding:2px 5px; border-radius:4px; }
.msg-row.user .msg-bubble ::v-deep code { background:rgba(255,255,255,0.2); color:#fff; }
.msg-bubble ::v-deep pre { background:#0f172a; color:#e2e8f0; padding:10px 12px; border-radius:8px; overflow-x:auto; margin:6px 0; }
.msg-bubble ::v-deep pre code { background:transparent; color:inherit; padding:0; }
.msg-bubble ::v-deep blockquote { margin:6px 0; padding:6px 12px; border-left:3px solid #94a3b8; background:#f8fafc; color:#475569; border-radius:0 4px 4px 0; }
.msg-row.user .msg-bubble ::v-deep blockquote { background:rgba(255,255,255,0.1); border-left-color:rgba(255,255,255,0.4); color:#fff; }
.msg-bubble ::v-deep ul, .msg-bubble ::v-deep ol { margin:6px 0; padding-left:18px; }
.msg-bubble ::v-deep li { margin-bottom:3px; }
.msg-bubble ::v-deep a { color:#2563eb; text-decoration:underline; }
.msg-row.user .msg-bubble ::v-deep a { color:#bfdbfe; }
.msg-actions { margin-top:6px; display:flex; gap:6px; }
.gen-report-btn { display:inline-flex; align-items:center; gap:4px; padding:4px 10px; border:1px solid #dbeafe; border-radius:6px; background:#eff6ff; color:#2563eb; font-size:11px; font-weight:500; cursor:pointer; font-family:inherit; transition:all .15s; }
.gen-report-btn:hover { background:#dbeafe; border-color:#93c5fd; }
.msg-time { font-size:10px; color:#94a3b8; margin-top:4px; }
.typing-dots { display:flex; gap:4px; padding:12px 16px; background:#f1f5f9; border-radius:10px; }
.typing-dots span { width:7px; height:7px; border-radius:50%; background:#94a3b8; animation:typing 1.4s infinite; }
.typing-dots span:nth-child(2) { animation-delay:.2s; }
.typing-dots span:nth-child(3) { animation-delay:.4s; }
@keyframes typing { 0%,60%,100%{opacity:.3;transform:scale(.8)} 30%{opacity:1;transform:scale(1)} }
.chat-input-bar { display:flex; gap:8px; padding:12px 16px; border-top:1px solid #f0f2f5; background:#fff; }
.chat-input-bar input { flex:1; padding:8px 12px; border:1px solid #e2e8f0; border-radius:8px; font-size:13px; outline:none; font-family:inherit; transition:border-color .15s; }
.chat-input-bar input:focus { border-color:#2563eb; }
.chat-input-bar input::placeholder { color:#94a3b8; }
.send-btn { width:36px; height:36px; display:flex; align-items:center; justify-content:center; border:none; border-radius:8px; background:#2563eb; color:#fff; cursor:pointer; flex-shrink:0; transition:background .15s; }
.send-btn:hover { background:#1d4ed8; }
.send-btn:disabled { background:#93c5fd; cursor:not-allowed; }

.modal-overlay { position:fixed; inset:0; background:rgba(0,0,0,0.3); z-index:1000; display:flex; align-items:center; justify-content:center; }
.modal-card { background:#fff; border-radius:12px; width:600px; max-height:80vh; overflow-y:auto; box-shadow:0 4px 24px rgba(0,0,0,0.1); }
.modal-head { display:flex; align-items:center; justify-content:space-between; padding:16px 20px; border-bottom:1px solid #f0f2f5; font-size:15px; font-weight:600; color:#0f172a; }
.modal-close { width:26px; height:26px; display:flex; align-items:center; justify-content:center; background:#f1f5f9; border:none; border-radius:6px; color:#94a3b8; cursor:pointer; font-size:12px; }
.modal-body { padding:20px; }
.report-preview { font-size:13px; line-height:1.8; color:#334155; }
.report-preview ::v-deep ul, .report-preview ::v-deep ol { margin:8px 0; padding-left:18px; }
.report-preview ::v-deep li { margin-bottom:4px; }
.modal-foot { display:flex; justify-content:flex-end; gap:8px; padding:14px 20px; border-top:1px solid #f0f2f5; }
.btn-ghost { padding:7px 16px; border:1px solid #e2e8f0; border-radius:6px; background:#fff; font-size:12px; color:#475569; cursor:pointer; font-family:inherit; }
.btn-ghost:hover { border-color:#2563eb; color:#2563eb; }
.btn-primary { padding:7px 16px; border:none; border-radius:6px; background:#2563eb; color:#fff; font-size:12px; font-weight:500; cursor:pointer; }
.btn-primary:hover { background:#1d4ed8; }
</style>
