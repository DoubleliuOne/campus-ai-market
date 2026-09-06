<script setup>
import { nextTick, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Bot, SendHorizonal, Sparkles } from 'lucide-vue-next'
import { marked } from 'marked'
import DOMPurify from 'dompurify'

import { agentChatApi } from '../api'

const route = useRoute()

const messages = ref([])
const inputText = ref('')
const loading = ref(false)
const chatBody = ref()
const itemId = ref(Number(route.query.itemId) || null)

const suggestions = [
  '帮我找 200 元以内的机械键盘',
  '推荐适合宿舍的闲置好物',
  '平台不能发布什么商品？',
  '看看我最近的订单情况',
]

async function scrollToBottom() {
  await nextTick()
  if (chatBody.value) {
    chatBody.value.scrollTop = chatBody.value.scrollHeight
  }
}

function pushMessage(role, content) {
  messages.value.push({ role, content })
  scrollToBottom()
}

function renderAssistantMessage(content) {
  const html = marked.parse(content || '', {
    async: false,
    gfm: true,
    breaks: true,
  })
  return DOMPurify.sanitize(typeof html === 'string' ? html : '')
}

async function sendMessage(content) {
  const text = (content ?? inputText.value).trim()
  if (!text || loading.value) {
    return
  }

  inputText.value = ''
  pushMessage('user', text)
  loading.value = true
  try {
    const result = await agentChatApi({
      message: text,
      itemId: itemId.value,
    })
    pushMessage('assistant', result.reply || '暂时没有获得回答，请再试一次。')
  } catch (error) {
    if (error.status !== 401) {
      ElMessage.error(error.message || 'AI 服务暂时不可用')
    }
  } finally {
    loading.value = false
    scrollToBottom()
  }
}

function pickSuggestion(text) {
  sendMessage(text)
}

onMounted(() => {
  if (itemId.value) {
    pushMessage(
      'assistant',
      `我会结合当前查看的这件商品（#${itemId.value}）来回答。`,
    )
  }
})

watch(
  () => route.query.itemId,
  (value) => {
    const nextItemId = value ? Number(value) : null
    if (nextItemId === itemId.value) {
      return
    }
    itemId.value = nextItemId
    messages.value = []
    if (nextItemId) {
      pushMessage(
        'assistant',
        `我会结合当前查看的这件商品（#${nextItemId}）来回答。`,
      )
    }
  },
)
</script>

<template>
  <div class="page-shell assistant-page">
    <div class="assistant-layout">
      <aside class="assistant-aside">
        <div class="aside-head">
          <span class="aside-icon"><Bot :size="21" /></span>
          <div>
            <h1>AI 助手</h1>
            <p v-if="itemId">已关联商品 #{{ itemId }}</p>
            <p v-else>校园交易问答与真实商品搜索</p>
          </div>
        </div>

        <div v-if="!messages.length" class="suggestion-list">
          <button
            v-for="suggestion in suggestions"
            :key="suggestion"
            type="button"
            class="suggestion-chip"
            @click="pickSuggestion(suggestion)"
          >
            <Sparkles :size="14" />
            {{ suggestion }}
          </button>
        </div>
      </aside>

      <section class="chat-panel">
        <div ref="chatBody" class="chat-body">
          <div v-if="!messages.length" class="chat-welcome">
            <span class="welcome-orb"><Bot :size="30" /></span>
            <h2>想找什么，或想了解什么？</h2>
            <p>直接描述你的预算与需求，我会基于平台真实商品回答。</p>
          </div>

          <transition-group v-else name="fade" tag="div" class="message-list">
            <div
              v-for="(message, index) in messages"
              :key="index"
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
            <div v-if="loading" key="typing" class="message-row assistant">
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
            placeholder="例如：想买一个 300 元内的台灯"
            :disabled="loading"
            @keydown.enter.exact.prevent="sendMessage()"
            @keydown.enter.shift.exact="() => {}"
          />
          <el-button
            type="primary"
            :icon="SendHorizonal"
            :loading="loading"
            aria-label="发送消息"
            @click="sendMessage()"
          >
            发送
          </el-button>
        </div>
      </section>
    </div>
  </div>
</template>

<style scoped>
.assistant-page {
  padding-top: 24px;
}

.assistant-layout {
  display: grid;
  grid-template-columns: 260px minmax(0, 1fr);
  gap: 16px;
  min-height: calc(100vh - 190px);
}

.assistant-aside {
  padding: 18px;
  background: #eaf3f8;
  border-radius: 8px;
}

.aside-head {
  display: flex;
  align-items: center;
  gap: 11px;
  padding-bottom: 16px;
  border-bottom: 1px solid #cfdfe8;
}

.aside-icon {
  display: grid;
  place-items: center;
  width: 42px;
  height: 42px;
  color: #fff;
  background: #256e8f;
  border-radius: 8px;
}

.aside-head h1 {
  margin: 0;
  font-size: 18px;
  font-weight: 750;
}

.aside-head p {
  margin: 4px 0 0;
  color: #57717e;
  font-size: 12px;
}

.suggestion-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 16px;
}

.suggestion-chip {
  display: inline-flex;
  align-items: flex-start;
  gap: 8px;
  width: 100%;
  padding: 10px 11px;
  color: #2b576c;
  background: #fff;
  border: 1px solid #d5e3ea;
  border-radius: 6px;
  font-size: 13px;
  line-height: 1.45;
  text-align: left;
  cursor: pointer;
}

.suggestion-chip:hover {
  border-color: #8fb9cd;
}

.chat-panel {
  display: flex;
  min-width: 0;
  min-height: 520px;
  flex-direction: column;
  overflow: hidden;
  background: #fff;
  border: 1px solid var(--campus-line);
  border-radius: 8px;
}

.chat-body {
  flex: 1;
  min-height: 0;
  padding: 24px;
  overflow-y: auto;
  background:
    linear-gradient(rgba(247, 249, 248, 0.78), rgba(247, 249, 248, 0.78)),
    #fff;
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
  width: 62px;
  height: 62px;
  color: #fff;
  background: var(--campus-green);
  border-radius: 8px;
}

.chat-welcome h2 {
  margin: 16px 0 0;
  color: var(--campus-ink);
  font-size: 19px;
  font-weight: 750;
}

.chat-welcome p {
  margin: 8px 0 0;
  font-size: 13px;
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
  background: #256e8f;
  border-radius: 6px;
}

.message-bubble {
  max-width: 78%;
  padding: 11px 13px;
  border-radius: 8px;
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

.message-bubble :deep(img) {
  display: block;
  max-width: 100%;
  margin-top: 8px;
  border-radius: 6px;
}

.message-bubble :deep(blockquote) {
  margin: 0;
  padding-left: 10px;
  border-left: 3px solid rgba(37, 61, 52, 0.24);
}

.message-row.assistant .message-bubble {
  color: #32413b;
  background: #eef2f0;
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
  padding: 14px;
  border-top: 1px solid var(--campus-line);
}

.chat-input-area :deep(.el-textarea) {
  flex: 1;
}

.chat-input-area :deep(.el-textarea__inner) {
  min-height: 54px !important;
  box-shadow: 0 0 0 1px #dce4e0 inset;
}

.chat-input-area :deep(.el-button) {
  height: 54px;
}

@media (max-width: 860px) {
  .assistant-layout {
    grid-template-columns: 1fr;
  }

  .assistant-aside {
    padding: 14px;
  }

  .suggestion-list {
    flex-direction: row;
    margin-top: 12px;
    overflow-x: auto;
  }

  .suggestion-chip {
    flex: 0 0 auto;
    width: auto;
    max-width: 220px;
  }

  .chat-panel {
    min-height: 460px;
  }
}
</style>
