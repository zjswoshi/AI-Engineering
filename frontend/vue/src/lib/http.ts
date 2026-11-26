export type RequestApi = {
  <T>(path: string, method?: string, data?: unknown): Promise<T>
  blob(path: string, signal?: AbortSignal): Promise<Blob>
  uploadZip(path: string, file: File, progress: (percent: number) => void, signal: AbortSignal): Promise<unknown>
}

export function createRequest(username: string, password: string): RequestApi {
  const bytes = new TextEncoder().encode(`${username}:${password}`)
  const authorization = `Basic ${btoa(Array.from(bytes, (byte) => String.fromCharCode(byte)).join(''))}`

  const request = async <T>(path: string, method = 'GET', data?: unknown): Promise<T> => {
    const headers = new Headers({ Authorization: authorization, 'X-Requested-With': 'XMLHttpRequest' })
    if (data !== undefined) headers.set('Content-Type', 'application/json')
    const response = await fetch(`/api/${path}`, {
      method,
      headers,
      body: data === undefined ? undefined : JSON.stringify(data),
      signal: AbortSignal.timeout(15000),
    })
    if (!response.ok) throw new Error(await responseMessage(response))
    return (response.status === 204 || response.headers.get('Content-Length') === '0' ? undefined : await response.json()) as T
  }
  const authHeaders = { Authorization: authorization, 'X-Requested-With': 'XMLHttpRequest' }
  return Object.assign(request, {
    async blob(path: string, signal?: AbortSignal) {
      const response = await fetch(`/api/${path}`, { headers: authHeaders, signal })
      if (!response.ok) throw new Error(await responseMessage(response))
      return response.blob()
    },
    uploadZip(path: string, file: File, progress: (percent: number) => void, signal: AbortSignal) {
      return new Promise<unknown>((resolve, reject) => {
        const xhr = new XMLHttpRequest()
        const abort = () => xhr.abort()
        if (signal.aborted) {
          reject(new Error('上传已取消。'))
          return
        }
        signal.addEventListener('abort', abort, { once: true })
        xhr.addEventListener('loadend', () => signal.removeEventListener('abort', abort))
        xhr.open('POST', `/api/${path}`)
        xhr.setRequestHeader('Content-Type', 'application/zip')
        for (const [header, value] of Object.entries(authHeaders)) xhr.setRequestHeader(header, value)
        xhr.upload.onprogress = (event) => {
          if (event.lengthComputable) progress(Math.round((event.loaded / event.total) * 100))
        }
        xhr.onload = async () => {
          if (xhr.status >= 200 && xhr.status < 300) {
            try { resolve(JSON.parse(xhr.responseText)) } catch { reject(new Error('任务响应格式有误。')) }
          } else {
            const contentType = xhr.getResponseHeader('Content-Type') ?? ''
            reject(new Error(await responseMessage(new Response(xhr.responseText, {
              status: xhr.status, headers: { 'Content-Type': contentType },
            }))))
          }
        }
        xhr.onerror = () => reject(new Error('上传中断，请重新上传。'))
        xhr.onabort = () => reject(new Error('上传已取消。'))
        xhr.send(file)
      })
    },
  })
}

async function responseMessage(response: Response) {
  const contentType = response.headers.get('Content-Type')?.split(';', 1)[0].trim().toLowerCase()
  if (contentType === 'application/problem+json') {
    const problem = await response.clone().json().catch(() => null) as { code?: unknown } | null
    switch (problem?.code) {
      case 'INVALID_SAMPLE': return '样本编号须为 1 至 120 个字符。'
      case 'INVALID_ROLE': return '角色只能设为查看者或操作员。'
      case 'INVALID_REQUEST': return '请求参数或正文格式有误。'
    }
  }
  const problemDetail = contentType === 'application/problem+json'
    ? await response.clone().json().catch(() => null) as { detail?: unknown } | null : null
  if (response.status === 413) return 'ZIP 超过服务器上传上限。'
  if (response.status === 409 && !problemDetail) return '账号已存在。'
  if (typeof problemDetail?.detail === 'string') return problemDetail.detail
  if (response.status === 401) return '账号或密码有误。'
  if (response.status === 403) return '当前账号没有此权限。'
  if (response.status === 503) return '算法服务暂不可用。'
  return `请求失败（${response.status}）。`
}

export function requestError(error: unknown) {
  return error instanceof Error && error.name === 'Error' ? error.message : '连接失败，请重试。'
}
