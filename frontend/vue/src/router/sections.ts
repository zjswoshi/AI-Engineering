import { defineAsyncComponent, type Component } from 'vue'
import type { RouteRecordRaw } from 'vue-router'
import { hasAccess, type Role } from '../features/auth/model'

export type Section = { path: string; label: string; short: string; roles?: readonly Role[]; component: RouteRecordRaw['component'] }
type FeatureModule = { section: Section; workbench: Component }

// Optional feature registrations live here so a copy can drop the entire example from the shell.
const modules: FeatureModule[] = [{
  section: { path: '/detections', label: '检测任务', short: '检测', component: () => import('../views/DetectionsView.vue') },
  workbench: defineAsyncComponent(() => import('../features/detection/RecentDetections.vue')),
}]

export const workbenchFeatures = modules.map((feature) => feature.workbench)
export const sections: Section[] = [
  { path: '/', label: '工作台', short: '首页', component: () => import('../views/WorkbenchView.vue') },
  ...modules.map((feature) => feature.section),
  { path: '/admin/roles', label: '角色管理', short: '角色', roles: ['ADMIN'], component: () => import('../views/RolesView.vue') },
]

export const visibleSections = (role: Role) => sections.filter((section) => hasAccess(role, section.roles))
