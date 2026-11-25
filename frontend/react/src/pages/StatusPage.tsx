import { Link } from 'react-router'

export default function StatusPage({ forbidden }: { forbidden?: boolean }) {
  return (
    <section className="panel" aria-labelledby="status-title">
      <h1 id="status-title">{forbidden ? '403 无权限' : '404 页面不存在'}</h1>
      <p className="muted">{forbidden ? '当前账号无权打开此页面。' : '请通过侧栏进入需要的功能。'}</p>
      <Link className="shortcut" to="/">返回工作台</Link>
    </section>
  )
}
