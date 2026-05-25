<script setup lang="ts">
import DOMPurify from 'dompurify'
import { marked } from 'marked'
import { computed, reactive, ref } from 'vue'
import {
  Bot,
  Clock3,
  Copy,
  Database,
  Download,
  FileDown,
  Loader2,
  MapPinned,
  MessageSquareText,
  RefreshCw,
  Send,
  Sparkles,
  Trash2,
  Users,
  WalletCards,
  Waves,
} from 'lucide-vue-next'
import { continueChat, exportPlan, generatePlan, ragChat, streamPlan } from './api'
import type { ChatMessage, RequestMode, TravelPlanRequest, TravelPlanResponse } from './types'

const form = reactive<TravelPlanRequest>({
  city: '',
  startLocation: '',
  destinationArea: '',
  dateTime: '',
  budget: '',
  people: '',
  preference: '',
  message: '',
})

const conversationId = ref('')
const planContent = ref('')
const chatInput = ref('')
const downloadUrl = ref('')
const errorMessage = ref('')
const lastDuration = ref<number | null>(null)
const loadingMode = ref<RequestMode>('idle')
const messages = ref<ChatMessage[]>([])

const isBusy = computed(() => loadingMode.value !== 'idle')
const hasConversation = computed(() => Boolean(conversationId.value))
const canChat = computed(() => hasConversation.value && chatInput.value.trim().length > 0 && !isBusy.value)
const statusText = computed(() => {
  const map: Record<RequestMode, string> = {
    idle: '就绪',
    generating: '正在生成规划',
    streaming: '正在流式生成',
    chatting: '正在续聊',
    rag: '正在 RAG 问答',
    exporting: '正在导出 Markdown',
  }
  return map[loadingMode.value]
})

marked.use({
  gfm: true,
  breaks: true,
})

function renderMarkdown(content: string) {
  if (!content.trim()) return ''
  return DOMPurify.sanitize(marked.parse(content, { async: false }))
}

function nowTime() {
  return new Date().toLocaleTimeString('zh-CN', {
    hour: '2-digit',
    minute: '2-digit',
  })
}

function buildRequest(withConversationId: boolean): TravelPlanRequest {
  const request: TravelPlanRequest = {
    city: form.city?.trim(),
    startLocation: form.startLocation?.trim(),
    destinationArea: form.destinationArea?.trim(),
    dateTime: form.dateTime?.trim(),
    budget: form.budget?.trim(),
    people: form.people?.trim(),
    preference: form.preference?.trim(),
    message: form.message?.trim(),
  }

  if (withConversationId && conversationId.value) {
    request.conversationId = conversationId.value
  }

  return request
}

function addMessage(role: ChatMessage['role'], content: string) {
  if (!content.trim()) return
  messages.value.push({
    role,
    content: content.trim(),
    time: nowTime(),
  })
}

function applyPlanResponse(response: TravelPlanResponse) {
  conversationId.value = response.conversationId || conversationId.value
  planContent.value = response.content || ''
  addMessage('assistant', response.content || '')
}

function resolveDownloadUrl(filePath: string) {
  if (!filePath) return ''
  if (filePath.startsWith('http://') || filePath.startsWith('https://') || filePath.startsWith('/api/')) {
    return filePath
  }
  return `/api/travel/plan/download/${encodeURIComponent(filePath)}`
}

async function runAction<T>(mode: RequestMode, action: () => Promise<T>) {
  const startedAt = performance.now()
  loadingMode.value = mode
  errorMessage.value = ''

  try {
    return await action()
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '请求失败，请检查后端服务'
    throw error
  } finally {
    lastDuration.value = Math.round(performance.now() - startedAt)
    loadingMode.value = 'idle'
  }
}

function validatePlanInput() {
  if (!form.city?.trim() && !form.message?.trim()) {
    errorMessage.value = '请至少填写目标城市或补充需求'
    return false
  }
  return true
}

async function handleGenerate() {
  if (!validatePlanInput()) return

  const userRequest = form.message || `生成 ${form.city || '目的地'} 的旅行规划`
  addMessage('user', userRequest)
  downloadUrl.value = ''

  await runAction('generating', async () => {
    const response = await generatePlan(buildRequest(false))
    applyPlanResponse(response)
  }).catch(() => undefined)
}

async function handleStream() {
  if (!validatePlanInput()) return

  const userRequest = form.message || `流式生成 ${form.city || '目的地'} 的旅行规划`
  addMessage('user', userRequest)
  planContent.value = ''
  downloadUrl.value = ''

  await runAction('streaming', async () => {
    await streamPlan(
      buildRequest(false),
      (token) => {
        planContent.value += token
      },
      (id) => {
        conversationId.value = id
      },
    )
    addMessage('assistant', planContent.value)
  }).catch(() => undefined)
}

async function handleChat() {
  if (!canChat.value) return

  const message = chatInput.value.trim()
  chatInput.value = ''
  addMessage('user', message)

  await runAction('chatting', async () => {
    const response = await continueChat({
      conversationId: conversationId.value,
      message,
    })
    applyPlanResponse(response)
  }).catch(() => undefined)
}

async function handleRag() {
  if (!canChat.value) return

  const message = chatInput.value.trim()
  chatInput.value = ''
  addMessage('user', `[RAG] ${message}`)

  await runAction('rag', async () => {
    const response = await ragChat({
      conversationId: conversationId.value,
      message,
    })
    applyPlanResponse(response)
  }).catch(() => undefined)
}

async function handleExport() {
  if (!validatePlanInput()) return

  await runAction('exporting', async () => {
    const response = await exportPlan(buildRequest(true))
    conversationId.value = response.conversationId || conversationId.value
    planContent.value = response.content || planContent.value
    downloadUrl.value = resolveDownloadUrl(response.filePath)
    addMessage('system', `Markdown 已生成：${response.filePath}`)
  }).catch(() => undefined)
}

function fillDemo() {
  form.city = '杭州'
  form.startLocation = '上海虹桥'
  form.destinationArea = '西湖、灵隐寺、法喜寺'
  form.dateTime = '本周六到周日'
  form.budget = '1500 元'
  form.people = '2 人'
  form.preference = '轻松 citywalk、拍照、美食、不要太赶'
  form.message = '请给出两天一晚路线，包含交通、餐饮和时间安排'
}

function clearSession() {
  conversationId.value = ''
  planContent.value = ''
  chatInput.value = ''
  downloadUrl.value = ''
  errorMessage.value = ''
  lastDuration.value = null
  messages.value = []
}

async function copyConversationId() {
  if (!conversationId.value) return
  await navigator.clipboard?.writeText(conversationId.value)
}
</script>

<template>
  <div class="app-shell">
    <header class="topbar">
      <div>
        <p class="eyebrow">AI Travel Planner</p>
        <h1>旅游规划助手工作台</h1>
      </div>
      <div class="topbar-status" :class="{ active: isBusy }">
        <Loader2 v-if="isBusy" class="spin" :size="17" />
        <Sparkles v-else :size="17" />
        <span>{{ statusText }}</span>
      </div>
    </header>

    <main class="workspace">
      <section class="panel form-panel">
        <div class="panel-heading">
          <div>
            <p class="eyebrow">Trip Brief</p>
            <h2>旅行需求</h2>
          </div>
          <button class="ghost-icon" type="button" title="填入演示数据" @click="fillDemo">
            <RefreshCw :size="17" />
          </button>
        </div>

        <div class="field-grid">
          <label>
            <span>目标城市</span>
            <input v-model="form.city" placeholder="例如：杭州" />
          </label>
          <label>
            <span>出发地点</span>
            <input v-model="form.startLocation" placeholder="例如：上海虹桥" />
          </label>
          <label>
            <span>目标区域</span>
            <input v-model="form.destinationArea" placeholder="例如：西湖 / 灵隐寺" />
          </label>
          <label>
            <span>日期时间</span>
            <input v-model="form.dateTime" placeholder="例如：本周六到周日" />
          </label>
          <label>
            <span>预算</span>
            <input v-model="form.budget" placeholder="例如：1500 元" />
          </label>
          <label>
            <span>出行人数</span>
            <input v-model="form.people" placeholder="例如：2 人" />
          </label>
        </div>

        <label>
          <span>偏好要求</span>
          <textarea v-model="form.preference" rows="3" placeholder="例如：美食、拍照、轻松、不绕路" />
        </label>
        <label>
          <span>补充需求</span>
          <textarea v-model="form.message" rows="5" placeholder="告诉 AI 更完整的规划目标" />
        </label>

        <div class="button-row">
          <button class="primary" type="button" :disabled="isBusy" @click="handleGenerate">
            <Sparkles :size="17" />
            生成规划
          </button>
          <button class="secondary" type="button" :disabled="isBusy" @click="handleStream">
            <Waves :size="17" />
            流式生成
          </button>
        </div>
      </section>

      <section class="panel output-panel">
        <div class="panel-heading">
          <div>
            <p class="eyebrow">AI Output</p>
            <h2>规划与对话</h2>
          </div>
          <div class="compact-meta">
            <Clock3 :size="15" />
            <span>{{ lastDuration === null ? '未请求' : `${lastDuration} ms` }}</span>
          </div>
        </div>

        <div class="result-area">
          <div v-if="planContent" class="content-render markdown-body" v-html="renderMarkdown(planContent)" />
          <div v-else class="empty-state">
            <Bot :size="34" />
            <p>填写左侧需求后生成规划，后续对话会自动沿用同一个 conversationId。</p>
          </div>
        </div>

        <div v-if="errorMessage" class="error-banner">
          {{ errorMessage }}
        </div>

        <div class="chat-panel">
          <div class="chat-list">
            <article v-for="(message, index) in messages" :key="index" class="message" :class="message.role">
              <div class="message-head">
                <span>{{ message.role === 'user' ? '你' : message.role === 'assistant' ? 'AI' : '系统' }}</span>
                <time>{{ message.time }}</time>
              </div>
              <p v-if="message.role === 'user'" class="message-text">{{ message.content }}</p>
              <div v-else class="message-render markdown-body" v-html="renderMarkdown(message.content)" />
            </article>
          </div>

          <div class="composer">
            <textarea
              v-model="chatInput"
              :disabled="!hasConversation || isBusy"
              rows="3"
              placeholder="输入补充问题，前端会自动携带当前 conversationId"
              @keydown.ctrl.enter.prevent="handleChat"
            />
            <div class="button-row align-right">
              <button class="secondary" type="button" :disabled="!canChat" @click="handleRag">
                <Database :size="17" />
                RAG 问答
              </button>
              <button class="primary" type="button" :disabled="!canChat" @click="handleChat">
                <Send :size="17" />
                继续对话
              </button>
            </div>
          </div>
        </div>
      </section>

      <aside class="panel side-panel">
        <div class="panel-heading">
          <div>
            <p class="eyebrow">Session</p>
            <h2>会话状态</h2>
          </div>
        </div>

        <div class="stat-list">
          <div class="stat-item">
            <MapPinned :size="18" />
            <div>
              <span>当前城市</span>
              <strong>{{ form.city || '未填写' }}</strong>
            </div>
          </div>
          <div class="stat-item">
            <Users :size="18" />
            <div>
              <span>出行人数</span>
              <strong>{{ form.people || '未填写' }}</strong>
            </div>
          </div>
          <div class="stat-item">
            <WalletCards :size="18" />
            <div>
              <span>预算</span>
              <strong>{{ form.budget || '未填写' }}</strong>
            </div>
          </div>
        </div>

        <div class="session-box">
          <span>Conversation ID</span>
          <code>{{ conversationId || '等待后端生成' }}</code>
          <button class="secondary full" type="button" :disabled="!conversationId" @click="copyConversationId">
            <Copy :size="16" />
            复制 ID
          </button>
        </div>

        <div class="side-actions">
          <button class="primary full" type="button" :disabled="isBusy" @click="handleExport">
            <FileDown :size="17" />
            导出 Markdown
          </button>
          <a v-if="downloadUrl" class="download-link" :href="downloadUrl" target="_blank" rel="noreferrer">
            <Download :size="17" />
            下载规划文件
          </a>
          <button class="danger full" type="button" :disabled="isBusy" @click="clearSession">
            <Trash2 :size="17" />
            清空会话
          </button>
        </div>

        <div class="note">
          <MessageSquareText :size="17" />
          <p>首次生成不传 ID；续聊、RAG 与导出会沿用后端返回的 ID。</p>
        </div>
      </aside>
    </main>
  </div>
</template>
