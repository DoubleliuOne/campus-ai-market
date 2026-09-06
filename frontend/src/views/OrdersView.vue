<script setup>
import { onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Ban, CheckCircle2 } from 'lucide-vue-next'

import { getMyOrdersApi, updateOrderStatusApi } from '../api'
import EmptyState from '../components/EmptyState.vue'
import ItemMedia from '../components/ItemMedia.vue'
import StatusPill from '../components/StatusPill.vue'
import { categoryName, firstImage, formatDateTime, formatMoney } from '../utils/format'

const route = useRoute()
const router = useRouter()

const tabs = [
  { label: '我买到的', value: 'buyer' },
  { label: '我卖出的', value: 'seller' },
]
const role = ref(route.query.role === 'seller' ? 'seller' : 'buyer')
const orders = ref([])
const total = ref(0)
const page = ref(1)
const size = 8
const loading = ref(false)
const acting = ref(false)
const loadError = ref('')

async function loadOrders() {
  loading.value = true
  loadError.value = ''
  try {
    const data = await getMyOrdersApi({
      role: role.value,
      page: page.value,
      size,
    })
    orders.value = data.records || []
    total.value = data.total || 0
  } catch (error) {
    loadError.value = error.message || '订单加载失败'
  } finally {
    loading.value = false
  }
}

function switchRole(value) {
  role.value = value
  page.value = 1
  router.replace({ query: { role: value } })
  loadOrders()
}

async function changeOrderStatus(order, targetStatus, confirmText) {
  try {
    await ElMessageBox.confirm(confirmText, '订单操作', {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      type: 'info',
    })
  } catch {
    return
  }

  acting.value = true
  try {
    await updateOrderStatusApi(order.id, targetStatus)
    ElMessage.success('订单状态已更新')
    loadOrders()
  } catch (error) {
    if (error.status !== 401) {
      ElMessage.error(error.message)
    }
  } finally {
    acting.value = false
  }
}

function changePage(next) {
  page.value = next
  loadOrders()
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

watch(
  () => route.query.role,
  (value) => {
    const next = value === 'seller' ? 'seller' : 'buyer'
    if (next !== role.value) {
      role.value = next
      page.value = 1
      loadOrders()
    }
  },
)

onMounted(loadOrders)
</script>

<template>
  <div class="page-shell">
    <div class="page-head">
      <div>
        <h1 class="page-title">我的订单</h1>
        <p class="page-subtitle">跟进交易进度与订单状态</p>
      </div>
    </div>

    <el-tabs v-model="role" class="status-tabs" @tab-change="switchRole">
      <el-tab-pane
        v-for="tab in tabs"
        :key="tab.value"
        :label="tab.label"
        :name="tab.value"
      />
    </el-tabs>

    <div v-loading="loading" class="order-list">
      <div v-for="order in orders" :key="order.id" class="order-row">
        <div class="order-thumb">
          <ItemMedia
            :src="firstImage(order)"
            :alt="order.itemTitle"
            :category="order.itemTitle || '商品'"
            :muted="false"
          />
        </div>

        <div class="order-main">
          <div class="order-topline">
            <StatusPill :status="order.status" kind="order" />
            <span class="order-number">订单 #{{ order.id }}</span>
          </div>
          <h3 class="order-title">{{ order.itemTitle }}</h3>
          <p class="order-meta">
            <template v-if="role === 'buyer'">
              卖家：{{ order.sellerUsername }}
            </template>
            <template v-else>
              买家：{{ order.buyerUsername }}
            </template>
            <span>· {{ formatDateTime(order.createTime) }}</span>
          </p>
        </div>

        <div class="order-price">{{ formatMoney(order.price) }}</div>

        <div class="order-actions">
          <template v-if="order.status === 'CREATED'">
            <el-button
              v-if="role === 'buyer'"
              text
              type="danger"
              :icon="Ban"
              :disabled="acting"
              @click="
                changeOrderStatus(
                  order,
                  'CANCELLED',
                  `确认取消订单 #${order.id}？取消后商品会恢复在售。`,
                )
              "
            >
              取消订单
            </el-button>
            <el-button
              v-else
              text
              type="primary"
              :icon="CheckCircle2"
              :disabled="acting"
              @click="
                changeOrderStatus(
                  order,
                  'COMPLETED',
                  `确认完成订单 #${order.id}？`,
                )
              "
            >
              确认完成
            </el-button>
          </template>
          <span v-else class="order-note">已结束的交易</span>
        </div>
      </div>
    </div>

    <EmptyState
      v-if="!loading && !loadError && !orders.length"
      title="这里还没有订单"
      :description="role === 'buyer' ? '去集市找到需要的商品并下单' : '收到买家下单后会显示在这里'"
    >
      <el-button v-if="role === 'buyer'" type="primary" @click="router.push({ name: 'home' })">
        去逛集市
      </el-button>
    </EmptyState>

    <div v-if="!loading && loadError" class="load-error">
      <p>{{ loadError }}</p>
      <el-button @click="loadOrders">重新加载</el-button>
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

.order-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  min-height: 200px;
}

.order-row {
  display: grid;
  grid-template-columns: 118px minmax(0, 1fr) 130px 150px;
  gap: 16px;
  align-items: center;
  padding: 12px;
  background: var(--campus-surface);
  border: 1px solid var(--campus-line);
  border-radius: 8px;
}

.order-thumb {
  width: 118px;
  height: 88px;
  overflow: hidden;
  border-radius: 6px;
}

.order-thumb :deep(.item-media) {
  height: 100%;
  aspect-ratio: auto;
}

.order-main {
  min-width: 0;
}

.order-topline {
  display: flex;
  align-items: center;
  gap: 8px;
}

.order-number {
  color: #98a19d;
  font-size: 11px;
}

.order-title {
  margin: 7px 0 0;
  overflow: hidden;
  font-size: 16px;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.order-meta {
  margin: 6px 0 0;
  color: #7e8a84;
  font-size: 12px;
}

.order-price {
  color: #d2562b;
  font-size: 18px;
  font-weight: 750;
}

.order-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.order-note {
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
  .order-row {
    grid-template-columns: 92px minmax(0, 1fr);
  }

  .order-thumb {
    width: 92px;
    height: 72px;
  }

  .order-price {
    grid-column: 2 / 3;
  }

  .order-actions {
    grid-column: 2 / 3;
    justify-content: flex-start;
    padding-top: 6px;
  }
}

@media (max-width: 520px) {
  .order-row {
    grid-template-columns: 78px minmax(0, 1fr);
  }

  .order-thumb {
    width: 78px;
    height: 62px;
  }
}
</style>
