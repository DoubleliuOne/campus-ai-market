<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import ElMessage from 'element-plus/es/components/message/index.mjs'
import ElMessageBox from 'element-plus/es/components/message-box/index.mjs'
import {
  ArrowLeft,
  Bot,
  Check,
  Heart,
  RotateCcw,
  Store,
} from 'lucide-vue-next'

import {
  addFavoriteApi,
  checkFavoriteApi,
  createOrderApi,
  getItemDetailApi,
  getManagedItemDetailApi,
  relistItemApi,
  removeFavoriteApi,
} from '../api'
import ItemMedia from '../components/ItemMedia.vue'
import EmptyState from '../components/EmptyState.vue'
import StatusPill from '../components/StatusPill.vue'
import { authState, isLoggedIn } from '../stores/auth'
import { categoryName, firstImage, formatDateTime, formatMoney } from '../utils/format'

const route = useRoute()
const router = useRouter()

const item = ref(null)
const favorite = ref(false)
const activeImage = ref('')
const loading = ref(true)
const loadError = ref('')
const acting = ref(false)

const itemId = computed(() => Number(route.params.id))
const images = computed(() => (Array.isArray(item.value?.images) ? item.value.images.filter(Boolean) : []))
const isSeller = computed(() =>
  isLoggedIn() && item.value ? authState.user?.id === item.value.sellerId : false,
)

async function loadDetail() {
  loading.value = true
  loadError.value = ''
  item.value = null
  favorite.value = false
  try {
    const data =
      route.query.manage === '1' && isLoggedIn()
        ? await getManagedItemDetailApi(itemId.value)
        : await getItemDetailApi(itemId.value)
    item.value = data
    activeImage.value = firstImage(data)
    if (isLoggedIn() && authState.user?.id !== data.sellerId) {
      favorite.value = Boolean(await checkFavoriteApi(data.id))
    }
  } catch (error) {
    loadError.value = error.message || '商品加载失败'
  } finally {
    loading.value = false
  }
}

async function relistItem() {
  if (!item.value) {
    return
  }
  acting.value = true
  try {
    await relistItemApi(item.value.id)
    ElMessage.success('商品已重新上架')
    await loadDetail()
  } catch (error) {
    if (error.status !== 401) {
      ElMessage.error(error.message)
    }
  } finally {
    acting.value = false
  }
}

async function toggleFavorite() {
  if (!isLoggedIn()) {
    router.push({ name: 'login', query: { redirect: route.fullPath } })
    return
  }
  if (!item.value) {
    return
  }
  acting.value = true
  try {
    if (favorite.value) {
      await removeFavoriteApi(item.value.id)
      favorite.value = false
      ElMessage.success('已取消收藏')
    } else {
      await addFavoriteApi(item.value.id)
      favorite.value = true
      ElMessage.success('已加入收藏')
    }
  } catch (error) {
    if (error.status !== 401) {
      ElMessage.error(error.message)
    }
  } finally {
    acting.value = false
  }
}

async function buyNow() {
  if (!isLoggedIn()) {
    router.push({ name: 'login', query: { redirect: route.fullPath } })
    return
  }
  if (!item.value) {
    return
  }
  try {
    await ElMessageBox.confirm(
      `确定以 ${formatMoney(item.value.price)} 购买这件商品吗？确认后其他买家将无法继续下单。`,
      '确认购买',
      {
        confirmButtonText: '确认购买',
        cancelButtonText: '再看看',
        type: 'info',
      },
    )
  } catch {
    return
  }

  acting.value = true
  try {
    await createOrderApi(item.value.id)
    ElMessage.success('下单成功，等待卖家完成交易')
    item.value.status = 'SOLD'
    router.push({ name: 'orders', query: { role: 'buyer' } })
  } catch (error) {
    if (error.status !== 401) {
      ElMessage.error(error.message)
    }
  } finally {
    acting.value = false
  }
}

function backToMarket() {
  router.back()
}

watch([itemId, () => route.query.manage], () => {
  if (itemId.value) {
    loadDetail()
  }
})

onMounted(loadDetail)
</script>

<template>
  <div class="page-shell detail-page">
    <button type="button" class="back-link" @click="backToMarket">
      <ArrowLeft :size="16" />
      返回
    </button>

    <div v-if="loading" v-loading="true" class="detail-loading" />

    <EmptyState
      v-else-if="!loading && loadError && !item"
      title="暂时看不到这件商品"
      :description="loadError"
    >
      <el-button type="primary" @click="router.push({ name: 'home' })">回到集市</el-button>
    </EmptyState>

    <div v-else-if="item" class="detail-layout">
      <section class="detail-media">
        <ItemMedia
          :src="activeImage || firstImage(item)"
          :alt="item.title"
          :category="categoryName(item)"
        />
        <div v-if="images.length > 1" class="thumbnail-row">
          <button
            v-for="(image, index) in images"
            :key="image + index"
            type="button"
            class="thumbnail"
            :class="{ 'is-active': image === activeImage }"
            @click="activeImage = image"
          >
            <img :src="image" :alt="`商品图片 ${index + 1}`" />
          </button>
        </div>
      </section>

      <section class="detail-info">
        <div class="info-topline">
          <StatusPill v-if="item.status !== 'ON_SALE'" :status="item.status" />
          <span v-else class="top-category">{{ item.categoryName }}</span>
          <span class="listed-time">发布于 {{ formatDateTime(item.createTime) }}</span>
        </div>

        <h1>{{ item.title }}</h1>
        <div class="detail-price">{{ formatMoney(item.price) }}</div>

        <div class="detail-description">
          <h2>商品描述</h2>
          <p>{{ item.description || '卖家没有填写更多描述' }}</p>
        </div>

        <dl class="seller-block">
          <div>
            <dt>卖家</dt>
            <dd>{{ item.sellerUsername }}</dd>
          </div>
          <div>
            <dt>分类</dt>
            <dd>{{ item.categoryName }}</dd>
          </div>
        </dl>

        <div class="detail-actions">
          <template v-if="isSeller">
            <el-button
              type="primary"
              :icon="Store"
              @click="router.push({ name: 'my-items' })"
            >
              这是你发布的商品
            </el-button>
            <el-button
              v-if="item.status === 'OFF_SHELF'"
              type="primary"
              :icon="RotateCcw"
              :loading="acting"
              @click="relistItem"
            >
              重新上架
            </el-button>
          </template>
          <template v-else-if="item.status === 'ON_SALE'">
            <el-button type="primary" size="large" :loading="acting" @click="buyNow">
              立即购买
            </el-button>
            <el-button
              size="large"
              :type="favorite ? 'warning' : 'default'"
              :loading="acting"
              @click="toggleFavorite"
            >
              <Heart :size="16" :fill="favorite ? 'currentColor' : 'none'" />
              {{ favorite ? '已收藏' : '收藏' }}
            </el-button>
          </template>
          <template v-else>
            <el-button type="primary" plain :icon="Check" disabled>当前不可购买</el-button>
          </template>

          <RouterLink
            class="ask-ai"
            :to="{ name: 'assistant', query: { itemId: item.id } }"
          >
            <Bot :size="16" />
            问问 AI 这件商品
          </RouterLink>
        </div>
      </section>
    </div>
  </div>
</template>

<style scoped>
.detail-page {
  padding-top: 18px;
}

.back-link {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 14px;
  padding: 4px 0;
  color: var(--campus-text);
  background: none;
  border: 0;
  font-size: 13px;
  cursor: pointer;
}

.back-link:hover {
  color: var(--campus-green);
}

.detail-loading {
  min-height: 420px;
}

.detail-layout {
  display: grid;
  grid-template-columns: minmax(0, 560px) minmax(0, 1fr);
  gap: 28px;
  align-items: start;
}

.detail-media {
  position: sticky;
  top: 82px;
  overflow: hidden;
  background: #fff;
  border: 1px solid var(--campus-line);
  border-radius: 8px;
}

.detail-media :deep(.item-media) {
  aspect-ratio: 1;
  background: #eef1ef;
}

.thumbnail-row {
  display: flex;
  gap: 10px;
  padding: 12px;
  overflow-x: auto;
}

.thumbnail {
  width: 64px;
  height: 64px;
  flex: 0 0 64px;
  padding: 0;
  overflow: hidden;
  background: #eef1ef;
  border: 2px solid transparent;
  border-radius: 6px;
  cursor: pointer;
}

.thumbnail.is-active {
  border-color: var(--campus-green);
}

.thumbnail img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.detail-info {
  min-width: 0;
}

.info-topline {
  display: flex;
  align-items: center;
  gap: 10px;
  color: #69766f;
  font-size: 13px;
}

.top-category {
  color: var(--campus-green-dark);
  font-weight: 650;
}

.detail-info h1 {
  margin: 12px 0 0;
  font-size: 27px;
  font-weight: 760;
  line-height: 1.35;
  letter-spacing: 0;
}

.detail-price {
  margin-top: 14px;
  color: #d2562b;
  font-size: 30px;
  font-weight: 800;
  letter-spacing: 0;
}

.detail-description {
  margin-top: 22px;
  padding-top: 18px;
  border-top: 1px solid var(--campus-line);
}

.detail-description h2 {
  margin: 0 0 8px;
  font-size: 14px;
  font-weight: 700;
}

.detail-description p {
  margin: 0;
  color: #44504b;
  font-size: 14px;
  line-height: 1.75;
  white-space: pre-wrap;
}

.seller-block {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  margin: 18px 0 0;
  padding: 14px;
  background: #f2f5f3;
  border-radius: 8px;
}

.seller-block div {
  min-width: 0;
}

.seller-block dt {
  color: #79847e;
  font-size: 12px;
}

.seller-block dd {
  margin: 4px 0 0;
  overflow: hidden;
  font-size: 14px;
  font-weight: 650;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.detail-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
  margin-top: 24px;
  padding-top: 18px;
  border-top: 1px solid var(--campus-line);
}

.ask-ai {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  height: 40px;
  margin-left: auto;
  padding: 0 14px;
  color: #1d6687;
  background: #eaf3f8;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 650;
}

@media (max-width: 860px) {
  .detail-layout {
    grid-template-columns: 1fr;
    gap: 20px;
  }

  .detail-media {
    position: static;
  }

  .detail-info h1 {
    font-size: 22px;
  }
}
</style>
