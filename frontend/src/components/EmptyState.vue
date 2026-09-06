<script setup>
import { PackageSearch } from 'lucide-vue-next'

defineProps({
  title: { type: String, default: '这里还没有内容' },
  description: { type: String, default: '' },
})
</script>

<template>
  <div class="empty-state">
    <div class="empty-icon"><PackageSearch :size="26" /></div>
    <p class="empty-title">{{ title }}</p>
    <p v-if="description" class="empty-description">{{ description }}</p>
    <div v-if="$slots.default" class="empty-action">
      <slot />
    </div>
  </div>
</template>

<style scoped>
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 260px;
  padding: 36px 20px;
  text-align: center;
}

.empty-icon {
  display: grid;
  place-items: center;
  width: 56px;
  height: 56px;
  color: var(--campus-green);
  background: var(--el-color-primary-light-9);
  border-radius: 8px;
}

.empty-title {
  margin: 16px 0 0;
  font-size: 16px;
  font-weight: 700;
}

.empty-description {
  max-width: 420px;
  margin: 8px 0 0;
  color: var(--campus-text);
  font-size: 13px;
  line-height: 1.6;
}

.empty-action {
  margin-top: 18px;
}
</style>
