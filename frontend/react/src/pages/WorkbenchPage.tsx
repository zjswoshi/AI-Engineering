import { Suspense } from 'react'
import { Link } from 'react-router'
import { visibleSections, workbenchFeatures } from '../app/sections'
import { useSession } from '../features/auth/session'

export default function WorkbenchPage() {
  const { profile } = useSession()
  const shortcuts = visibleSections(profile.role).filter((item) => item.path !== '/')
  return (
    <>
      <div className="page-title"><div><h1>工作台</h1><p className="muted">{profile.username}，欢迎回来</p></div></div>
      <div className="grid gap-4">
        <Suspense fallback={<p role="status">正在加载工作台模块…</p>}>
          {workbenchFeatures.map((Feature, index) => <Feature key={index} />)}
        </Suspense>
        {shortcuts.length > 0 && (
          <section className="panel" aria-labelledby="shortcuts-title">
            <h2 id="shortcuts-title">快捷入口</h2>
            <div className="flex flex-wrap gap-4">
              {shortcuts.map((item) => <Link className="shortcut" key={item.path} to={item.path}>{item.label}</Link>)}
            </div>
          </section>
        )}
      </div>
    </>
  )
}
