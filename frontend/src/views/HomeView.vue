<script setup>
import { nextTick, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  ArrowRight,
  Bot,
  FilterX,
  Search,
  ShoppingBasket,
  Sparkles,
  TrendingUp,
} from 'lucide-vue-next'

import { getCategoriesApi, listItemsApi } from '../api'
import EmptyState from '../components/EmptyState.vue'
import ProductCard from '../components/ProductCard.vue'

const route = useRoute()
const router = useRouter()

const DEFAULT_CATEGORIES = [
  { id: 1, name: '数码产品' },
  { id: 2, name: '教材' },
  { id: 3, name: '宿舍用品' },
  { id: 4, name: '生活用品' },
  { id: 5, name: '其他' },
]

const filters = reactive({
  keyword: '',
  categoryId: null,
  minPrice: null,
  maxPrice: null,
  sort: 'latest',
})
const applied = reactive({ ...filters })

const categories = ref(DEFAULT_CATEGORIES)
const items = ref([])
const total = ref(0)
const page = ref(1)
const size = 12
const loading = ref(false)
const loaded = ref(false)
const loadError = ref('')
const searchInput = ref()
const resultSection = ref()
let loadRequestId = 0

const sortOptions = [
  { label: '最新发布', value: 'latest' },
  { label: '价格从低到高', value: 'priceAsc' },
  { label: '价格从高到低', value: 'priceDesc' },
]

async function loadCategories() {
  try {
    const data = await getCategoriesApi()
    if (Array.isArray(data) && data.length) {
      categories.value = data
    }
  } catch (error) {
    if (error.status !== 401) {
      loadError.value = error.message
    }
  }
}

async function loadItems() {
  const requestId = ++loadRequestId
  loading.value = true
  loadError.value = ''
  try {
    const data = await listItemsApi({
      keyword: applied.keyword || undefined,
      categoryId: applied.categoryId || undefined,
      minPrice: applied.minPrice ?? undefined,
      maxPrice: applied.maxPrice ?? undefined,
      sort: applied.sort,
      page: page.value,
      size,
    })
    if (requestId === loadRequestId) {
      items.value = data.records || []
      total.value = data.total || 0
      loaded.value = true
    }
  } catch (error) {
    if (requestId === loadRequestId) {
      loadError.value = error.message || '商品加载失败'
    }
  } finally {
    if (requestId === loadRequestId) {
      loading.value = false
    }
  }
}

function search() {
  Object.assign(applied, filters)
  page.value = 1
  loadItems()
}

function sortChanged() {
  applied.sort = filters.sort
  page.value = 1
  loadItems()
}

function resetFilters() {
  filters.keyword = ''
  filters.categoryId = null
  filters.minPrice = null
  filters.maxPrice = null
  filters.sort = 'latest'
  search()
}

function toggleCategory(id) {
  filters.categoryId = filters.categoryId === id ? null : id
  search()
}

function changePage(next) {
  page.value = next
  loadItems()
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

async function focusSearch() {
  await nextTick()
  searchInput.value?.focus()
  resultSection.value?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

function startAiSearch() {
  router.push({
    name: 'assistant',
    query: filters.keyword ? { prompt: filters.keyword } : {},
  })
}

watch(
  () => route.query.focus,
  (value) => {
    if (value === 'search') {
      focusSearch()
      router.replace({ query: {} })
    }
  },
)

onMounted(() => {
  loadCategories()
  loadItems()
  if (route.query.focus === 'search') {
    focusSearch()
  }
})
</script>

<template>
  <div class="page-shell home-page">
    <section class="market-hero">
      <div class="hero-copy">
        <span class="hero-kicker">
          <Sparkles :size="14" />
          真实商品数据 + AI 智能检索
        </span>
        <h1>让每一件校园闲置，找到真正需要它的人。</h1>
        <p>搜索同学发布的数码、教材和生活用品，也可以直接告诉 AI 你的预算与需求。</p>
      </div>
      <button type="button" class="hero-ai" @click="startAiSearch">
        <span class="hero-ai-icon"><Bot :size="21" /></span>
        <span>
          <strong>问问 CampusAI</strong>
          <small>用自然语言找商品、查规则、看订单</small>
        </span>
        <ArrowRight :size="17" />
      </button>
    </section>

    <section class="toolbar search-toolbar" aria-label="商品搜索">
      <div class="keyword-row">
        <el-input
          ref="searchInput"
          v-model="filters.keyword"
          size="large"
          clearable
          placeholder="搜索商品名称、品牌或用途"
          @keyup.enter="search"
          @clear="search"
        >
          <template #prefix><Search :size="17" /></template>
          <template #append>
            <el-button type="primary" :icon="Search" @click="search">搜索</el-button>
          </template>
        </el-input>
      </div>
      <div class="filter-row">
        <div class="filter-label">
          <TrendingUp :size="15" />
          价格区间
        </div>
        <div class="price-group">
          <el-input-number
            v-model="filters.minPrice"
            :min="0"
            :precision="2"
            :step="50"
            controls-position="right"
            placeholder="最低价"
          />
          <span class="price-sep">至</span>
          <el-input-number
            v-model="filters.maxPrice"
            :min="0"
            :precision="2"
            :step="50"
            controls-position="right"
            placeholder="最高价"
          />
        </div>
        <div class="filter-actions">
          <el-button class="reset-button" plain :icon="FilterX" @click="resetFilters">
            重置
          </el-button>
          <el-button type="primary" plain :icon="Search" @click="search">应用筛选</el-button>
        </div>
      </div>
      <div class="category-row">
        <button
          type="button"
          class="category-chip"
          :class="{ 'is-active': filters.categoryId === null }"
          @click="toggleCategory(null)"
        >
          全部
        </button>
        <button
          v-for="category in categories"
          :key="category.id"
          type="button"
          class="category-chip"
          :class="{ 'is-active': filters.categoryId === category.id }"
          @click="toggleCategory(category.id)"
        >
          {{ category.name }}
        </button>
      </div>
    </section>

    <div ref="resultSection" class="result-bar">
      <div>
        <strong>在售商品</strong>
        <span>共 {{ total }} 件</span>
        <span v-if="applied.keyword" class="result-keyword">“{{ applied.keyword }}”</span>
      </div>
      <el-select
        v-model="filters.sort"
        class="sort-select"
        aria-label="商品排序"
        @change="sortChanged"
      >
        <el-option
          v-for="option in sortOptions"
          :key="option.value"
          :label="option.label"
          :value="option.value"
        />
      </el-select>
    </div>

    <div v-loading="loading" class="market-content">
      <transition-group v-if="items.length" name="product-list" tag="div" class="product-grid">
        <ProductCard v-for="item in items" :key="item.id" :item="item" />
      </transition-group>
      <EmptyState
        v-else-if="!loading && !loadError"
        title="没有找到符合条件的商品"
        description="换一个关键词或放宽价格范围，也可以让 AI 帮你重新描述需求"
      >
        <el-button type="primary" plain @click="resetFilters">查看全部商品</el-button>
        <el-button type="primary" :icon="Bot" @click="startAiSearch">让 AI 帮我找</el-button>
      </EmptyState>
      <EmptyState
        v-else-if="!loading && loadError"
        title="商品加载失败"
        :description="loadError"
      >
        <el-button type="primary" @click="loadItems">重新加载</el-button>
      </EmptyState>
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
.home-page {
  padding-top: 22px;
}

.market-hero {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 360px;
  gap: 26px;
  align-items: center;
  margin-bottom: 18px;
  padding: 30px 32px;
  color: #f4fbf8;
  background: #155b49;
  border-radius: var(--campus-radius-md);
  box-shadow: 0 16px 42px rgba(21, 91, 73, 0.16);
}

.hero-kicker {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: #b8e3d4;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.06em;
}

.hero-copy h1 {
  max-width: 650px;
  margin: 12px 0 0;
  color: #fff;
  font-size: clamp(25px, 3vw, 36px);
  font-weight: 780;
  line-height: 1.35;
  letter-spacing: 0;
}

.hero-copy p {
  max-width: 670px;
  margin: 12px 0 0;
  color: rgba(244, 251, 248, 0.72);
  font-size: 14px;
  line-height: 1.7;
}

.hero-ai {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  padding: 15px;
  color: #143c31;
  background: #f7fbf9;
  border: 1px solid rgba(255, 255, 255, 0.35);
  border-radius: var(--campus-radius-md);
  text-align: left;
  cursor: pointer;
  transition:
    transform 0.16s ease,
    box-shadow 0.16s ease;
}

.hero-ai:hover {
  box-shadow: 0 10px 26px rgba(10, 48, 37, 0.18);
  transform: translateY(-2px);
}

.hero-ai-icon {
  display: grid;
  flex: 0 0 42px;
  place-items: center;
  width: 42px;
  height: 42px;
  color: #fff;
  background: var(--campus-blue);
  border-radius: var(--campus-radius-sm);
}

.hero-ai > span:nth-child(2) {
  flex: 1;
  min-width: 0;
}

.hero-ai strong,
.hero-ai small {
  display: block;
}

.hero-ai strong {
  font-size: 14px;
}

.hero-ai small {
  margin-top: 4px;
  color: #65726c;
  font-size: 11px;
  line-height: 1.4;
}

.search-toolbar {
  padding: 18px;
}

.keyword-row {
  display: flex;
  gap: 10px;
}

.keyword-row :deep(.el-input-group__append) {
  padding: 0;
  background: var(--campus-green);
  border-color: var(--campus-green);
}

.keyword-row :deep(.el-input-group__append .el-button) {
  height: 38px;
  padding: 0 22px;
  color: #fff;
}

.filter-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 12px;
}

.filter-label {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: var(--campus-text);
  font-size: 13px;
  font-weight: 650;
}

.price-group {
  display: flex;
  align-items: center;
  gap: 8px;
}

.price-group :deep(.el-input-number) {
  width: 140px;
}

.price-sep {
  color: #8a948f;
  font-size: 12px;
}

.filter-actions {
  display: flex;
  gap: 8px;
  margin-left: auto;
}

.reset-button {
  color: var(--campus-text);
}

.category-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 14px;
  padding-top: 14px;
  border-top: 1px solid #edf0ee;
}

.category-chip {
  min-width: 68px;
  height: 32px;
  padding: 0 12px;
  color: #52605a;
  background: #f1f4f2;
  border: 1px solid transparent;
  border-radius: 6px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
}

.category-chip:hover {
  background: var(--el-color-primary-light-9);
}

.category-chip.is-active {
  color: var(--campus-green-dark);
  background: var(--el-color-primary-light-8);
  border-color: #9ac9b8;
}

.result-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  min-height: 58px;
  color: #6a7670;
  font-size: 13px;
}

.result-bar > div {
  display: flex;
  align-items: center;
  gap: 9px;
}

.result-bar strong {
  color: var(--campus-ink);
  font-size: 17px;
}

.sort-select {
  width: 150px;
}

.result-keyword {
  padding: 3px 8px;
  color: var(--campus-green-dark);
  background: var(--el-color-primary-light-9);
  border-radius: 4px;
}

.market-content {
  min-height: 320px;
}

.product-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(238px, 1fr));
  gap: 18px;
}

.product-list-enter-active,
.product-list-leave-active {
  transition:
    opacity 0.18s ease,
    transform 0.18s ease;
}

.product-list-enter-from,
.product-list-leave-to {
  opacity: 0;
  transform: translateY(6px);
}

.pagination-row {
  display: flex;
  justify-content: center;
  margin-top: 28px;
}

@media (max-width: 920px) {
  .market-hero {
    grid-template-columns: 1fr;
    padding: 26px;
  }

  .hero-ai {
    max-width: 480px;
  }
}

@media (max-width: 720px) {
  .market-hero {
    gap: 20px;
    margin-bottom: 14px;
    padding: 22px 18px;
  }

  .hero-copy h1 {
    font-size: 25px;
  }

  .keyword-row {
    flex-direction: column;
  }

  .filter-row {
    justify-content: space-between;
    align-items: stretch;
    flex-direction: column;
  }

  .price-group {
    width: 100%;
  }

  .price-group :deep(.el-input-number) {
    flex: 1;
    width: auto;
  }

  .filter-actions {
    width: 100%;
    margin-left: 0;
  }

  .filter-actions :deep(.el-button) {
    flex: 1;
  }

  .category-row {
    flex-wrap: nowrap;
    margin-right: -14px;
    padding-right: 14px;
    overflow-x: auto;
  }

  .category-chip {
    flex-shrink: 0;
  }

  .result-bar {
    align-items: flex-start;
    flex-direction: column;
    padding: 14px 0;
  }

  .sort-select {
    width: 100%;
  }
}
</style>
