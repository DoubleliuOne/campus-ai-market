const ITEM_STATUS = {
  ON_SALE: { text: '在售', className: 'status-on-sale' },
  SOLD: { text: '已售出', className: 'status-sold' },
  OFF_SHELF: { text: '已下架', className: 'status-off-shelf' },
}

const ORDER_STATUS = {
  CREATED: { text: '待完成', className: 'status-created' },
  PAID: { text: '已支付', className: 'status-paid' },
  COMPLETED: { text: '已完成', className: 'status-completed' },
  CANCELLED: { text: '已取消', className: 'status-cancelled' },
}

export function formatMoney(value) {
  const number = Number(value)
  if (!Number.isFinite(number)) {
    return '¥0.00'
  }
  return `¥${number.toFixed(2)}`
}

function normalizeDate(value) {
  if (!value) {
    return ''
  }
  return String(value).replace('T', ' ').replace(/\.\d+$/, '')
}

export function formatDate(value) {
  return normalizeDate(value).slice(0, 10)
}

export function formatDateTime(value) {
  const text = normalizeDate(value)
  return text.length > 16 ? text.slice(0, 16) : text
}

export function itemStatusMeta(status) {
  return ITEM_STATUS[status] || { text: status || '未知', className: 'status-muted' }
}

export function orderStatusMeta(status) {
  return ORDER_STATUS[status] || { text: status || '未知', className: 'status-muted' }
}

export function firstImage(item) {
  if (!item) {
    return ''
  }
  if (Array.isArray(item.images)) {
    return item.images.find(Boolean) || ''
  }
  return ''
}

export function categoryName(item) {
  return item?.categoryName || '其他'
}
