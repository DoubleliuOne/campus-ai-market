import http from './http'

export const loginApi = (data) => http.post('/auth/login', data)
export const registerApi = (data) => http.post('/auth/register', data)
export const getMeApi = () => http.get('/auth/me')
export const logoutApi = () => http.post('/auth/logout')

export const getCategoriesApi = () => http.get('/categories')

export const listItemsApi = (params) => http.get('/items', { params })
export const getHotItemsApi = () => http.get('/items/hot')
export const getItemDetailApi = (id) => http.get(`/items/${id}`)
export const createItemApi = (data) => http.post('/items', data)
export const updateItemApi = (id, data) => http.put(`/items/${id}`, data)
export const takeOffItemApi = (id) => http.delete(`/items/${id}`)
export const getMyItemsApi = (params) => http.get('/items/mine', { params })

export const addFavoriteApi = (itemId) => http.post('/favorites', { itemId })
export const removeFavoriteApi = (itemId) => http.delete(`/favorites/${itemId}`)
export const getMyFavoritesApi = (params) => http.get('/favorites', { params })
export const checkFavoriteApi = (itemId) => http.get(`/favorites/check/${itemId}`)

export const createOrderApi = (itemId) => http.post('/orders', { itemId })
export const getMyOrdersApi = (params) => http.get('/orders/my', { params })
export const updateOrderStatusApi = (orderId, status) =>
  http.patch(`/orders/${orderId}/status`, { status })

export const agentChatApi = (data) => http.post('/agent/chat', data)
