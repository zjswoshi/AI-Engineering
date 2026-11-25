import type { RequestApi } from '../../lib/http'
import type { Role } from '../auth/model'

export type Detection = {
  id: string
  sample: string
  owner: string
  createdAt: string
  status: 'PENDING' | 'COMPLETED'
  result: { label: string; score: number } | null
}

export const submitRoles: readonly Role[] = ['OPERATOR', 'ADMIN']
export const listDetections = (request: RequestApi) => request<Detection[]>('detections')
export const submitDetection = (request: RequestApi, sample: string) =>
  request<Detection>('detections', 'POST', { sample })

const dates = new Intl.DateTimeFormat('zh-CN', { dateStyle: 'medium', timeStyle: 'short' })
export const formatDate = (value: string) => dates.format(new Date(value))
