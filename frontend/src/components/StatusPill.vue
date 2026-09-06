<script setup>
import { computed } from 'vue'

import { itemStatusMeta, orderStatusMeta } from '../utils/format'

const props = defineProps({
  status: { type: String, required: true },
  kind: { type: String, default: 'item' },
})

const meta = computed(() =>
  props.kind === 'order' ? orderStatusMeta(props.status) : itemStatusMeta(props.status),
)
</script>

<template>
  <span class="status-pill" :class="meta.className">{{ meta.text }}</span>
</template>
