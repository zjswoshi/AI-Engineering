import { useEffect, useRef, useState } from 'react'
import { Button, Tag } from 'antd'
import { NavLink, Outlet, useLocation, useNavigate } from 'react-router'
import { sections, visibleSections } from '../app/sections'
import { roleLabels } from '../features/auth/model'
import { useAuth, useSession } from '../features/auth/session'

export default function AdminLayout() {
  const { profile } = useSession()
  const { setSession } = useAuth()
  const navigate = useNavigate()
  const { pathname } = useLocation()
  const main = useRef<HTMLElement>(null)
  const [collapsed, setCollapsed] = useState(false)

  useEffect(() => {
    const title = sections.find((section) => section.path === pathname)?.label
    document.title = `${title ?? (pathname === '/403' ? '无权限' : '页面不存在')} · AI 检测工作台`
  }, [pathname])

  function logout() {
    setSession(null)
    void navigate('/login', { replace: true })
  }

  return (
    <div className="shell" data-collapsed={collapsed}>
      <a className="skip-link" href="#main-content" onClick={(event) => { event.preventDefault(); main.current?.focus() }}>跳到主内容</a>
      <aside className="side">
        <div className="brand"><span className="brand-mark" aria-hidden="true">AI</span>{!collapsed && <span>检测工作台</span>}</div>
        <nav className="nav" aria-label="主导航">
          {visibleSections(profile.role).map((item) => (
            <NavLink key={item.path} end to={item.path} aria-label={item.label} title={collapsed ? item.label : undefined}>
              {collapsed ? item.short : item.label}
            </NavLink>
          ))}
        </nav>
      </aside>
      <div className="min-w-0">
        <header className="topbar">
          <Button size="small" aria-expanded={!collapsed} onClick={() => setCollapsed(!collapsed)}>{collapsed ? '展开导航' : '折叠导航'}</Button>
          <div className="flex flex-wrap items-center gap-3">
            <span>{profile.username}</span><Tag>{roleLabels[profile.role]}</Tag><Button size="small" onClick={logout}>退出</Button>
          </div>
        </header>
        <main id="main-content" className="content" tabIndex={-1} ref={main}><Outlet /></main>
      </div>
    </div>
  )
}
