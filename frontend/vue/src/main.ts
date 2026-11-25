import { createApp } from 'vue'
import { ElAlert, ElButton, ElDrawer, ElInput, ElOption, ElSelect, ElTable, ElTableColumn, ElTag } from 'element-plus'
import 'element-plus/dist/index.css'
import App from './App.vue'
import { router } from './router'
import './styles/base.css'

const app = createApp(App)
for (const component of [ElAlert, ElButton, ElDrawer, ElInput, ElOption, ElSelect, ElTable, ElTableColumn, ElTag]) {
  app.use(component)
}
app.use(router).mount('#app')
