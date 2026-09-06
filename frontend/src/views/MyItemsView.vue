<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Eye, PackagePlus, Pencil, Power } from 'lucide-vue-next'

import { getMyItemsApi, takeOffItemApi } from '../api'
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

const tabs = [
  { label: '全部', value: '' },
  { label: '在售', value: 'ON_SALE' },
  { label: '已售出', value: 'SOLD' },
  { label: '已下架', value: 'OFF_SHELF' },
]

const activeStatus = ref('')
const items = ref([])
const total = ref(0)
const page = ref(1)
const size = 8
const loading = ref(false)
const loadError = ref('')

async function loadItems() {
  loading.value = true
  loadError.value = ''
  try {
    const data = await getMyItemsApi({
      status: activeStatus.value || undefined,
      page: page.value,
      size,
    })
    items.value = data.records || []
    total.value = data.total || 0
  } catch (error) {
    loadError.value = error.message || '商品加载失败'
  } finally {
    loading.value = false
  }
}

function switchTab(value) {
  activeStatus.value = value
  page.value = 1
  loadItems()
}

async function takeOffShelf(item) {
  try {
    await ElMessageBox.confirm(`确认下架“${item.title}”吗？`, '下架商品', {
      confirmButtonText: '确认下架',
      cancelButtonText: '取消',
      type: 'warning',
    })
  } catch {
    return
  }

  try {
    await takeOffItemApi(item.id)
    ElMessage.success('商品已下架')
    loadItems()
  } catch (error) {
    if (error.status !== 401) {
      ElMessage.error(error.message)
    }
  }
}

function changePage(next) {
  page.value = next
  loadItems()
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

onMounted(loadItems)
</script>

<template>
  <div class="page-shell">
    <div class="page-head">
      <div>
        <h1 class="page-title">我的商品</h1>
        <p class="page-subtitle">管理自己发布的闲置与交易状态</p>
      </div>
      <el-button type="primary" :icon="PackagePlus" @click="router.push('/publish')">
        发布商品
      </el-button>
    </div>

    <el-tabs v-model="activeStatus" class="status-tabs" @tab-change="switchTab">
      <el-tab-pane
        v-for="tab in tabs"
        :key="tab.value"
        :label="tab.label"
        :name="tab.value"
      />
    </el-tabs>

    <div v-loading="loading" class="my-list">
      <div v-for="item in items" :key="item.id" class="my-row">
        <div class="row-thumb">
          <ItemMedia
            :src="firstImage(item)"
            :alt="item.title"
            :category="categoryName(item)"
            :muted="item.status !== 'ON_SALE'"
          />
        </div>

        <div class="row-main">
          <div class="row-topline">
            <StatusPill :status="item.status" />
            <span>{{ item.categoryName }}</span>
          </div>
          <h3 class="row-title">{{ item.title }}</h3>
          <p class="row-time">发布于 {{ formatDateTime(item.createTime) }}</p>
        </div>

        <div class="row-price">{{ formatMoney(item.price) }}</div>

        <div class="row-actions">
          <template v-if="item.status === 'ON_SALE'">
            <el-button
              text
              :icon="Eye"
              @click="router.push({ name: 'item-detail', params: { id: item.id } })"
            >
              查看
            </el-button>
            <el-button
              text
              :icon="Pencil"
              @click="router.push({ name: 'item-edit', params: { id: item.id } })"
            >
              编辑
            </el-button>
            <el-button text type="danger" :icon="Power" @click="takeOffShelf(item)">
              下架
            </el-button>
          </template>
          <span v-else class="row-note">该状态不可编辑</span>
        </div>
      </div>
    </div>

    <EmptyState
      v-if="!loading && !loadError && !items.length"
      title="这里还没有商品"
      description="把闲置发布出来，让同学更容易找到"
    >
      <el-button type="primary" @click="router.push('/publish')">去发布</el-button>
    </EmptyState>

    <div v-if="!loading && loadError" class="load-error">
      <p>{{ loadError }}</p>
      <el-button @click="loadItems">重新加载</el-button>
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
.status-tabs :deep(.el-tabs__header) {
  margin-bottom: 16px;
}

.my-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  min-height: 200px;
}

.my-row {
  display: grid;
  grid-template-columns: 118px minmax(0, 1fr) 130px 190px;
  gap: 16px;
  align-items: center;
  padding: 12px;
  background: var(--campus-surface);
  border: 1px solid var(--campus-line);
  border-radius: 8px;
}

.row-thumb {
  width: 118px;
  height: 88px;
  overflow: hidden;
  border-radius: 6px;
}

.row-thumb :deep(.item-media) {
  height: 100%;
  aspect-ratio: auto;
}

.row-main {
  min-width: 0;
}

.row-topline {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #6b7771;
  font-size: 12px;
}

.row-title {
  margin: 7px 0 0;
  overflow: hidden;
  font-size: 16px;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.row-time {
  margin: 6px 0 0;
  color: #849089;
  font-size: 12px;
}

.row-price {
  color: #d2562b;
  font-size: 18px;
  font-weight: 750;
}

.row-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 2px;
}

.row-note {
  color: #9aa39f;
  font-size: 12px;
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
  .my-row {
    grid-template-columns: 92px minmax(0, 1fr) auto;
  }

  .row-thumb {
    width: 92px;
    height: 72px;
  }

  .row-price {
    grid-column: 2 / 3;
  }

  .row-actions {
    grid-column: 3 / 4;
    grid-row: 1 / span 2;
  }
}

@media (max-width: 560px) {
  .my-row {
    grid-template-columns: 78px minmax(0, 1fr);
    gap: 12px;
  }

  .row-thumb {
    width: 78px;
    height: 62px;
  }

  .row-price,
  .row-actions {
    grid-column: 1 / -1;
    grid-row: auto;
  }

  .row-actions {
    justify-content: flex-start;
    padding-top: 8px;
    border-top: 1px solid #edf0ee;
  }
}
</style>
