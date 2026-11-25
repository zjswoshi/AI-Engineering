import type { AnalystRole } from '../auth/model'
import type { RequestApi } from '../../lib/http'

type Analyst = { username: string; role: AnalystRole }

export const loadAnalyst = (request: RequestApi) => request<Analyst>('admin/analyst')
export const saveAnalyst = (request: RequestApi, role: AnalystRole) =>
  request<Analyst>('admin/analyst', 'PUT', { role })
