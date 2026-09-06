<script setup>
import { computed, ref, watch } from 'vue'
import {
  BedDouble,
  BookOpen,
  MonitorSmartphone,
  Package,
  Shirt,
} from 'lucide-vue-next'

const props = defineProps({
  src: { type: String, default: '' },
  alt: { type: String, default: '商品图片' },
  category: { type: String, default: '其他' },
  muted: { type: Boolean, default: false },
})

const imageFailed = ref(false)
watch(
  () => props.src,
  () => {
    imageFailed.value = false
  },
)

const iconMap = {
  数码产品: MonitorSmartphone,
  教材: BookOpen,
  宿舍用品: BedDouble,
  生活用品: Shirt,
  其他: Package,
}

const categoryIcon = computed(() => iconMap[props.category] || Package)
const showImage = computed(() => Boolean(props.src) && !imageFailed.value)
</script>

<template>
  <div class="item-media" :class="{ 'is-muted': muted }">
    <img
      v-if="showImage"
      :src="src"
      :alt="alt"
      loading="lazy"
      @error="imageFailed = true"
    />
    <div v-else class="media-fallback">
      <component :is="categoryIcon" :size="30" :stroke-width="1.7" />
      <span>{{ category }}</span>
    </div>
  </div>
</template>

<style scoped>
.item-media {
  position: relative;
  display: block;
  width: 100%;
  aspect-ratio: 4 / 3;
  overflow: hidden;
  background: #e9eeea;
}

.item-media img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.media-fallback {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  width: 100%;
  height: 100%;
  color: #5f756b;
  background:
    linear-gradient(135deg, rgba(20, 122, 92, 0.09), rgba(220, 122, 53, 0.07)),
    #edf1ee;
}

.media-fallback span {
  font-size: 13px;
  font-weight: 600;
  letter-spacing: 0.04em;
}

.is-muted {
  opacity: 0.72;
}
</style>
