import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'

import App from './App.vue'
import router from './router'
import { restoreSession } from './stores/auth'
import './styles/index.css'

restoreSession().finally(() => {
  const app = createApp(App)
  app.use(ElementPlus)
  app.use(router)
  app.mount('#app')
})
