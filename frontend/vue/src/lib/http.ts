export type RequestApi = <T>(path: string, method?: string, data?: unknown) => Promise<T>

export function createRequest(username: string, password: string): RequestApi {
  const bytes = new TextEncoder().encode(`${username}:${password}`)
  const authorization = `Basic ${btoa(Array.from(bytes, (byte) => String.fromCharCode(byte)).join(''))}`

  return async <T>(path: string, method = 'GET', data?: unknown): Promise<T> => {
    const headers = new Headers({ Authorization: authorization, 'X-Requested-With': 'XMLHttpRequest' })
    if (data !== undefined) headers.set('Content-Type', 'application/json')
    const response = await fetch(`/api/${path}`, {
      method,
      headers,
      body: data === undefined ? undefined : JSON.stringify(data),
      signal: AbortSignal.timeout(15000),
    })
    if (!response.ok) throw new Error(await responseMessage(response))
    return response.json() as Promise<T>
  }
}

async function responseMessage(response: Response) {
  const contentType = response.headers.get('Content-Type')?.split(';', 1)[0].trim().toLowerCase()
  if (contentType === 'application/problem+json') {
    const problem = await response.json().catch(() => null) as { code?: unknown } | null
    switch (problem?.code) {
      case 'INVALID_SAMPLE': return '样本编号须为 1 至 120 个字符。'
      case 'INVALID_ROLE': return '角色只能设为查看者或操作员。'
      case 'INVALID_REQUEST': return '请求参数或正文格式有误。'
    }
  }
  if (response.status === 401) return '账号或密码有误。'
  if (response.status === 403) return '当前账号没有此权限。'
  if (response.status === 503) return '算法服务暂不可用。'
  return `请求失败（${response.status}）。`
}

export function requestError(error: unknown) {
  return error instanceof Error && error.name === 'Error' ? error.message : '连接失败，请重试。'
}
