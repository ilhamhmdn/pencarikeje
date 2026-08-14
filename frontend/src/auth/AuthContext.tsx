import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from 'react'
import { api, setUnauthenticatedHandler, tokenStorage } from '../api/client'
import type { AuthResponse, UserResponse } from '../api/types'

interface AuthState {
  user: UserResponse | null
  initialising: boolean
  login: (email: string, password: string) => Promise<void>
  register: (
    name: string,
    email: string,
    password: string,
    confirmPassword: string,
  ) => Promise<void>
  logout: () => void
  setUser: (user: UserResponse) => void
}

const AuthContext = createContext<AuthState | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<UserResponse | null>(null)
  const [initialising, setInitialising] = useState(true)

  const logout = useCallback(() => {
    tokenStorage.clear()
    setUser(null)
  }, [])

  // A 401 from anywhere ends the session, including one raised while restoring
  // it below.
  useEffect(() => {
    setUnauthenticatedHandler(() => setUser(null))
  }, [])

  /*
   * On boot the stored token is the only thing we have. Rather than trust it,
   * we spend one request confirming it still works and learning who it belongs
   * to, which also catches a token invalidated by a server restart.
   */
  useEffect(() => {
    const token = tokenStorage.get()
    if (!token) {
      setInitialising(false)
      return
    }

    api
      .get<UserResponse>('/profile')
      .then((response) => setUser(response.data))
      .catch(() => tokenStorage.clear())
      .finally(() => setInitialising(false))
  }, [])

  const login = useCallback(async (email: string, password: string) => {
    const { data } = await api.post<AuthResponse>('/auth/login', { email, password })
    tokenStorage.set(data.token)
    setUser(data.user)
  }, [])

  const register = useCallback(
    async (name: string, email: string, password: string, confirmPassword: string) => {
      const { data } = await api.post<AuthResponse>('/auth/register', {
        name,
        email,
        password,
        confirmPassword,
      })
      tokenStorage.set(data.token)
      setUser(data.user)
    },
    [],
  )

  const value = useMemo<AuthState>(
    () => ({ user, initialising, login, register, logout, setUser }),
    [user, initialising, login, register, logout],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth(): AuthState {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error('useAuth must be used inside an AuthProvider')
  }
  return context
}
