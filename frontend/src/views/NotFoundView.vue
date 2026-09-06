<script setup>
import { useRouter } from 'vue-router'

import EmptyState from '../components/EmptyState.vue'

const router = useRouter()
</script>

<template>
  <div class="not-found-page">
    <EmptyState title="页面不存在" description="你访问的页面可能已经移动或删除">
      <el-button type="primary" @click="router.push({ name: 'home' })">回到首页</el-button>
    </EmptyState>
  </div>
</template>

<style scoped>
.not-found-page {
  max-width: 720px;
  min-height: 60vh;
  margin: 0 auto;
  padding: 40px 20px;
}
</style>
