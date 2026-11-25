import { useEffect, useState, type FormEvent } from 'react'
import { Alert, Button, Select } from 'antd'
import type { AnalystRole } from '../auth/model'
import { useSession } from '../auth/session'
import { requestError } from '../../lib/http'
import { loadAnalyst, saveAnalyst } from './api'

export default function RolesPanel() {
  const { request } = useSession()
  const [role, setRole] = useState<AnalystRole>()
  const [savedRole, setSavedRole] = useState<AnalystRole>()
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [busy, setBusy] = useState(true)
  const [attempt, setAttempt] = useState(0)

  useEffect(() => {
    let active = true
    void loadAnalyst(request).then((profile) => {
      if (active) { setRole(profile.role); setSavedRole(profile.role) }
    }, (cause: unknown) => {
      if (active) setError(requestError(cause))
    }).finally(() => { if (active) setBusy(false) })
    return () => { active = false }
  }, [request, attempt])

  function retry() {
    setError('')
    setBusy(true)
    setAttempt((value) => value + 1)
  }

  async function save(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!role || busy) return
    setBusy(true)
    setError('')
    setNotice('')
    try {
      const profile = await saveAnalyst(request, role)
      setSavedRole(profile.role)
      setRole(profile.role)
      setNotice('权限已更新。')
    } catch (cause) {
      setError(requestError(cause))
    } finally {
      setBusy(false)
    }
  }

  return (
    <section className="panel max-w-xl" aria-labelledby="role-form-title">
      <h2 id="role-form-title">analyst</h2>
      <form onSubmit={save}>
        <div className="field">
          <label htmlFor="analyst-role">角色</label>
          <Select<AnalystRole> id="analyst-role" aria-label="角色" value={role} onChange={setRole} disabled={busy || !savedRole} placeholder="选择角色" options={[
            { label: '查看者', value: 'VIEWER' }, { label: '操作员', value: 'OPERATOR' },
          ]} />
        </div>
        <div className="flex gap-2">
          <Button type="primary" htmlType="submit" disabled={!role || role === savedRole} loading={busy}>保存权限</Button>
          {error && !savedRole && <Button onClick={retry}>重试</Button>}
        </div>
      </form>
      {error && <Alert className="mt-4" title={error} type="error" showIcon />}
      <p className="status-space" role="status">{notice}</p>
    </section>
  )
}
