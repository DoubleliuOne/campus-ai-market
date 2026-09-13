<script setup>
import {
  Bot,
  ChevronDown,
  Heart,
  Home,
  LogOut,
  PackagePlus,
  Search,
  ShoppingBag,
  Store,
  Ticket,
  UserRound,
} from 'lucide-vue-next'
import { useRouter } from 'vue-router'
import ElMessage from 'element-plus/es/components/message/index.mjs'

import { authState, isLoggedIn, logout } from '../stores/auth'

const router = useRouter()

async function handleLogout() {
  await logout()
  ElMessage.success('已退出登录')
  router.push({ name: 'home' })
}

function go(path) {
  router.push(path)
}
</script>

<template>
  <div class="app-layout">
    <header class="site-header">
      <div class="header-inner">
        <RouterLink to="/" class="brand" aria-label="返回首页">
          <span class="brand-mark"><ShoppingBag :size="19" /></span>
          <span class="brand-copy">
            <strong>CampusAI Market</strong>
            <small>校园二手集市</small>
          </span>
        </RouterLink>

        <nav class="site-nav" aria-label="主导航">
          <RouterLink to="/" class="nav-link" exact-active-class="is-active">
            <Home :size="17" />
            <span>首页</span>
          </RouterLink>
          <RouterLink to="/?focus=search" class="nav-link">
            <Search :size="17" />
            <span>逛集市</span>
          </RouterLink>
          <RouterLink to="/assistant" class="nav-link" active-class="is-active">
            <Bot :size="17" />
            <span>AI 助手</span>
          </RouterLink>
        </nav>

        <div class="user-area">
          <template v-if="isLoggedIn()">
            <el-button type="primary" class="publish-button" @click="go('/publish')">
              <PackagePlus :size="16" />
              <span>发布商品</span>
            </el-button>
            <el-dropdown trigger="click" popper-class="user-menu">
              <button class="user-chip" type="button">
                <span class="user-avatar"><UserRound :size="15" /></span>
                <span class="user-name">{{ authState.user?.username }}</span>
                <ChevronDown :size="14" />
              </button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item @click="go('/my-items')">
                    <Store :size="15" /> 我的商品
                  </el-dropdown-item>
                  <el-dropdown-item @click="go('/favorites')">
                    <Heart :size="15" /> 我的收藏
                  </el-dropdown-item>
                  <el-dropdown-item @click="go('/orders')">
                    <Ticket :size="15" /> 我的订单
                  </el-dropdown-item>
                  <el-dropdown-item divided @click="handleLogout">
                    <LogOut :size="15" /> 退出登录
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
          <template v-else>
            <el-button plain @click="go({ name: 'login', query: { mode: 'register' } })">
              注册
            </el-button>
            <el-button type="primary" @click="go('/login')">登录</el-button>
          </template>
        </div>
      </div>
    </header>

    <main class="site-main">
      <RouterView />
    </main>

    <footer class="site-footer">
      <span>CampusAI Market 校园二手集市</span>
      <span class="footer-sep">·</span>
      <span>Java + Spring AI + Vue</span>
    </footer>
  </div>
</template>

<style scoped>
.app-layout {
  display: flex;
  min-height: 100vh;
  flex-direction: column;
}

.site-header {
  position: sticky;
  top: 0;
  z-index: 20;
  background: rgba(255, 255, 255, 0.95);
  border-bottom: 1px solid var(--campus-line);
  backdrop-filter: blur(10px);
}

.header-inner {
  display: flex;
  align-items: center;
  gap: 20px;
  width: 100%;
  max-width: var(--campus-page-width);
  height: 66px;
  margin: 0 auto;
  padding: 0 20px;
}

.brand {
  display: inline-flex;
  flex-shrink: 0;
  align-items: center;
  gap: 9px;
}

.brand-mark {
  display: grid;
  place-items: center;
  width: 34px;
  height: 34px;
  color: #fff;
  background: var(--campus-green);
  border-radius: var(--campus-radius-md);
  box-shadow: 0 5px 14px rgba(20, 122, 92, 0.2);
}

.brand-copy {
  display: flex;
  flex-direction: column;
  line-height: 1.05;
}

.brand-copy strong {
  font-size: 15px;
  font-weight: 800;
  letter-spacing: 0;
}

.brand-copy small {
  margin-top: 4px;
  color: #6b7872;
  font-size: 11px;
  letter-spacing: 0.12em;
}

.site-nav {
  display: flex;
  flex: 1;
  align-items: center;
  gap: 4px;
}

.nav-link {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 38px;
  padding: 0 11px;
  color: #4e5a55;
  border-radius: var(--campus-radius-sm);
  font-size: 14px;
  font-weight: 600;
}

.nav-link:hover {
  color: var(--campus-green);
  background: var(--el-color-primary-light-9);
}

.nav-link.is-active {
  color: var(--campus-green-dark);
  background: var(--el-color-primary-light-9);
}

.user-area {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  gap: 10px;
}

.publish-button {
  gap: 6px;
}

.user-chip {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  height: 38px;
  padding: 0 9px 0 5px;
  color: var(--campus-ink);
  background: transparent;
  border: 0;
  border-radius: 8px;
  cursor: pointer;
}

.user-chip:hover {
  background: #eef1ef;
}

.user-avatar {
  display: grid;
  place-items: center;
  width: 28px;
  height: 28px;
  color: #fff;
  background: #2d7a8f;
  border-radius: var(--campus-radius-sm);
}

.user-name {
  max-width: 90px;
  overflow: hidden;
  font-size: 14px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.site-main {
  flex: 1;
}

.site-footer {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  min-height: 58px;
  color: #7a857f;
  border-top: 1px solid var(--campus-line);
  font-size: 12px;
}

.footer-sep {
  opacity: 0.5;
}

@media (max-width: 720px) {
  .header-inner {
    height: auto;
    min-height: 58px;
    flex-wrap: wrap;
    gap: 8px 12px;
    padding-top: 9px;
    padding-bottom: 9px;
  }

  .brand-copy small,
  .publish-button span,
  .user-name {
    display: none;
  }

  .brand-copy strong {
    font-size: 14px;
  }

  .site-nav {
    flex: 0 1 auto;
    order: 3;
    width: 100%;
    overflow-x: auto;
  }

  .nav-link {
    height: 34px;
    flex: 0 0 auto;
    justify-content: center;
    padding: 0 8px;
    font-size: 13px;
  }

  .nav-link:first-child {
    flex: 1;
  }

  .nav-link:nth-child(2) {
    flex: 1;
  }

  .nav-link:nth-child(3) {
    flex: 1;
  }

  .user-area {
    margin-left: auto;
  }

  .publish-button {
    width: 38px;
    padding: 8px;
  }
}
</style>

<style>
.user-menu .el-dropdown-menu__item {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 132px;
}
</style>
