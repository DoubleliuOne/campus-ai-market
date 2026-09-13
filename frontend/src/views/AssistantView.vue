<script setup>
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import ElMessage from 'element-plus/es/components/message/index.mjs'
import ElMessageBox from 'element-plus/es/components/message-box/index.mjs'
import {
  BookOpenCheck,
  Bot,
  CirclePlus,
  Clock3,
  MessageSquareText,
  PackageSearch,
  ReceiptText,
  Search,
  SendHorizontal,
  Sparkles,
  Trash2,
} from 'lucide-vue-next'
import { marked } from 'marked'
import DOMPurify from 'dompurify'

import {
  createConversationApi,
  deleteConversationApi,
  getConversationMessagesApi,
  getConversationsApi,
  sendConversationMessageApi,
} from '../api'
import EmptyState from '../components/EmptyState.vue'
import { formatDateTime } from '../utils/format'

const route = useRoute()
const router = useRouter()

const conversations = ref([])
const activeConversationId = ref(null)
const messages = ref([])
const inputText = ref('')
const contextItemId = ref(toItemId(route.query.itemId))
const loadingConversations = ref(false)
const loadingMessages = ref(false)
const sending = ref(false)
const conversationError = ref('')
const chatBody = ref()
let messageRequestId = 0

const suggestions = [
  '帮我找 200 元以内的机械键盘',
  '推荐适合宿舍的闲置好物',
  '平台不能发布什么商品？',
  '看看我最近的订单情况',
]

const agentCapabilities = [
  {
    icon: Search,
    title: '真实商品检索',
    description: '按关键词、分类和预算查询在售商品',
  },
  {
    icon: PackageSearch,
    title: '商品详情理解',
    description: '结合当前商品信息回答配置、成色与适配问题',
  },
  {
    icon: BookOpenCheck,
    title: '平台规则问答',
    description: '基于校园交易规则知识库回答发布与售后问题',
  },
  {
    icon: ReceiptText,
    title: '个人订单查询',
    description: '读取当前账号的买家或卖家订单数据',
  },
]

const activeConversation = computed(() =>
  conversations.value.find((conversation) => conversation.id === activeConversationId.value),
)

function toItemId(value) {
  const id = Number(value)
  return Number.isInteger(id) && id > 0 ? id : null
}

function conversationRole(role) {
  return String(role || '').toLowerCase() === 'user' ? 'user' : 'assistant'
}

function renderAssistantMessage(content) {
  const html = marked.parse(content || '', {
    async: false,
    gfm: true,
    breaks: true,
  })
  return DOMPurify.sanitize(typeof html === 'string' ? html : '')
}

async function scrollToBottom() {
  await nextTick()
  if (chatBody.value) {
    chatBody.value.scrollTop = chatBody.value.scrollHeight
  }
}

async function loadConversations() {
  loadingConversations.value = true
  conversationError.value = ''
  try {
    const result = await getConversationsApi({ page: 1, size: 50 })
    conversations.value = result.records || []
  } catch (error) {
    conversationError.value = error.message || '会话加载失败'
  } finally {
    loadingConversations.value = false
  }
}

async function selectConversation(conversation) {
  if (!conversation) {
    return
  }
  const requestId = ++messageRequestId
  activeConversationId.value = conversation.id
  loadingMessages.value = true
  messages.value = []
  try {
    const result = await getConversationMessagesApi(conversation.id)
    if (requestId !== messageRequestId
        || activeConversationId.value !== conversation.id) {
      return
    }
    messages.value = (result || []).map((message) => ({
      id: message.id,
      role: conversationRole(message.role),
      content: message.content,
      createTime: message.createTime,
    }))
    scrollToBottom()
  } catch (error) {
    if (error.status !== 401) {
      ElMessage.error(error.message || '消息加载失败')
    }
  } finally {
    if (requestId === messageRequestId) {
      loadingMessages.value = false
    }
  }
}

async function newConversation() {
  try {
    const conversation = await createConversationApi({})
    messageRequestId += 1
    loadingMessages.value = false
    conversations.value.unshift(conversation)
    activeConversationId.value = conversation.id
    messages.value = []
    inputText.value = ''
    return conversation
  } catch (error) {
    if (error.status !== 401) {
      ElMessage.error(error.message || '新建会话失败')
    }
    return null
  }
}

async function removeConversation(conversation) {
  try {
    await ElMessageBox.confirm(`确认删除“${conversation.title}”吗？`, '删除会话', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning',
    })
  } catch {
    return
  }

  try {
    await deleteConversationApi(conversation.id)
    if (activeConversationId.value === conversation.id) {
      messageRequestId += 1
      loadingMessages.value = false
    }
    conversations.value = conversations.value.filter((item) => item.id !== conversation.id)
    if (activeConversationId.value === conversation.id) {
      activeConversationId.value = null
      messages.value = []
    }
    ElMessage.success('会话已删除')
  } catch (error) {
    if (error.status !== 401) {
      ElMessage.error(error.message || '删除失败')
    }
  }
}

async function sendMessage(content = inputText.value) {
  const text = String(content || '').trim()
  if (!text || sending.value || loadingMessages.value) {
    return
  }

  let conversationId = activeConversationId.value
  if (!conversationId) {
    const conversation = await newConversation()
    if (!conversation) {
      return
    }
    conversationId = conversation.id
  }

  inputText.value = ''
  const viewVersion = messageRequestId
  const sentItemId = contextItemId.value
  const optimisticMessage = {
    role: 'user',
    content: text,
    createTime: new Date().toISOString(),
  }
  messages.value.push(optimisticMessage)
  scrollToBottom()
  sending.value = true

  try {
    const result = await sendConversationMessageApi(conversationId, {
      message: text,
      itemId: sentItemId,
    })

    const current = conversations.value.find((item) => item.id === conversationId)
    if (current) {
      if (!current.title || current.title === '新对话') {
        current.title = text.slice(0, 30)
      }
      current.lastMessage = result.reply || text
      current.messageCount = (current.messageCount || 0) + 2
      current.updateTime = new Date().toISOString()
    }

    if (viewVersion === messageRequestId
        && activeConversationId.value === conversationId) {
      messages.value.push({
        role: 'assistant',
        content: result.reply || '暂时没有获得回答，请再试一次。',
        createTime: new Date().toISOString(),
      })
    }
  } catch (error) {
    messages.value = messages.value.filter((message) => message !== optimisticMessage)
    if (error.status !== 401) {
      ElMessage.error(error.message || 'AI 服务暂时不可用')
    }
  } finally {
    sending.value = false
    scrollToBottom()
  }
}

function clearItemContext() {
  contextItemId.value = null
  router.replace({ query: {} })
}

onMounted(async () => {
  await loadConversations()
  if (route.query.prompt) {
    inputText.value = String(route.query.prompt)
  }
  if (contextItemId.value) {
    activeConversationId.value = null
    messages.value = []
    return
  }
  if (conversations.value.length) {
    await selectConversation(conversations.value[0])
  }
})

watch(
  () => route.query.itemId,
  (value) => {
    const next = toItemId(value)
    if (next === contextItemId.value) {
      return
    }
    contextItemId.value = next
    messageRequestId += 1
    loadingMessages.value = false
    activeConversationId.value = null
    messages.value = []
  },
)

watch(
  () => route.query.prompt,
  (value) => {
    if (value) {
      inputText.value = String(value)
    }
  },
)
</script>

<template>
  <div class="page-shell assistant-page">
    <section class="assistant-workspace">
      <aside class="conversation-sidebar">
        <div class="sidebar-head">
          <div>
            <span class="sidebar-kicker">CampusAI Agent</span>
            <h1>AI 助手</h1>
          </div>
          <el-button
            type="primary"
            circle
            :icon="CirclePlus"
            :disabled="sending"
            aria-label="新建会话"
            @click="newConversation"
          />
        </div>

        <div v-loading="loadingConversations" class="conversation-list">
          <div
            v-for="conversation in conversations"
            :key="conversation.id"
            class="conversation-item"
            :class="{ 'is-active': conversation.id === activeConversationId }"
          >
            <button
              type="button"
              class="conversation-main"
              @click="selectConversation(conversation)"
            >
              <span class="conversation-icon"><MessageSquareText :size="15" /></span>
              <span class="conversation-copy">
                <strong>{{ conversation.title || '新对话' }}</strong>
                <small>{{ conversation.lastMessage || '开始一段新的咨询' }}</small>
              </span>
            </button>
            <button
              type="button"
              class="conversation-delete"
              :disabled="sending"
              aria-label="删除会话"
              @click="removeConversation(conversation)"
            >
              <Trash2 :size="14" />
            </button>
          </div>

          <EmptyState
            v-if="!loadingConversations && !conversationError && !conversations.length"
            title="还没有历史会话"
            description="发送第一条消息后会保存在这里"
          />

          <div v-if="conversationError" class="sidebar-error">
            <p>{{ conversationError }}</p>
            <el-button text type="primary" @click="loadConversations">重试</el-button>
          </div>
        </div>
      </aside>

      <section class="chat-panel">
        <header class="chat-head">
          <div class="chat-title">
            <span class="agent-avatar"><Bot :size="18" /></span>
            <div>
              <strong>{{ activeConversation?.title || '新的 AI 咨询' }}</strong>
              <small>
                {{
                  contextItemId
                    ? `正在结合商品 #${contextItemId} 回答`
                    : '查询平台真实商品、规则与订单数据'
                }}
              </small>
            </div>
          </div>
          <button
            v-if="contextItemId"
            type="button"
            class="context-chip"
            @click="clearItemContext"
          >
            商品 #{{ contextItemId }} · 移除上下文
          </button>
        </header>

        <div ref="chatBody" class="chat-body">
          <div v-if="loadingMessages" v-loading="true" class="message-loading" />

          <div v-else-if="!messages.length" class="chat-welcome">
            <span class="welcome-orb"><Sparkles :size="26" /></span>
            <h2>告诉我你想找什么</h2>
            <p>可以描述预算、用途和条件，我会基于平台真实商品回答。</p>
            <div class="welcome-suggestions">
              <button
                v-for="suggestion in suggestions"
                :key="suggestion"
                type="button"
                @click="sendMessage(suggestion)"
              >
                {{ suggestion }}
              </button>
            </div>
          </div>

          <transition-group v-else name="fade" tag="div" class="message-list">
            <div
              v-for="(message, index) in messages"
              :key="message.id || `${message.role}-${index}`"
              class="message-row"
              :class="message.role"
            >
              <span v-if="message.role === 'assistant'" class="message-avatar">
                <Bot :size="15" />
              </span>
              <div
                v-if="message.role === 'assistant'"
                class="message-bubble markdown-body"
                v-html="renderAssistantMessage(message.content)"
              ></div>
              <div v-else class="message-bubble">{{ message.content }}</div>
            </div>
            <div v-if="sending" key="typing" class="message-row assistant">
              <span class="message-avatar"><Bot :size="15" /></span>
              <div class="message-bubble typing">
                <i></i><i></i><i></i>
              </div>
            </div>
          </transition-group>
        </div>

        <div class="chat-input-area">
          <el-input
            v-model="inputText"
            type="textarea"
            :rows="2"
            resize="none"
            maxlength="2000"
            placeholder="描述你的预算、用途或想了解的问题"
            :disabled="sending || loadingMessages"
            @keydown.enter.exact.prevent="sendMessage()"
          />
          <el-button
            type="primary"
            :icon="SendHorizontal"
            :loading="sending"
            aria-label="发送消息"
            @click="sendMessage()"
          >
            发送
          </el-button>
        </div>
      </section>

      <aside class="agent-aside">
        <div class="agent-aside-head">
          <span><Sparkles :size="17" /></span>
          <div>
            <strong>Agent 能力</strong>
            <small>基于真实业务数据回答</small>
          </div>
        </div>

        <div class="capability-list">
          <article v-for="capability in agentCapabilities" :key="capability.title">
            <span><component :is="capability.icon" :size="17" /></span>
            <div>
              <strong>{{ capability.title }}</strong>
              <p>{{ capability.description }}</p>
            </div>
          </article>
        </div>

        <div class="context-panel">
          <span class="context-panel-icon"><Clock3 :size="16" /></span>
          <div>
            <strong>多轮记忆</strong>
            <p>
              {{
                activeConversation?.messageCount
                  ? `当前会话已保存 ${activeConversation.messageCount} 条消息`
                  : '首次发送后自动创建并保存会话'
              }}
            </p>
          </div>
        </div>

        <div v-if="activeConversation?.updateTime" class="last-update">
          更新于 {{ formatDateTime(activeConversation.updateTime) }}
        </div>
      </aside>
    </section>
  </div>
</template>

<style scoped>
.assistant-page {
  padding-top: 20px;
}

.assistant-workspace {
  display: grid;
  grid-template-columns: 252px minmax(0, 1fr) 270px;
  gap: 14px;
  min-height: calc(100vh - 168px);
}

.conversation-sidebar,
.chat-panel,
.agent-aside {
  min-width: 0;
  overflow: hidden;
  background: var(--campus-surface);
  border: 1px solid var(--campus-line);
  border-radius: var(--campus-radius-md);
  box-shadow: var(--campus-shadow-sm);
}

.conversation-sidebar {
  display: flex;
  flex-direction: column;
  background: #f8faf9;
}

.sidebar-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 17px 16px 14px;
  border-bottom: 1px solid var(--campus-line);
}

.sidebar-kicker {
  color: var(--campus-green);
  font-size: 10px;
  font-weight: 750;
  letter-spacing: 0.1em;
}

.sidebar-head h1 {
  margin: 4px 0 0;
  font-size: 17px;
  font-weight: 760;
}

.conversation-list {
  flex: 1;
  min-height: 200px;
  padding: 9px;
  overflow-y: auto;
}

.conversation-item {
  position: relative;
  display: flex;
  align-items: center;
  gap: 4px;
  margin-bottom: 5px;
  border: 1px solid transparent;
  border-radius: var(--campus-radius-sm);
}

.conversation-item:hover {
  background: #fff;
  border-color: var(--campus-line);
}

.conversation-item.is-active {
  background: var(--campus-green-soft);
  border-color: #bfdbd1;
}

.conversation-main {
  display: flex;
  flex: 1;
  align-items: center;
  gap: 9px;
  min-width: 0;
  padding: 10px 5px 10px 9px;
  color: inherit;
  background: transparent;
  border: 0;
  text-align: left;
  cursor: pointer;
}

.conversation-icon {
  display: grid;
  flex: 0 0 30px;
  place-items: center;
  width: 30px;
  height: 30px;
  color: var(--campus-green);
  background: #fff;
  border: 1px solid var(--campus-line);
  border-radius: var(--campus-radius-sm);
}

.conversation-copy {
  min-width: 0;
}

.conversation-copy strong,
.conversation-copy small {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.conversation-copy strong {
  font-size: 12px;
}

.conversation-copy small {
  margin-top: 4px;
  color: #849089;
  font-size: 10px;
}

.conversation-delete {
  display: grid;
  flex: 0 0 28px;
  place-items: center;
  width: 28px;
  height: 28px;
  margin-right: 4px;
  padding: 0;
  color: #9aa39f;
  background: transparent;
  border: 0;
  border-radius: 5px;
  cursor: pointer;
  opacity: 0;
}

.conversation-item:hover .conversation-delete,
.conversation-delete:focus-visible {
  opacity: 1;
}

.conversation-delete:hover {
  color: #b64337;
  background: #fff0ed;
}

.conversation-delete:disabled {
  cursor: not-allowed;
  opacity: 0.35;
}

.sidebar-error {
  padding: 28px 8px;
  color: var(--campus-text);
  font-size: 12px;
  text-align: center;
}

.sidebar-error p {
  margin: 0 0 6px;
}

.chat-panel {
  display: flex;
  min-height: 620px;
  flex-direction: column;
}

.chat-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  min-height: 68px;
  padding: 12px 16px;
  border-bottom: 1px solid var(--campus-line);
}

.chat-title {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.agent-avatar {
  display: grid;
  flex: 0 0 36px;
  place-items: center;
  width: 36px;
  height: 36px;
  color: #fff;
  background: var(--campus-blue);
  border-radius: var(--campus-radius-sm);
}

.chat-title strong,
.chat-title small {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.chat-title strong {
  font-size: 14px;
}

.chat-title small {
  margin-top: 4px;
  color: var(--campus-text-soft);
  font-size: 11px;
}

.context-chip {
  flex-shrink: 0;
  height: 28px;
  padding: 0 9px;
  color: #25617d;
  background: var(--campus-blue-soft);
  border: 1px solid #c7dfe9;
  border-radius: 999px;
  font-size: 11px;
  cursor: pointer;
}

.chat-body {
  flex: 1;
  min-height: 0;
  padding: 22px;
  overflow-y: auto;
  background: #fafcfb;
}

.message-loading {
  min-height: 360px;
}

.chat-welcome {
  display: flex;
  min-height: 100%;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: var(--campus-text);
  text-align: center;
}

.welcome-orb {
  display: grid;
  place-items: center;
  width: 58px;
  height: 58px;
  color: #fff;
  background: var(--campus-green);
  border-radius: var(--campus-radius-md);
  box-shadow: 0 10px 26px rgba(20, 122, 92, 0.2);
}

.chat-welcome h2 {
  margin: 16px 0 0;
  color: var(--campus-ink);
  font-size: 19px;
  font-weight: 750;
}

.chat-welcome p {
  max-width: 460px;
  margin: 8px 0 0;
  font-size: 13px;
  line-height: 1.6;
}

.welcome-suggestions {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 8px;
  max-width: 620px;
  margin-top: 18px;
}

.welcome-suggestions button {
  min-height: 34px;
  padding: 7px 11px;
  color: #2d5e70;
  background: #fff;
  border: 1px solid #d4e2e8;
  border-radius: var(--campus-radius-sm);
  font-size: 12px;
  cursor: pointer;
}

.welcome-suggestions button:hover {
  color: var(--campus-green-dark);
  border-color: #9ac9b8;
  background: var(--campus-green-soft);
}

.message-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.message-row {
  display: flex;
  align-items: flex-start;
  gap: 9px;
}

.message-row.user {
  justify-content: flex-end;
}

.message-avatar {
  display: grid;
  flex: 0 0 30px;
  place-items: center;
  width: 30px;
  height: 30px;
  color: #fff;
  background: var(--campus-blue);
  border-radius: var(--campus-radius-sm);
}

.message-bubble {
  max-width: min(78%, 720px);
  padding: 11px 13px;
  border-radius: var(--campus-radius-md);
  font-size: 14px;
  line-height: 1.7;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}

.message-bubble :deep(*) {
  margin-top: 0;
  margin-bottom: 0;
}

.message-bubble :deep(*) + * {
  margin-top: 0.5em;
}

.message-bubble :deep(ul),
.message-bubble :deep(ol) {
  padding-left: 1.3em;
}

.message-bubble :deep(h1),
.message-bubble :deep(h2),
.message-bubble :deep(h3),
.message-bubble :deep(h4) {
  font-size: 15px;
  font-weight: 750;
}

.message-bubble :deep(a) {
  color: #1d6687;
  text-decoration: underline;
}

.message-bubble :deep(code) {
  padding: 1px 5px;
  background: rgba(37, 61, 52, 0.08);
  border-radius: 4px;
  font-family: Consolas, "Courier New", monospace;
  font-size: 12px;
}

.message-bubble :deep(blockquote) {
  margin: 0;
  padding-left: 10px;
  border-left: 3px solid rgba(37, 61, 52, 0.24);
}

.message-row.assistant .message-bubble {
  color: #32413b;
  background: #eef2f0;
  border: 1px solid #e1e7e3;
}

.message-row.user .message-bubble {
  color: #fff;
  background: var(--campus-green);
}

.message-bubble.typing {
  display: inline-flex;
  gap: 4px;
  align-items: center;
}

.message-bubble.typing i {
  width: 5px;
  height: 5px;
  background: #7b8a83;
  border-radius: 50%;
  animation: typing 1s infinite ease-in-out;
}

.message-bubble.typing i:nth-child(2) {
  animation-delay: 0.14s;
}

.message-bubble.typing i:nth-child(3) {
  animation-delay: 0.28s;
}

@keyframes typing {
  0%,
  60%,
  100% {
    opacity: 0.35;
    transform: translateY(0);
  }

  30% {
    opacity: 1;
    transform: translateY(-2px);
  }
}

.chat-input-area {
  display: flex;
  align-items: flex-end;
  gap: 10px;
  padding: 13px;
  background: #fff;
  border-top: 1px solid var(--campus-line);
}

.chat-input-area :deep(.el-textarea) {
  flex: 1;
}

.chat-input-area :deep(.el-textarea__inner) {
  min-height: 56px !important;
  box-shadow: 0 0 0 1px #dce4e0 inset;
}

.chat-input-area :deep(.el-button) {
  height: 56px;
}

.agent-aside {
  padding: 16px;
  background: #f8faf9;
}

.agent-aside-head {
  display: flex;
  align-items: center;
  gap: 9px;
  padding-bottom: 14px;
  border-bottom: 1px solid var(--campus-line);
}

.agent-aside-head > span {
  display: grid;
  place-items: center;
  width: 34px;
  height: 34px;
  color: #fff;
  background: var(--campus-green);
  border-radius: var(--campus-radius-sm);
}

.agent-aside-head strong,
.agent-aside-head small {
  display: block;
}

.agent-aside-head strong {
  font-size: 13px;
}

.agent-aside-head small {
  margin-top: 3px;
  color: var(--campus-text-soft);
  font-size: 10px;
}

.capability-list {
  display: grid;
  gap: 8px;
  margin-top: 14px;
}

.capability-list article {
  display: flex;
  gap: 9px;
  padding: 11px;
  background: #fff;
  border: 1px solid var(--campus-line);
  border-radius: var(--campus-radius-sm);
}

.capability-list article > span {
  display: grid;
  flex: 0 0 30px;
  place-items: center;
  width: 30px;
  height: 30px;
  color: var(--campus-blue);
  background: var(--campus-blue-soft);
  border-radius: var(--campus-radius-sm);
}

.capability-list strong {
  font-size: 12px;
}

.capability-list p {
  margin: 4px 0 0;
  color: var(--campus-text-soft);
  font-size: 10px;
  line-height: 1.5;
}

.context-panel {
  display: flex;
  gap: 9px;
  margin-top: 14px;
  padding: 12px;
  background: var(--campus-green-soft);
  border: 1px solid #cfe4dc;
  border-radius: var(--campus-radius-sm);
}

.context-panel-icon {
  color: var(--campus-green);
}

.context-panel strong {
  font-size: 12px;
}

.context-panel p {
  margin: 4px 0 0;
  color: #60716a;
  font-size: 10px;
  line-height: 1.5;
}

.last-update {
  margin-top: 12px;
  color: #919b96;
  font-size: 10px;
  text-align: right;
}

@media (max-width: 1180px) {
  .assistant-workspace {
    grid-template-columns: 236px minmax(0, 1fr);
  }

  .agent-aside {
    display: none;
  }
}

@media (max-width: 820px) {
  .assistant-workspace {
    grid-template-columns: 1fr;
    min-height: auto;
  }

  .conversation-sidebar {
    max-height: 230px;
  }

  .conversation-list {
    display: flex;
    gap: 7px;
    overflow-x: auto;
    overflow-y: hidden;
  }

  .conversation-item {
    flex: 0 0 190px;
    margin-bottom: 0;
    background: #fff;
  }

  .conversation-delete {
    opacity: 1;
  }

  .chat-panel {
    min-height: 620px;
  }
}

@media (max-width: 560px) {
  .assistant-page {
    padding-left: 0;
    padding-right: 0;
  }

  .assistant-workspace {
    gap: 8px;
  }

  .conversation-sidebar,
  .chat-panel {
    border-left: 0;
    border-right: 0;
    border-radius: 0;
  }

  .chat-head {
    align-items: flex-start;
    flex-direction: column;
  }

  .context-chip {
    max-width: 100%;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .chat-body {
    padding: 16px 12px;
  }

  .message-bubble {
    max-width: 88%;
  }

  .chat-input-area {
    padding: 10px;
  }

  .chat-input-area :deep(.el-button span) {
    display: none;
  }
}
</style>
