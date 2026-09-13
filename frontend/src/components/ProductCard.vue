<script setup>
import { computed } from 'vue'
import { RouterLink } from 'vue-router'
import { Clock3, UserRound } from 'lucide-vue-next'

import ItemMedia from './ItemMedia.vue'
import StatusPill from './StatusPill.vue'
import {
  categoryName,
  firstImage,
  formatDate,
  formatMoney,
} from '../utils/format'

const props = defineProps({
  item: { type: Object, required: true },
})

const canOpen = computed(() => props.item.status === 'ON_SALE')
const linkTarget = computed(() => (canOpen.value ? RouterLink : 'article'))
const linkTo = computed(() => ({
  name: 'item-detail',
  params: { id: props.item.id },
}))
</script>

<template>
  <component
    :is="linkTarget"
    :to="canOpen ? linkTo : undefined"
    class="product-card"
    :class="{ 'is-unavailable': !canOpen }"
  >
    <div class="card-media">
      <ItemMedia
        :src="firstImage(item)"
        :alt="item.title"
        :category="categoryName(item)"
        :muted="!canOpen"
      />
      <div v-if="!canOpen" class="card-status">
        <StatusPill :status="item.status" />
      </div>
    </div>
    <div class="card-body">
      <div class="card-topline">
        <span class="card-category">{{ item.categoryName || '校园闲置' }}</span>
        <StatusPill :status="item.status" />
      </div>
      <h3 class="card-title">{{ item.title }}</h3>
      <p v-if="item.description" class="card-description">{{ item.description }}</p>
      <div class="card-meta">
        <span class="card-price">{{ formatMoney(item.price) }}</span>
      </div>
      <div class="card-foot">
        <span class="card-seller">
          <UserRound :size="13" />
          {{ item.sellerUsername || '校园用户' }}
        </span>
        <span class="card-time">
          <Clock3 :size="13" />
          {{ formatDate(item.createTime) }}
        </span>
      </div>
    </div>
  </component>
</template>

<style scoped>
.product-card {
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: var(--campus-surface);
  border: 1px solid var(--campus-line);
  border-radius: var(--campus-radius-md);
  box-shadow: var(--campus-shadow-sm);
  transition:
    transform 0.16s ease,
    box-shadow 0.16s ease,
    border-color 0.16s ease;
}

.product-card:hover {
  border-color: #b9d4c8;
  box-shadow: var(--campus-shadow-md);
  transform: translateY(-3px);
}

.product-card.is-unavailable {
  cursor: default;
}

.product-card.is-unavailable:hover {
  border-color: var(--campus-line);
  box-shadow: none;
  transform: none;
}

.card-media {
  position: relative;
}

.card-status {
  position: absolute;
  top: 10px;
  left: 10px;
  z-index: 1;
}

.card-body {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 8px;
  min-width: 0;
  padding: 13px 14px 14px;
}

.card-topline {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.card-category {
  align-self: flex-start;
  max-width: 100%;
  overflow: hidden;
  color: #64736c;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-title {
  display: -webkit-box;
  min-height: 44px;
  margin: 0;
  overflow: hidden;
  color: var(--campus-ink);
  font-size: 15px;
  font-weight: 650;
  line-height: 1.45;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.card-description {
  display: -webkit-box;
  margin: 0;
  overflow: hidden;
  color: var(--campus-text);
  font-size: 12px;
  line-height: 1.5;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 1;
}

.card-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-top: auto;
  padding-top: 7px;
}

.card-price {
  color: #d2562b;
  font-size: 18px;
  font-weight: 750;
  letter-spacing: 0;
}

.card-seller {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  min-width: 0;
  overflow: hidden;
  color: #75817c;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding-top: 9px;
  border-top: 1px solid #edf0ee;
}

.card-time {
  display: inline-flex;
  flex-shrink: 0;
  align-items: center;
  gap: 4px;
  color: #8a948f;
  font-size: 11px;
}
</style>
