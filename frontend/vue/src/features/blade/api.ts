import type { RequestApi } from '../../lib/http'

export type Task = {
  id: string
  farmName: string
  turbineNumber: string
  originalName: string
  status: 'PENDING' | 'RUNNING' | 'COMPLETED' | 'FAILED'
  createdAt: string
  failureMessage: string | null
}
export type Page<T> = { items: T[]; page: number; hasNext: boolean }
export type Summary = {
  model: string
  imageCount: number
  defectImageCount: number
  detectionCount: number
  classes: Record<string, number>
}
export type InspectionImage = {
  index: number
  name: string
  detections: { label: string; confidence: number; polygon: [number, number][] }[]
}

const root = 'detection-tasks'
export const statusLabel: Record<Task['status'], string> = {
  PENDING: '等待检测', RUNNING: '检测中', COMPLETED: '已完成', FAILED: '失败',
}
export const formatDate = (value: string) => new Date(value).toLocaleString('zh-CN', { hour12: false })
export const bladeApi = (request: RequestApi) => ({
  list: (page: number) => request<Page<Task>>(`${root}?page=${page}`),
  task: (id: string) => request<Task>(`${root}/${id}`),
  summary: (id: string) => request<Summary>(`${root}/${id}/report`),
  images: (id: string, page: number) => request<Page<InspectionImage>>(`${root}/${id}/images?page=${page}`),
  preview: (id: string, index: number, signal: AbortSignal) => request.blob(`${root}/${id}/images/${index}/preview`, signal),
  pdf: (id: string) => request.blob(`${root}/${id}/report.pdf`),
  submit: async (farmName: string, turbineNumber: string, file: File, progress: (percent: number) => void, signal: AbortSignal) => {
    const query = new URLSearchParams({ farmName, turbineNumber, filename: file.name })
    return await request.uploadZip(`${root}?${query}`, file, progress, signal) as Task
  },
})
