import { createRouter, createWebHistory } from 'vue-router'

import MainLayout from '../layouts/MainLayout.vue'
import { authState, isLoggedIn, restoreSession } from '../stores/auth'

const AuthView = () => import('../views/AuthView.vue')
const AssistantView = () => import('../views/AssistantView.vue')
const FavoritesView = () => import('../views/FavoritesView.vue')
const HomeView = () => import('../views/HomeView.vue')
const ItemDetailView = () => import('../views/ItemDetailView.vue')
const ItemFormView = () => import('../views/ItemFormView.vue')
const MyItemsView = () => import('../views/MyItemsView.vue')
const NotFoundView = () => import('../views/NotFoundView.vue')
const OrdersView = () => import('../views/OrdersView.vue')

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: AuthView,
      meta: { guestOnly: true, title: '登录 CampusAI Market' },
    },
    {
      path: '/',
      component: MainLayout,
      children: [
        {
          path: '',
          name: 'home',
          component: HomeView,
          meta: { title: 'CampusAI Market 校园二手集市' },
        },
        {
          path: 'items/:id',
          name: 'item-detail',
          component: ItemDetailView,
          meta: { title: '商品详情' },
        },
        {
          path: 'publish',
          name: 'item-publish',
          component: ItemFormView,
          meta: { requiresAuth: true, title: '发布商品' },
        },
        {
          path: 'items/:id/edit',
          name: 'item-edit',
          component: ItemFormView,
          meta: { requiresAuth: true, title: '编辑商品' },
        },
        {
          path: 'my-items',
          name: 'my-items',
          component: MyItemsView,
          meta: { requiresAuth: true, title: '我的商品' },
        },
        {
          path: 'favorites',
          name: 'favorites',
          component: FavoritesView,
          meta: { requiresAuth: true, title: '我的收藏' },
        },
        {
          path: 'orders',
          name: 'orders',
          component: OrdersView,
          meta: { requiresAuth: true, title: '我的订单' },
        },
        {
          path: 'assistant',
          name: 'assistant',
          component: AssistantView,
          meta: { requiresAuth: true, title: 'AI 助手' },
        },
      ],
    },
    {
      path: '/:pathMatch(.*)*',
      name: 'not-found',
      component: NotFoundView,
      meta: { title: '页面不存在' },
    },
  ],
  scrollBehavior(to, from, savedPosition) {
    if (savedPosition) {
      return savedPosition
    }
    if (to.hash) {
      return { el: to.hash, behavior: 'smooth' }
    }
    return { top: 0 }
  },
})

router.beforeEach(async (to) => {
  await restoreSession()
  if (authState.restoring) {
    await restoreSession()
  }

  if (to.meta.requiresAuth && !isLoggedIn()) {
    return {
      name: 'login',
      query: { redirect: to.fullPath },
    }
  }
  if (to.meta.guestOnly && isLoggedIn()) {
    return { name: 'home' }
  }
  return true
})

router.afterEach((to) => {
  const title = to.meta.title || 'CampusAI Market'
  document.title = title
})

export default router
