import { useEffect, useState, type FormEvent } from 'react'
import { Alert, Button, Input } from 'antd'
import { Navigate, useSearchParams } from 'react-router'
import { allowedReturn } from '../app/sections'
import { authenticate } from '../features/auth/model'
import { useAuth } from '../features/auth/session'
import { requestError } from '../lib/http'

export default function LoginPage() {
  const { session, setSession } = useAuth()
  const [search] = useSearchParams()
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  useEffect(() => { document.title = '登录 · AI 检测工作台' }, [])

  async function login(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (busy || !username.trim() || !password) return
    setError('')
    setBusy(true)
    try {
      setSession(await authenticate(username, password))
    } catch (cause) {
      setError(requestError(cause))
    } finally {
      setPassword('')
      setBusy(false)
    }
  }

  if (session) return <Navigate replace to={allowedReturn(search.get('returnTo'), session.profile.role)} />

  return (
    <main className="login-shell">
      <section className="panel login-panel" aria-labelledby="login-title">
        <div className="brand"><span className="brand-mark" aria-hidden="true">AI</span>检测工作台</div>
        <h1 id="login-title">登录</h1>
        <form className="mt-6" onSubmit={login}>
          <div className="field"><label htmlFor="username">账号</label><Input id="username" value={username} onChange={(event) => setUsername(event.target.value)} autoComplete="username" maxLength={40} /></div>
          <div className="field"><label htmlFor="password">密码</label><Input.Password id="password" value={password} onChange={(event) => setPassword(event.target.value)} autoComplete="current-password" maxLength={120} /></div>
          {error && <Alert title={error} type="error" showIcon className="mb-4" />}
          <Button type="primary" htmlType="submit" block loading={busy} disabled={!username.trim() || !password}>登录</Button>
        </form>
      </section>
    </main>
  )
}
