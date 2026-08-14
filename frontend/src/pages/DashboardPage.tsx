import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api, errorMessage } from '../api/client'
import { formatDate } from '../api/useStatuses'
import type { DashboardResponse } from '../api/types'
import { PageHeader } from '../components/AppLayout'
import { EmptyState, ErrorNote, SkeletonRows, StatusLabel, statusTone } from '../components/ui'

export function DashboardPage() {
  const [data, setData] = useState<DashboardResponse | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    api
      .get<DashboardResponse>('/dashboard')
      .then((response) => setData(response.data))
      .catch((err) => setError(errorMessage(err, 'Could not load your dashboard.')))
      .finally(() => setLoading(false))
  }, [])

  if (loading) return <SkeletonRows rows={6} />
  if (error) return <ErrorNote>{error}</ErrorNote>
  if (!data) return null

  return (
    <>
      <PageHeader title="Dashboard" description="A summary of where everything stands." />

      <div className="grid gap-4 sm:grid-cols-3">
        <Stat label="Applications" value={data.totalApplications} />
        <Stat label="In interview" value={data.interviewCount} />
        <Stat label="Offers" value={data.offerCount} />
      </div>

      <div className="mt-8 grid gap-6 lg:grid-cols-[1fr_1.2fr]">
        <section className="rounded-lg border border-line bg-surface">
          <h2 className="border-b border-line px-5 py-3 text-sm font-semibold text-ink">By status</h2>

          {data.statusBreakdown.length === 0 ? (
            <EmptyState title="Nothing tracked yet" />
          ) : (
            <ul className="divide-y divide-line">
              {data.statusBreakdown.map((row) => (
                <li key={row.statusCode} className="flex items-center gap-3 px-5 py-3">
                  <StatusLabel code={row.statusCode} name={row.statusName} />
                  <span className="ml-auto text-sm font-semibold tabular-nums text-ink">{row.count}</span>
                  {/* A proportion bar reads faster than the number alone. */}
                  <span className="hidden h-1.5 w-24 overflow-hidden rounded-full bg-canvas sm:block">
                    <span
                      className={`block h-full rounded-full ${statusTone(row.statusCode)}`}
                      style={{
                        width: `${Math.max(4, (row.count / Math.max(data.totalApplications, 1)) * 100)}%`,
                      }}
                    />
                  </span>
                </li>
              ))}
            </ul>
          )}
        </section>

        <section className="rounded-lg border border-line bg-surface">
          <h2 className="border-b border-line px-5 py-3 text-sm font-semibold text-ink">Recent</h2>

          {data.recentApplications.length === 0 ? (
            <EmptyState title="No applications yet">
              Add your first one from the Applications page.
            </EmptyState>
          ) : (
            <ul className="divide-y divide-line">
              {data.recentApplications.map((app) => (
                <li key={app.id}>
                  <Link
                    to={`/applications/${app.id}`}
                    className="flex flex-wrap items-baseline gap-x-3 gap-y-1 px-5 py-3 transition-colors duration-150 hover:bg-canvas"
                  >
                    <span className="font-medium text-ink">{app.companyName}</span>
                    <span className="text-sm text-muted">{app.roleName}</span>
                    <span className="ml-auto flex items-center gap-3 text-sm text-muted">
                      <StatusLabel code={app.currentStatus.code} name={app.currentStatus.name} />
                      <span className="tabular-nums">{formatDate(app.dateApplied)}</span>
                    </span>
                  </Link>
                </li>
              ))}
            </ul>
          )}
        </section>
      </div>
    </>
  )
}

function Stat({ label, value }: { label: string; value: number }) {
  return (
    <div className="rounded-lg border border-line bg-surface px-5 py-4">
      <p className="text-sm text-muted">{label}</p>
      <p className="mt-1 text-3xl font-bold tabular-nums tracking-tight text-ink">{value}</p>
    </div>
  )
}
