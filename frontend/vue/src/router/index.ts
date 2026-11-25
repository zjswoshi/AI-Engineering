import { createRouter, createWebHistory } from 'vue-router'
import { hasAccess, returnPath, type Role } from '../features/auth/model'
import { session } from '../features/auth/session'
import AdminLayout from '../layouts/AdminLayout.vue'
import { sections } from './sections'

declare module 'vue-router' {
  interface RouteMeta {
    roles?: readonly Role[]
    requiresAuth?: boolean
    title?: string
  }
}

export const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', component: () => import('../views/LoginView.vue'), meta: { title: '登录' } },
    {
      path: '/', component: AdminLayout, meta: { requiresAuth: true },
      children: [
        ...sections.map(({ path, component, roles, label }) => ({
          path: path.slice(1), component: component!, meta: { roles, title: label },
        })),
        { path: '403', component: () => import('../views/StatusView.vue'), props: { forbidden: true }, meta: { title: '无权限' } },
        { path: ':pathMatch(.*)*', component: () => import('../views/StatusView.vue'), meta: { title: '页面不存在' } },
      ],
    },
  ],
  scrollBehavior: () => ({ top: 0 }),
})

export function allowedReturn(value: unknown, role: Role) {
  const target = returnPath(value)
  const resolved = router.resolve(target)
  return resolved.meta.requiresAuth && hasAccess(role, resolved.meta.roles) ? target : '/'
}

router.beforeEach((to) => {
  const current = session.value
  if (to.path === '/login' && current) return allowedReturn(to.query.returnTo, current.profile.role)
  if (to.meta.requiresAuth && !current) return { path: '/login', query: { returnTo: to.fullPath } }
  if (current && !hasAccess(current.profile.role, to.meta.roles)) return '/403'
})

router.afterEach((to) => {
  document.title = `${to.meta.title ?? '首页'} · AI 检测工作台`
})
