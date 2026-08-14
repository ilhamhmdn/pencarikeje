import { useEffect, useState } from 'react'
import { api } from './client'
import type { StatusResponse } from './types'

/**
 * Loads the status catalogue from the API.
 *
 * <p>MVP.md 8.2 makes hardcoding status values client-side a review-blocking
 * defect, so every dropdown and filter in the app reads from here.
 */
export function useStatuses() {
  const [statuses, setStatuses] = useState<StatusResponse[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    let cancelled = false

    api
      .get<StatusResponse[]>('/statuses')
      .then((response) => {
        if (!cancelled) setStatuses(response.data)
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })

    return () => {
      cancelled = true
    }
  }, [])

  return { statuses, loading }
}

/** Renders an ISO date as e.g. "1 Aug 2026". Timestamps are stored UTC; formatting is ours (NFR-07). */
export function formatDate(iso: string | null | undefined): string {
  if (!iso) return '—'
  const date = new Date(iso.length <= 10 ? `${iso}T00:00:00` : iso)
  return date.toLocaleDateString('en-GB', { day: 'numeric', month: 'short', year: 'numeric' })
}

/** Today in the yyyy-MM-dd form the API expects, in the user's local zone. */
export function todayIso(): string {
  const now = new Date()
  const offset = now.getTimezoneOffset() * 60_000
  return new Date(now.getTime() - offset).toISOString().slice(0, 10)
}
