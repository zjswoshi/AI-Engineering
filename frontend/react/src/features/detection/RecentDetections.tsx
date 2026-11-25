import { useEffect, useState } from 'react'
import { Alert, Button, Table, Tag } from 'antd'
import { Link, useNavigate } from 'react-router'
import { requestError } from '../../lib/http'
import { hasAccess } from '../auth/model'
import { useSession } from '../auth/session'
import { formatDate, listDetections, submitRoles, type Detection } from './api'

export default function RecentDetections() {
  const { request, profile } = useSession()
  const navigate = useNavigate()
  const [rows, setRows] = useState<Detection[]>([])
  const [busy, setBusy] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    let active = true
    void listDetections(request).then((jobs) => {
      if (active) setRows(jobs.slice(0, 5))
    }, (cause: unknown) => { if (active) setError(requestError(cause)) })
      .finally(() => { if (active) setBusy(false) })
    return () => { active = false }
  }, [request])

  return (
    <section className="panel" aria-labelledby="tasks-title">
      <div className="page-title">
        <div><h2 id="tasks-title">最近检测任务</h2><p className="muted">使用样本编号体验检测流程。</p></div>
        <div className="flex flex-wrap items-center gap-3">
          {hasAccess(profile.role, submitRoles) && <Button type="primary" onClick={() => void navigate('/detections?new=1')}>新建检测任务</Button>}
          <Link className="shortcut" to="/detections">查看历史结果</Link>
        </div>
      </div>
      {error && <Alert title={error} type="error" showIcon />}
      {busy && <p role="status">正在加载任务…</p>}
      {!busy && !error && !rows.length && <p className="muted">暂无检测任务。</p>}
      {rows.length > 0 && <Table<Detection> size="small" dataSource={rows} rowKey="id" pagination={false} scroll={{ x: 405 }} columns={[
        { title: '样本编号', dataIndex: 'sample' },
        { title: '状态', render: (_, row) => <Tag color={row.status === 'COMPLETED' ? 'success' : 'default'}>{row.status === 'COMPLETED' ? '已完成' : '待完成'}</Tag> },
        { title: '创建时间', render: (_, row) => formatDate(row.createdAt) },
      ]} />}
    </section>
  )
}
