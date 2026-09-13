import http from './http'

export const loginApi = (data) => http.post('/auth/login', data)
export const registerApi = (data) => http.post('/auth/register', data)
export const getMeApi = () => http.get('/auth/me')
export const logoutApi = () => http.post('/auth/logout')

export const getCategoriesApi = () => http.get('/categories')

export const listItemsApi = (params) => http.get('/items', { params })
export const getHotItemsApi = () => http.get('/items/hot')
export const getItemDetailApi = (id) => http.get(`/items/${id}`)
export const getManagedItemDetailApi = (id) => http.get(`/items/${id}/manage`)
export const createItemApi = (data) => http.post('/items', data)
export const updateItemApi = (id, data) => http.put(`/items/${id}`, data)
export const takeOffItemApi = (id) => http.delete(`/items/${id}`)
export const relistItemApi = (id) => http.patch(`/items/${id}/relist`)
export const getMyItemsApi = (params) => http.get('/items/mine', { params })
export const uploadItemImageApi = (file) => {
  const formData = new FormData()
  formData.append('file', file)
  return http.post('/files/images', formData)
}

export const addFavoriteApi = (itemId) => http.post('/favorites', { itemId })
export const removeFavoriteApi = (itemId) => http.delete(`/favorites/${itemId}`)
export const getMyFavoritesApi = (params) => http.get('/favorites', { params })
export const checkFavoriteApi = (itemId) => http.get(`/favorites/check/${itemId}`)

export const createOrderApi = (itemId) => http.post('/orders', { itemId })
export const getMyOrdersApi = (params) => http.get('/orders/my', { params })
export const updateOrderStatusApi = (orderId, status) =>
  http.patch(`/orders/${orderId}/status`, { status })

export const agentChatApi = (data) => http.post('/agent/chat', data)
export const getConversationsApi = (params) => http.get('/agent/conversations', { params })
export const createConversationApi = (data = {}) => http.post('/agent/conversations', data)
export const getConversationMessagesApi = (id) =>
  http.get(`/agent/conversations/${id}/messages`)
export const sendConversationMessageApi = (id, data) =>
  http.post(`/agent/conversations/${id}/messages`, data)
export const deleteConversationApi = (id) => http.delete(`/agent/conversations/${id}`)
