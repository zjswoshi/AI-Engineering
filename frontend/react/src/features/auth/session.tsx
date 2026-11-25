import { createContext, useContext, useState, type ReactNode } from 'react'
import type { Session } from './model'

const AuthContext = createContext<{ session: Session | null; setSession: (session: Session | null) => void } | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [session, setSession] = useState<Session | null>(null)
  return <AuthContext.Provider value={{ session, setSession }}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) throw new Error('缺少登录状态 Provider。')
  return context
}

export function useSession() {
  const { session } = useAuth()
  if (!session) throw new Error('请先登录。')
  return session
}
