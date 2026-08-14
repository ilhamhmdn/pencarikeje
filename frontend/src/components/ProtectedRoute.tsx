import { Navigate, useLocation } from 'react-router-dom'
import type { ReactNode } from 'react'
import { useAuth } from '../auth/AuthContext'
import { Spinner } from './ui'

/**
 * Everything except /login and /register sits behind this.
 *
 * <p>While the stored token is being verified we render a spinner rather than
 * redirecting, otherwise a reload would bounce an authenticated user to the
 * login screen before the check finishes.
 */
export function ProtectedRoute({ children }: { children: ReactNode }) {
  const { user, initialising } = useAuth()
  const location = useLocation()

  if (initialising) {
    return (
      <div className="flex min-h-screen items-center justify-center text-muted">
        <Spinner className="size-6" />
      </div>
    )
  }

  if (!user) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />
  }

  return <>{children}</>
}
