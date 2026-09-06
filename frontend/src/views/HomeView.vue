<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Bot, FilterX, Search } from 'lucide-vue-next'

import { getCategoriesApi, listItemsApi } from '../api'
import EmptyState from '../components/EmptyState.vue'
import ProductCard from '../components/ProductCard.vue'

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
  loading.value = true
  loadError.value = ''
  try {
    const data = await listItemsApi({
      keyword: applied.keyword || undefined,
      categoryId: applied.categoryId || undefined,
      minPrice: applied.minPrice ?? undefined,
      maxPrice: applied.maxPrice ?? undefined,
      page: page.value,
      size,
    })
    items.value = data.records || []
    total.value = data.total || 0
    loaded.value = true
  } catch (error) {
    loadError.value = error.message || '商品加载失败'
  } finally {
    loading.value = false
  }
}

function search() {
  Object.assign(applied, filters)
  page.value = 1
  loadItems()
}

function resetFilters() {
  filters.keyword = ''
  filters.categoryId = null
  filters.minPrice = null
  filters.maxPrice = null
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

onMounted(() => {
  loadCategories()
  loadItems()
})
</script>

<template>
  <div class="page-shell home-page">
    <section class="market-head">
      <div class="market-heading">
        <h1 class="page-title">校园二手集市</h1>
        <p>从同学手中找到需要的数码、教材与生活用品</p>
      </div>
      <RouterLink to="/assistant" class="ai-entry">
        <Bot :size="16" />
        <span>AI 助手</span>
      </RouterLink>
    </section>

    <section class="toolbar search-toolbar" aria-label="商品搜索">
      <div class="keyword-row">
        <el-input
          v-model="filters.keyword"
          size="large"
          clearable
          placeholder="搜索商品关键词"
          @keyup.enter="search"
          @clear="search"
        >
          <template #prefix><Search :size="17" /></template>
        </el-input>
        <el-button type="primary" size="large" :icon="Search" @click="search">
          搜索
        </el-button>
      </div>
      <div class="filter-row">
        <div class="price-group">
          <el-input-number
            v-model="filters.minPrice"
            :min="0"
            :precision="2"
            :step="50"
            controls-position="right"
            placeholder="最低价"
          />
          <span class="price-sep">-</span>
          <el-input-number
            v-model="filters.maxPrice"
            :min="0"
            :precision="2"
            :step="50"
            controls-position="right"
            placeholder="最高价"
          />
        </div>
        <el-button class="reset-button" text @click="resetFilters">
          <FilterX :size="15" />
          重置
        </el-button>
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

    <div class="result-bar">
      <span>共 {{ total }} 件在售商品</span>
      <span v-if="applied.keyword" class="result-keyword">“{{ applied.keyword }}”</span>
    </div>

    <div v-loading="loading" class="market-content">
      <div v-if="items.length" class="product-grid">
        <ProductCard v-for="item in items" :key="item.id" :item="item" />
      </div>
      <EmptyState
        v-else-if="!loading && !loadError"
        title="没有找到符合条件的商品"
        description="换一个关键词或放宽价格范围再试试"
      >
        <el-button type="primary" plain @click="resetFilters">查看全部商品</el-button>
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
  padding-top: 28px;
}

.market-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}

.market-heading p {
  margin: 7px 0 0;
  color: var(--campus-text);
  font-size: 14px;
}

.ai-entry {
  display: inline-flex;
  flex-shrink: 0;
  align-items: center;
  gap: 7px;
  height: 38px;
  padding: 0 13px;
  color: #fff;
  background: #256e8f;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 650;
}

.search-toolbar {
  padding: 16px;
}

.keyword-row {
  display: flex;
  gap: 10px;
}

.filter-row {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 14px;
  margin-top: 12px;
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
  gap: 10px;
  min-height: 46px;
  color: #6a7670;
  font-size: 13px;
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
  grid-template-columns: repeat(auto-fill, minmax(232px, 1fr));
  gap: 16px;
}

.pagination-row {
  display: flex;
  justify-content: center;
  margin-top: 28px;
}

@media (max-width: 720px) {
  .market-head {
    align-items: flex-start;
    flex-direction: column;
  }

  .keyword-row {
    flex-direction: column;
  }

  .filter-row {
    justify-content: space-between;
    align-items: flex-end;
  }

  .price-group {
    width: 100%;
  }

  .price-group :deep(.el-input-number) {
    flex: 1;
    width: auto;
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
}
</style>
