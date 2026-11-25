import { Suspense } from 'react'
import { ConfigProvider } from 'antd'
import zhCN from 'antd/locale/zh_CN'
import { BrowserRouter, Navigate, Route, Routes, useLocation } from 'react-router'
import { hasAccess, type Role } from '../features/auth/model'
import { AuthProvider, useAuth } from '../features/auth/session'
import AdminLayout from '../layouts/AdminLayout'
import LoginPage from '../pages/LoginPage'
import StatusPage from '../pages/StatusPage'
import { sections } from './sections'

function RouteGuard({ roles, children }: { roles?: readonly Role[]; children: React.ReactNode }) {
  const { session } = useAuth()
  const location = useLocation()
  if (!session) {
    const original = location.pathname + location.search + location.hash
    return <Navigate replace to={`/login?${new URLSearchParams({ returnTo: original })}`} />
  }
  return hasAccess(session.profile.role, roles) ? children : <Navigate replace to="/403" />
}

export default function App() {
  return (
    <ConfigProvider locale={zhCN} button={{ autoInsertSpace: false }} theme={{ token: { colorPrimary: '#174b9b', borderRadius: 8 } }}>
      <AuthProvider>
        <BrowserRouter>
          <Suspense fallback={<p className="content" role="status">正在加载页面…</p>}>
            <Routes>
              <Route path="/login" element={<LoginPage />} />
              <Route element={<RouteGuard><AdminLayout /></RouteGuard>}>
                {sections.map(({ path, Page, roles }) => (
                  <Route key={path} path={path} element={<RouteGuard roles={roles}><Page /></RouteGuard>} />
                ))}
                <Route path="/403" element={<StatusPage forbidden />} />
                <Route path="*" element={<StatusPage />} />
              </Route>
            </Routes>
          </Suspense>
        </BrowserRouter>
      </AuthProvider>
    </ConfigProvider>
  )
}
