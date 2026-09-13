<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import ElMessage from 'element-plus/es/components/message/index.mjs'
import { ExternalLink, HeartOff } from 'lucide-vue-next'

import { getMyFavoritesApi, removeFavoriteApi } from '../api'
import EmptyState from '../components/EmptyState.vue'
import ItemMedia from '../components/ItemMedia.vue'
import StatusPill from '../components/StatusPill.vue'
import {
  categoryName,
  firstImage,
  formatDateTime,
  formatMoney,
} from '../utils/format'

const router = useRouter()

const items = ref([])
const total = ref(0)
const page = ref(1)
const size = 8
const loading = ref(false)
const removing = ref(false)
const loadError = ref('')
let loadRequestId = 0

async function loadFavorites() {
  const requestId = ++loadRequestId
  loading.value = true
  loadError.value = ''
  try {
    const data = await getMyFavoritesApi({ page: page.value, size })
    if (requestId === loadRequestId) {
      items.value = data.records || []
      total.value = data.total || 0
    }
  } catch (error) {
    if (requestId === loadRequestId) {
      loadError.value = error.message || '收藏加载失败'
    }
  } finally {
    if (requestId === loadRequestId) {
      loading.value = false
    }
  }
}

async function removeFavorite(item) {
  if (removing.value) {
    return
  }
  removing.value = true
  try {
    await removeFavoriteApi(item.id)
    ElMessage.success('已取消收藏')
    await loadFavorites()
  } catch (error) {
    if (error.status !== 401) {
      ElMessage.error(error.message)
    }
  } finally {
    removing.value = false
  }
}

function openItem(item) {
  if (item.status === 'ON_SALE') {
    router.push({ name: 'item-detail', params: { id: item.id } })
  }
}

function changePage(next) {
  page.value = next
  loadFavorites()
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

onMounted(loadFavorites)
</script>

<template>
  <div class="page-shell">
    <div class="page-head">
      <div>
        <h1 class="page-title">我的收藏</h1>
        <p class="page-subtitle">你关注的商品都在这里</p>
      </div>
    </div>

    <div v-loading="loading" class="favorite-list">
      <div v-for="item in items" :key="item.id" class="favorite-row">
        <button
          type="button"
          class="favorite-thumb"
          :disabled="item.status !== 'ON_SALE'"
          @click="openItem(item)"
        >
          <ItemMedia
            :src="firstImage(item)"
            :alt="item.title"
            :category="categoryName(item)"
            :muted="item.status !== 'ON_SALE'"
          />
        </button>

        <div class="favorite-main">
          <div class="row-topline">
            <StatusPill :status="item.status" />
            <span>{{ item.categoryName }}</span>
          </div>
          <h3
            class="favorite-title"
            :class="{ 'is-locked': item.status !== 'ON_SALE' }"
            role="button"
            tabindex="0"
            @click="openItem(item)"
            @keydown.enter="openItem(item)"
          >
            {{ item.title }}
          </h3>
          <p class="favorite-meta">
            {{ item.sellerUsername }} · {{ formatDateTime(item.createTime) }}
          </p>
        </div>

        <div class="favorite-price">{{ formatMoney(item.price) }}</div>

        <div class="favorite-actions">
          <el-button
            v-if="item.status === 'ON_SALE'"
            text
            type="primary"
            :icon="ExternalLink"
            @click="openItem(item)"
          >
            查看详情
          </el-button>
          <el-button
            text
            type="danger"
            :icon="HeartOff"
            :disabled="removing"
            @click="removeFavorite(item)"
          >
            取消收藏
          </el-button>
        </div>
      </div>
    </div>

    <EmptyState
      v-if="!loading && !loadError && !items.length"
      title="还没有收藏商品"
      description="在商品详情页点击收藏，之后可以快速回来查看"
    >
      <el-button type="primary" @click="router.push({ name: 'home' })">去逛集市</el-button>
    </EmptyState>

    <div v-if="!loading && loadError" class="load-error">
      <p>{{ loadError }}</p>
      <el-button @click="loadFavorites">重新加载</el-button>
    </div>

    <div v-if="total > size" class="pagination-row">
      <el-pagination
        background
        layout="prev, pager, next"
        :total="total"
        :page-size="size"
        :current-page="page"
        @current-change="changePage"
      />
    </div>
  </div>
</template>

<style scoped>
.favorite-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  min-height: 200px;
}

.favorite-row {
  display: grid;
  grid-template-columns: 118px minmax(0, 1fr) 130px 150px;
  gap: 16px;
  align-items: center;
  padding: 12px;
  background: var(--campus-surface);
  border: 1px solid var(--campus-line);
  border-radius: 8px;
}

.favorite-thumb {
  width: 118px;
  height: 88px;
  padding: 0;
  overflow: hidden;
  background: #edf0ee;
  border: 0;
  border-radius: 6px;
  cursor: pointer;
}

.favorite-thumb:disabled {
  cursor: default;
}

.favorite-thumb :deep(.item-media) {
  height: 100%;
  aspect-ratio: auto;
}

.favorite-main {
  min-width: 0;
}

.row-topline {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #6b7771;
  font-size: 12px;
}

.favorite-title {
  margin: 7px 0 0;
  overflow: hidden;
  color: var(--campus-ink);
  font-size: 16px;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
  cursor: pointer;
}

.favorite-title:hover:not(.is-locked) {
  color: var(--campus-green);
}

.favorite-title.is-locked {
  cursor: default;
}

.favorite-meta {
  margin: 6px 0 0;
  color: #849089;
  font-size: 12px;
}

.favorite-price {
  color: #d2562b;
  font-size: 18px;
  font-weight: 750;
}

.favorite-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 2px;
}

.load-error {
  padding: 40px 0;
  color: var(--campus-text);
  text-align: center;
}

.pagination-row {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}

@media (max-width: 860px) {
  .favorite-row {
    grid-template-columns: 92px minmax(0, 1fr);
  }

  .favorite-thumb {
    width: 92px;
    height: 72px;
  }

  .favorite-price {
    grid-column: 2 / 3;
  }

  .favorite-actions {
    grid-column: 2 / 3;
    justify-content: flex-start;
    padding-top: 6px;
  }
}

@media (max-width: 520px) {
  .favorite-row {
    grid-template-columns: 78px minmax(0, 1fr);
  }

  .favorite-thumb {
    width: 78px;
    height: 62px;
  }
}
</style>
