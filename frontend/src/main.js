import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import * as ElementPlusComponents from 'element-plus'
import 'element-plus/dist/index.css'

import App from './App.vue'
import router from './router'
import './styles/global.css'

const app = createApp(App)

// 兼容某些环境下 app.use(ElementPlus) 未正确注入组件的情况：
// 显式注册所有 El* 组件，确保 <el-*> 标签可以被正常解析。
for (const [name, component] of Object.entries(ElementPlusComponents)) {
  if (name.startsWith('El') && component) {
    app.component(name, component)
  }
}

app.use(router)
app.use(ElementPlus)

app.mount('#app')
