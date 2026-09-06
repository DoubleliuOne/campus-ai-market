<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { KeyRound, ShoppingBag, UserRound } from 'lucide-vue-next'

import { authState, isLoggedIn, login, register } from '../stores/auth'

const route = useRoute()
const router = useRouter()

const mode = ref(route.query.mode === 'register' ? 'register' : 'login')
const submitting = ref(false)
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

function redirectAfterAuth() {
  const raw = typeof route.query.redirect === 'string' ? route.query.redirect : ''
  router.replace(raw.startsWith('/') && !raw.startsWith('//') ? raw : { name: 'home' })
}

async function submit() {
  if (!form.username.trim() || !form.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  if (mode.value === 'register' && form.password.length < 6) {
    ElMessage.warning('密码至少需要 6 位')
    return
  }

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
    ElMessage.error(error.message || '操作失败，请稍后重试')
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
      <RouterLink to="/" class="auth-brand">
        <span class="auth-logo"><ShoppingBag :size="22" /></span>
        <span>
          <strong>CampusAI Market</strong>
          <small>校园二手集市</small>
        </span>
      </RouterLink>

      <section class="auth-card" aria-label="账号表单">
        <div class="auth-head">
          <h1>{{ title }}</h1>
          <p>{{ mode === 'login' ? '使用账号进入校园集市' : '创建你的校园闲置交易账号' }}</p>
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
            autocomplete="current-password"
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
    linear-gradient(160deg, rgba(20, 122, 92, 0.08), rgba(247, 248, 246, 0) 42%),
    var(--campus-bg);
}

.auth-wrap {
  width: 100%;
  max-width: 420px;
}

.auth-brand {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  margin-bottom: 22px;
}

.auth-logo {
  display: grid;
  place-items: center;
  width: 40px;
  height: 40px;
  color: #fff;
  background: var(--campus-green);
  border-radius: 8px;
}

.auth-brand strong,
.auth-brand small {
  display: block;
}

.auth-brand strong {
  font-size: 17px;
  letter-spacing: 0;
}

.auth-brand small {
  margin-top: 3px;
  color: var(--campus-text);
  font-size: 12px;
  letter-spacing: 0.12em;
  text-align: center;
}

.auth-card {
  padding: 26px 24px 24px;
  background: var(--campus-surface);
  border: 1px solid var(--campus-line);
  border-radius: 8px;
  box-shadow: 0 14px 40px rgba(36, 58, 48, 0.07);
}

.auth-head {
  text-align: center;
}

.auth-head h1 {
  margin: 0;
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
  border-radius: 8px;
}

.mode-switch button {
  height: 36px;
  color: #58645e;
  background: transparent;
  border: 0;
  border-radius: 6px;
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

@media (max-width: 480px) {
  .auth-card {
    padding: 22px 18px;
  }
}
</style>
