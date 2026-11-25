import { lazy, type ComponentType } from 'react'
import { matchPath } from 'react-router'
import { hasAccess, returnPath, type Role } from '../features/auth/model'

type Section = { path: string; label: string; short: string; roles?: readonly Role[]; Page: ComponentType }
type FeatureModule = { section: Section; Workbench: ComponentType }

// Optional feature registrations live here so a copy can drop the entire example from the shell.
const modules: FeatureModule[] = [{
  section: { path: '/detections', label: '检测任务', short: '检测', Page: lazy(() => import('../pages/DetectionsPage')) },
  Workbench: lazy(() => import('../features/detection/RecentDetections')),
}]

export const workbenchFeatures = modules.map((feature) => feature.Workbench)
export const sections: Section[] = [
  { path: '/', label: '工作台', short: '首页', Page: lazy(() => import('../pages/WorkbenchPage')) },
  ...modules.map((feature) => feature.section),
  { path: '/admin/roles', label: '角色管理', short: '角色', roles: ['ADMIN'], Page: lazy(() => import('../pages/RolesPage')) },
]

export const visibleSections = (role: Role) => sections.filter((section) => hasAccess(role, section.roles))

export function allowedReturn(value: unknown, role: Role) {
  const target = returnPath(value)
  const pathname = target.split(/[?#]/, 1)[0]
  const route = sections.find(({ path }) => matchPath(path, pathname))
  return route && hasAccess(role, route.roles) ? target : '/'
}
