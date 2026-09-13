import { createApp } from 'vue'
import ElButton from 'element-plus/es/components/button/index.mjs'
import ElDropdown from 'element-plus/es/components/dropdown/index.mjs'
import ElForm from 'element-plus/es/components/form/index.mjs'
import ElInput from 'element-plus/es/components/input/index.mjs'
import ElInputNumber from 'element-plus/es/components/input-number/index.mjs'
import ElLoading from 'element-plus/es/components/loading/index.mjs'
import ElPagination from 'element-plus/es/components/pagination/index.mjs'
import ElSelect from 'element-plus/es/components/select/index.mjs'
import ElTabs from 'element-plus/es/components/tabs/index.mjs'
import 'element-plus/dist/index.css'

import App from './App.vue'
import router from './router'
import { restoreSession } from './stores/auth'
import './styles/index.css'

restoreSession().finally(() => {
  const app = createApp(App)
  ;[
    ElButton,
    ElDropdown,
    ElForm,
    ElInput,
    ElInputNumber,
    ElPagination,
    ElSelect,
    ElTabs,
  ].forEach((component) => app.use(component))
  app.use(ElLoading)
  app.use(router)
  app.mount('#app')
})
