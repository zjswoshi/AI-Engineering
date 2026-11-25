import { createRequest, type RequestApi } from '../../lib/http'

export type AnalystRole = 'VIEWER' | 'OPERATOR'
export type Role = AnalystRole | 'ADMIN'
export type Profile = { username: string; role: Role }
export type Session = { profile: Profile; request: RequestApi }

export const roleLabels: Record<Role, string> = {
  VIEWER: '查看者',
  OPERATOR: '操作员',
  ADMIN: '管理员',
}

export async function authenticate(username: string, password: string): Promise<Session> {
  const request = createRequest(username.trim(), password)
  return { request, profile: await request<Profile>('me') }
}

export function hasAccess(role: Role, allowed?: readonly Role[]) {
  return !allowed || allowed.includes(role)
}

export function returnPath(value: unknown) {
  if (typeof value === 'string' && value.startsWith('/') && !value.startsWith('//')
      && !value.includes('\\') && !/[\u0000-\u001f\u007f]/.test(value) && !/^\/login(?:[/?#]|$)/i.test(value)) {
    return value
  }
  return '/'
}
