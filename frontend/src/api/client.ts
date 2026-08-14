import axios, { AxiosError } from 'axios'
import type { ApiErrorBody } from './types'

const TOKEN_KEY = 'jobtracker.token'

/**
 * The token is mirrored to localStorage so a reload survives.
 *
 * <p>MVP.md 12 records this as an accepted XSS trade-off for a 5-8 user
 * internal tool, with an HttpOnly cookie plus CSRF token as the deferred fix.
 */
export const tokenStorage = {
  get: () => localStorage.getItem(TOKEN_KEY),
  set: (token: string) => localStorage.setItem(TOKEN_KEY, token),
  clear: () => localStorage.removeItem(TOKEN_KEY),
}

export const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080/api',
})

api.interceptors.request.use((config) => {
  const token = tokenStorage.get()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

/** Set by AuthProvider so a 401 can clear context state, not just storage. */
let onUnauthenticated: (() => void) | null = null

export function setUnauthenticatedHandler(handler: () => void) {
  onUnauthenticated = handler
}

api.interceptors.response.use(
  (response) => response,
  (error: AxiosError<ApiErrorBody>) => {
    // An expired or rejected token means the session is over wherever it
    // happens, so recovery is centralised here rather than in every caller.
    if (error.response?.status === 401) {
      tokenStorage.clear()
      onUnauthenticated?.()
    }
    return Promise.reject(error)
  },
)

/**
 * Pulls a human-readable message out of the API's error envelope, preferring
 * the first field error so form mistakes name the offending field.
 */
export function errorMessage(error: unknown, fallback = 'Something went wrong.'): string {
  if (axios.isAxiosError<ApiErrorBody>(error)) {
    const body = error.response?.data
    if (body?.fieldErrors?.length) {
      const first = body.fieldErrors[0]
      return `${first.field}: ${first.message}`
    }
    if (body?.message) {
      return body.message
    }
    if (!error.response) {
      return 'Cannot reach the server. Is the API running?'
    }
  }
  return fallback
}

export function errorCode(error: unknown): string | undefined {
  return axios.isAxiosError<ApiErrorBody>(error) ? error.response?.data?.code : undefined
}
