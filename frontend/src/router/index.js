import { createRouter, createWebHistory } from 'vue-router'

import MainLayout from '../layouts/MainLayout.vue'
import { authState, isLoggedIn, restoreSession } from '../stores/auth'
import AssistantView from '../views/AssistantView.vue'
import AuthView from '../views/AuthView.vue'
import FavoritesView from '../views/FavoritesView.vue'
import HomeView from '../views/HomeView.vue'
import ItemDetailView from '../views/ItemDetailView.vue'
import ItemFormView from '../views/ItemFormView.vue'
import MyItemsView from '../views/MyItemsView.vue'
import NotFoundView from '../views/NotFoundView.vue'
import OrdersView from '../views/OrdersView.vue'

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
