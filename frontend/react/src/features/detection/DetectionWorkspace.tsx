import { useEffect, useState, type FormEvent } from 'react'
import { Alert, Button, Drawer, Input, Table, Tag } from 'antd'
import { useSearchParams } from 'react-router'
import { requestError } from '../../lib/http'
import { hasAccess } from '../auth/model'
import { useSession } from '../auth/session'
import { formatDate, listDetections, submitDetection, submitRoles, type Detection } from './api'

export default function DetectionWorkspace() {
  const { request, profile } = useSession()
  const [search, setSearch] = useSearchParams()
  const [rows, setRows] = useState<Detection[]>([])
  const [busy, setBusy] = useState(true)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [sample, setSample] = useState('')
  const [selected, setSelected] = useState<Detection>()
  const [drawerOpen, setDrawerOpen] = useState(false)
  const canSubmit = hasAccess(profile.role, submitRoles)
  const creating = canSubmit && search.get('new') === '1'
  const result = selected && (rows.find((row) => row.id === selected.id) ?? selected)

  useEffect(() => {
    let active = true
    void listDetections(request).then((jobs) => {
      if (active) setRows(jobs)
    }, (cause: unknown) => { if (active) setError(requestError(cause)) })
      .finally(() => { if (active) setBusy(false) })
    return () => { active = false }
  }, [request])

  function showForm(show: boolean) {
    const query = new URLSearchParams(search)
    if (show) query.set('new', '1')
    else query.delete('new')
    setSearch(query, { replace: true })
  }

  function openResult(job: Detection) {
    setSelected(job)
    setDrawerOpen(true)
  }

  async function refresh() {
    if (busy) return
    setBusy(true)
    setError('')
    try {
      setRows(await listDetections(request))
    } catch (cause) {
      setError(requestError(cause))
    } finally {
      setBusy(false)
    }
  }

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!canSubmit || busy || !sample.trim()) return
    setBusy(true)
    setError('')
    setNotice('')
    try {
      const job = await submitDetection(request, sample.trim())
      setRows((current) => [job, ...current].slice(0, 20))
      setSample('')
      setNotice(job.status === 'COMPLETED' ? '检测已完成。' : '任务已受理，可刷新查看状态。')
      showForm(false)
      openResult(job)
    } catch (cause) {
      setError(requestError(cause))
    } finally {
      setBusy(false)
    }
  }

  return (
    <>
      <div className="page-title">
        <div><h1>检测任务</h1><p className="muted">查看当前账号可访问的检测记录。</p></div>
        <div className="flex flex-wrap gap-2">
          <Button loading={busy} onClick={refresh}>刷新</Button>
          {canSubmit && !creating && <Button type="primary" onClick={() => showForm(true)}>新建检测任务</Button>}
        </div>
      </div>
      <div className="grid gap-4">
        {creating && (
          <section className="panel" aria-labelledby="sample-form-title">
            <h2 id="sample-form-title">提交样本检测</h2>
            <form className="max-w-lg" onSubmit={submit}>
              <div className="field"><label htmlFor="sample">样本编号</label><Input id="sample" value={sample} maxLength={120} placeholder="例如 sample-001" onChange={(event) => setSample(event.target.value)} /></div>
              <div className="flex gap-2">
                <Button type="primary" htmlType="submit" loading={busy} disabled={busy || !sample.trim()}>开始检测</Button>
                <Button disabled={busy} onClick={() => showForm(false)}>取消</Button>
              </div>
            </form>
          </section>
        )}
        {error && <Alert title={error} type="error" showIcon />}
        <section className="panel" aria-labelledby="history-title">
          <h2 id="history-title">最近记录</h2>
          <Table<Detection> dataSource={rows} rowKey="id" pagination={false} loading={busy} scroll={{ x: 610 }} locale={{ emptyText: '暂无检测任务' }} columns={[
            { title: '创建时间', render: (_, row) => formatDate(row.createdAt) },
            { title: '样本编号', dataIndex: 'sample' },
            { title: '提交者', dataIndex: 'owner' },
            { title: '状态', render: (_, row) => <Tag color={row.status === 'COMPLETED' ? 'success' : 'default'}>{row.status === 'COMPLETED' ? '已完成' : '待完成'}</Tag> },
            { title: '检测结果', render: (_, row) => <Button type="link" onClick={() => openResult(row)}>{row.result ? '查看结果' : '查看状态'}</Button> },
          ]} />
        </section>
      </div>
      <p className="status-space" role="status">{notice}</p>
      <Drawer title="任务详情" open={drawerOpen} onClose={() => setDrawerOpen(false)} size={480} styles={{ section: { maxWidth: '100vw' } }}>
        {result && <div className="grid gap-3">
          <h2>{result.sample}</h2>
          <p className="muted">{formatDate(result.createdAt)} · {result.owner}</p>
          {result.result ? <><p>检出项：{result.result.label}</p><p>置信度：{result.result.score}</p></> :
            <><p>任务待完成。</p><Button loading={busy} onClick={refresh}>刷新任务状态</Button></>}
        </div>}
      </Drawer>
    </>
  )
}
