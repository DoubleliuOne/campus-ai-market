<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import ElMessage from 'element-plus/es/components/message/index.mjs'
import {
  Bot,
  KeyRound,
  PackageSearch,
  ShieldCheck,
  ShoppingBag,
  UserRound,
} from 'lucide-vue-next'

import { authState, isLoggedIn, login, register } from '../stores/auth'

const route = useRoute()
const router = useRouter()

const mode = ref(route.query.mode === 'register' ? 'register' : 'login')
const submitting = ref(false)
const formError = ref('')
const form = reactive({
  username: '',
  password: '',
  campus: '',
})

watch(
  () => route.query.mode,
  (value) => {
    mode.value = value === 'register' ? 'register' : 'login'
  },
)

const title = computed(() => (mode.value === 'login' ? '登录校园集市' : '注册新账号'))

watch(
  () => [mode.value, form.username, form.password, form.campus],
  () => {
    formError.value = ''
  },
)

function redirectAfterAuth() {
  const raw = typeof route.query.redirect === 'string' ? route.query.redirect : ''
  router.replace(raw.startsWith('/') && !raw.startsWith('//') ? raw : { name: 'home' })
}

async function submit() {
  if (!form.username.trim() || !form.password) {
    formError.value = '请输入用户名和密码'
    return
  }
  if (mode.value === 'register' && form.password.length < 6) {
    formError.value = '密码至少需要 6 位'
    return
  }

  formError.value = ''
  submitting.value = true
  try {
    if (mode.value === 'login') {
      await login({
        username: form.username.trim(),
        password: form.password,
      })
      ElMessage.success(`欢迎回来，${authState.user?.username || ''}`)
      redirectAfterAuth()
    } else {
      await register({
        username: form.username.trim(),
        password: form.password,
        campus: form.campus.trim() || null,
      })
      ElMessage.success('注册成功，请登录')
      mode.value = 'login'
      form.password = ''
      form.campus = ''
      router.replace({ query: {} })
    }
  } catch (error) {
    formError.value = error.message || '操作失败，请稍后重试'
    ElMessage.error(formError.value)
  } finally {
    submitting.value = false
  }
}

if (isLoggedIn()) {
  redirectAfterAuth()
}
</script>

<template>
  <div class="auth-page">
    <div class="auth-wrap">
      <section class="auth-visual">
        <RouterLink to="/" class="auth-brand">
          <span class="auth-logo"><ShoppingBag :size="22" /></span>
          <span>
            <strong>CampusAI Market</strong>
            <small>校园二手集市</small>
          </span>
        </RouterLink>

        <div class="visual-copy">
          <span class="visual-kicker">AI 驱动的校园交易</span>
          <h1>不只搜索商品，<br />更能理解你的需求。</h1>
          <p>基于平台真实商品、规则知识和订单数据，完成更可靠的校园闲置交易。</p>
        </div>

        <div class="visual-features">
          <div class="feature-item">
            <span><Bot :size="17" /></span>
            <div>
              <strong>AI 商品助手</strong>
              <small>按预算和用途查找真实在售商品</small>
            </div>
          </div>
          <div class="feature-item">
            <span><PackageSearch :size="17" /></span>
            <div>
              <strong>校园闲置流转</strong>
              <small>发布、收藏、下单与交易状态一站管理</small>
            </div>
          </div>
          <div class="feature-item">
            <span><ShieldCheck :size="17" /></span>
            <div>
              <strong>真实业务数据</strong>
              <small>AI 回答来自 MySQL 与平台规则知识库</small>
            </div>
          </div>
        </div>
      </section>

      <section class="auth-card" aria-label="账号表单">
        <div class="auth-head">
          <div>
            <span class="auth-eyebrow">{{ mode === 'login' ? '欢迎回来' : '加入集市' }}</span>
            <h2>{{ title }}</h2>
            <p>{{ mode === 'login' ? '使用账号继续你的校园交易' : '创建账号，开始发布与发现闲置' }}</p>
          </div>
        </div>

        <div class="mode-switch" role="tablist">
          <button
            type="button"
            role="tab"
            :aria-selected="mode === 'login'"
            :class="{ 'is-active': mode === 'login' }"
            @click="mode = 'login'"
          >
            登录
          </button>
          <button
            type="button"
            role="tab"
            :aria-selected="mode === 'register'"
            :class="{ 'is-active': mode === 'register' }"
            @click="mode = 'register'"
          >
            注册
          </button>
        </div>

        <form class="auth-form" @submit.prevent="submit">
          <label class="field-label" for="auth-username">用户名</label>
          <el-input
            id="auth-username"
            v-model="form.username"
            size="large"
            :placeholder="mode === 'login' ? '输入用户名' : '设置用户名'"
            autocomplete="username"
            @keyup.enter="submit"
          >
            <template #prefix><UserRound :size="16" /></template>
          </el-input>

          <label class="field-label" for="auth-password">密码</label>
          <el-input
            id="auth-password"
            v-model="form.password"
            size="large"
            type="password"
            show-password
            :placeholder="mode === 'login' ? '输入密码' : '至少 6 位密码'"
            :autocomplete="mode === 'login' ? 'current-password' : 'new-password'"
            @keyup.enter="submit"
          >
            <template #prefix><KeyRound :size="16" /></template>
          </el-input>

          <template v-if="mode === 'register'">
            <label class="field-label" for="auth-campus">所在校区</label>
            <el-input
              id="auth-campus"
              v-model="form.campus"
              size="large"
              placeholder="选填，如：大学城校区"
              @keyup.enter="submit"
            />
          </template>

          <p v-if="formError" class="form-error" role="alert">{{ formError }}</p>

          <el-button
            type="primary"
            size="large"
            native-type="submit"
            class="auth-submit"
            :loading="submitting"
          >
            {{ mode === 'login' ? '登录' : '注册并去登录' }}
          </el-button>
        </form>
      </section>
    </div>
  </div>
</template>

<style scoped>
.auth-page {
  display: grid;
  min-height: 100vh;
  place-items: center;
  padding: 30px 16px;
  background:
    linear-gradient(145deg, rgba(20, 122, 92, 0.07), rgba(244, 246, 245, 0) 46%),
    var(--campus-bg);
}

.auth-wrap {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 420px;
  width: 100%;
  max-width: 980px;
  overflow: hidden;
  background: var(--campus-surface);
  border: 1px solid var(--campus-line);
  border-radius: var(--campus-radius-md);
  box-shadow: 0 24px 70px rgba(24, 50, 40, 0.12);
}

.auth-visual {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  min-height: 620px;
  padding: 38px;
  color: #eef8f3;
  background: #155b49;
}

.auth-brand {
  display: flex;
  align-items: center;
  gap: 10px;
}

.auth-logo {
  display: grid;
  place-items: center;
  width: 40px;
  height: 40px;
  color: #fff;
  color: #155b49;
  background: #fff;
  border-radius: var(--campus-radius-md);
}

.auth-brand strong,
.auth-brand small {
  display: block;
}

.auth-brand strong {
  color: #fff;
  font-size: 17px;
  letter-spacing: 0;
}

.auth-brand small {
  margin-top: 3px;
  color: rgba(238, 248, 243, 0.7);
  font-size: 12px;
  letter-spacing: 0.12em;
}

.visual-copy {
  max-width: 430px;
  margin: auto 0;
  padding: 34px 0;
}

.visual-kicker {
  display: inline-block;
  color: #a8d9c8;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.12em;
}

.visual-copy h1 {
  margin: 14px 0 0;
  color: #fff;
  font-size: clamp(28px, 3.2vw, 40px);
  font-weight: 780;
  line-height: 1.32;
  letter-spacing: 0;
}

.visual-copy p {
  margin: 16px 0 0;
  color: rgba(238, 248, 243, 0.76);
  font-size: 14px;
  line-height: 1.75;
}

.visual-features {
  display: grid;
  gap: 10px;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 11px;
  padding: 11px 12px;
  background: rgba(255, 255, 255, 0.07);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: var(--campus-radius-sm);
}

.feature-item > span {
  display: grid;
  flex: 0 0 34px;
  place-items: center;
  width: 34px;
  height: 34px;
  color: #b7e2d4;
  background: rgba(255, 255, 255, 0.08);
  border-radius: var(--campus-radius-sm);
}

.feature-item strong,
.feature-item small {
  display: block;
}

.feature-item strong {
  color: #fff;
  font-size: 13px;
}

.feature-item small {
  margin-top: 3px;
  color: rgba(238, 248, 243, 0.63);
  font-size: 11px;
}

.auth-card {
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 42px 36px;
}

.auth-head {
  text-align: left;
}

.auth-eyebrow {
  color: var(--campus-green);
  font-size: 12px;
  font-weight: 750;
  letter-spacing: 0.08em;
}

.auth-head h2 {
  margin: 0;
  margin-top: 7px;
  font-size: 22px;
  font-weight: 750;
  letter-spacing: 0;
}

.auth-head p {
  margin: 7px 0 0;
  color: var(--campus-text);
  font-size: 13px;
}

.mode-switch {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 4px;
  margin: 20px 0;
  padding: 4px;
  background: #eef1ef;
  border-radius: var(--campus-radius-md);
}

.mode-switch button {
  height: 36px;
  color: #58645e;
  background: transparent;
  border: 0;
  border-radius: var(--campus-radius-sm);
  font-size: 14px;
  font-weight: 650;
  cursor: pointer;
}

.mode-switch button.is-active {
  color: var(--campus-green-dark);
  background: #fff;
  box-shadow: 0 2px 8px rgba(36, 58, 48, 0.08);
}

.auth-form {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.field-label {
  margin-top: 10px;
  color: #4b5752;
  font-size: 13px;
  font-weight: 650;
}

.auth-submit {
  width: 100%;
  margin-top: 20px;
}

.form-error {
  margin: 8px 0 0;
  padding: 9px 11px;
  color: #a43c31;
  background: #fff0ed;
  border: 1px solid #f2c8c1;
  border-radius: var(--campus-radius-sm);
  font-size: 13px;
  line-height: 1.45;
}

@media (max-width: 820px) {
  .auth-wrap {
    grid-template-columns: 1fr;
    max-width: 460px;
  }

  .auth-visual {
    min-height: auto;
    padding: 24px;
  }

  .visual-copy {
    padding: 30px 0 8px;
  }

  .visual-copy h1 {
    font-size: 27px;
  }

  .visual-features {
    display: none;
  }
}

@media (max-width: 480px) {
  .auth-page {
    padding: 0;
    background: var(--campus-surface);
  }

  .auth-wrap {
    min-height: 100vh;
    border: 0;
    border-radius: 0;
    box-shadow: none;
  }

  .auth-visual {
    padding: 22px 18px;
  }

  .visual-copy {
    display: none;
  }

  .auth-card {
    padding: 26px 18px 36px;
  }
}
</style>
