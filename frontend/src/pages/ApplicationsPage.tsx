import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api, errorMessage } from '../api/client'
import { formatDate, useStatuses } from '../api/useStatuses'
import type { ApplicationDetail, ApplicationListItem, PageResponse } from '../api/types'
import { PageHeader } from '../components/AppLayout'
import { Modal } from '../components/Modal'
import {
  Button,
  EmptyState,
  ErrorNote,
  Input,
  Select,
  SkeletonRows,
  StatusLabel,
  cx,
} from '../components/ui'
import { ApplicationFormModal } from './ApplicationFormModal'

/** Keys the API accepts. An unknown key is a 400, so the UI must match exactly. */
const COLUMNS = [
  { key: 'companyName', label: 'Company' },
  { key: 'roleName', label: 'Role' },
  { key: 'status', label: 'Status' },
  { key: 'dateApplied', label: 'Applied' },
  { key: 'updatedAt', label: 'Updated' },
] as const

type SortKey = (typeof COLUMNS)[number]['key']

export function ApplicationsPage() {
  const { statuses } = useStatuses()

  const [query, setQuery] = useState('')
  const [debouncedQuery, setDebouncedQuery] = useState('')
  const [statusCode, setStatusCode] = useState('')
  const [sort, setSort] = useState<SortKey>('dateApplied')
  const [direction, setDirection] = useState<'asc' | 'desc'>('desc')
  const [page, setPage] = useState(0)

  const [data, setData] = useState<PageResponse<ApplicationListItem> | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const [formOpen, setFormOpen] = useState(false)
  const [jobDescription, setJobDescription] = useState<{ role: string; text: string | null } | null>(null)

  // Debounced so typing does not fire a request per keystroke.
  useEffect(() => {
    const timer = setTimeout(() => {
      setDebouncedQuery(query)
      setPage(0)
    }, 250)
    return () => clearTimeout(timer)
  }, [query])

  const load = useCallback(() => {
    setLoading(true)
    api
      .get<PageResponse<ApplicationListItem>>('/applications', {
        params: {
          q: debouncedQuery || undefined,
          statusCode: statusCode || undefined,
          sort,
          direction,
          page,
          size: 20,
        },
      })
      .then((response) => {
        setData(response.data)
        setError(null)
      })
      .catch((err) => setError(errorMessage(err, 'Could not load your applications.')))
      .finally(() => setLoading(false))
  }, [debouncedQuery, statusCode, sort, direction, page])

  useEffect(load, [load])

  const handleSort = (key: SortKey) => {
    if (key === sort) {
      setDirection((prev) => (prev === 'asc' ? 'desc' : 'asc'))
    } else {
      setSort(key)
      setDirection(key === 'companyName' || key === 'roleName' ? 'asc' : 'desc')
    }
    setPage(0)
  }

  const openJobDescription = async (app: ApplicationListItem) => {
    // The list row does not carry the JD, so fetch the detail on demand.
    try {
      const { data: detail } = await api.get<ApplicationDetail>(`/applications/${app.id}`)
      setJobDescription({ role: detail.roleName, text: detail.jobDescription })
    } catch (err) {
      setError(errorMessage(err, 'Could not load the job description.'))
    }
  }

  const filtersActive = Boolean(debouncedQuery || statusCode)

  return (
    <>
      <PageHeader
        title="Applications"
        description={data ? `${data.totalElements} tracked` : undefined}
        action={
          <Button variant="primary" onClick={() => setFormOpen(true)}>
            New application
          </Button>
        }
      />

      <div className="mb-4 flex flex-wrap gap-3">
        <Input
          type="search"
          placeholder="Search company or role"
          aria-label="Search company or role"
          className="max-w-xs"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
        />

        <Select
          aria-label="Filter by status"
          className="max-w-[14rem]"
          value={statusCode}
          onChange={(e) => {
            setStatusCode(e.target.value)
            setPage(0)
          }}
        >
          <option value="">All statuses</option>
          {statuses.map((status) => (
            <option key={status.id} value={status.code}>
              {status.name}
            </option>
          ))}
        </Select>
      </div>

      {error && (
        <div className="mb-4">
          <ErrorNote>{error}</ErrorNote>
        </div>
      )}

      <div className="overflow-hidden rounded-lg border border-line bg-surface">
        {loading && !data ? (
          <SkeletonRows rows={8} />
        ) : data && data.content.length === 0 ? (
          <EmptyState title={filtersActive ? 'No matches' : 'No applications yet'}>
            {filtersActive
              ? 'Try a different search or clear the status filter.'
              : 'Add your first application to start tracking it.'}
          </EmptyState>
        ) : (
          <>
            {/* Desktop: dense sortable table. */}
            <table className="hidden w-full border-collapse text-left sm:table">
              <thead>
                <tr className="border-b border-line">
                  {COLUMNS.map((column) => {
                    const active = sort === column.key
                    return (
                      <th key={column.key} scope="col" className="px-4 py-2.5">
                        <button
                          type="button"
                          onClick={() => handleSort(column.key)}
                          aria-sort={active ? (direction === 'asc' ? 'ascending' : 'descending') : 'none'}
                          className={cx(
                            'flex items-center gap-1 text-xs font-semibold tracking-wide uppercase transition-colors duration-150',
                            active ? 'text-ink' : 'text-muted hover:text-ink',
                          )}
                        >
                          {column.label}
                          <span aria-hidden className={cx('text-[0.6rem]', !active && 'opacity-0')}>
                            {direction === 'asc' ? '▲' : '▼'}
                          </span>
                        </button>
                      </th>
                    )
                  })}
                  <th scope="col" className="px-4 py-2.5" />
                </tr>
              </thead>

              <tbody className="divide-y divide-line">
                {data?.content.map((app) => (
                  <tr key={app.id} className="transition-colors duration-150 hover:bg-canvas">
                    <td className="px-4 py-3">
                      <Link
                        to={`/applications/${app.id}`}
                        className="font-medium text-ink hover:text-accent"
                      >
                        {app.companyName}
                      </Link>
                    </td>
                    <td className="px-4 py-3">
                      {/* APP-09: the role name opens the stored job description. */}
                      <button
                        type="button"
                        onClick={() => openJobDescription(app)}
                        className="text-left text-sm text-muted underline decoration-line-strong underline-offset-4 hover:text-ink"
                      >
                        {app.roleName}
                      </button>
                    </td>
                    <td className="px-4 py-3 text-sm">
                      <StatusLabel code={app.currentStatus.code} name={app.currentStatus.name} />
                    </td>
                    <td className="px-4 py-3 text-sm tabular-nums text-muted">
                      {formatDate(app.dateApplied)}
                    </td>
                    <td className="px-4 py-3 text-sm tabular-nums text-muted">
                      {formatDate(app.updatedAt)}
                    </td>
                    <td className="px-4 py-3 text-right">
                      {app.portalUrl && (
                        <a
                          href={app.portalUrl}
                          target="_blank"
                          rel="noreferrer noopener"
                          className="text-sm text-muted hover:text-accent"
                        >
                          Portal ↗
                        </a>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>

            {/* Mobile: the same rows stacked, since a 5-column table cannot fit. */}
            <ul className="divide-y divide-line sm:hidden">
              {data?.content.map((app) => (
                <li key={app.id}>
                  <Link to={`/applications/${app.id}`} className="block px-4 py-3 hover:bg-canvas">
                    <p className="font-medium text-ink">{app.companyName}</p>
                    <p className="text-sm text-muted">{app.roleName}</p>
                    <p className="mt-2 flex items-center justify-between text-sm text-muted">
                      <StatusLabel code={app.currentStatus.code} name={app.currentStatus.name} />
                      <span className="tabular-nums">{formatDate(app.dateApplied)}</span>
                    </p>
                  </Link>
                </li>
              ))}
            </ul>
          </>
        )}
      </div>

      {data && data.totalPages > 1 && (
        <div className="mt-4 flex items-center justify-between gap-4">
          <p className="text-sm text-muted">
            Page {data.page + 1} of {data.totalPages}
          </p>
          <div className="flex gap-2">
            <Button disabled={data.first} onClick={() => setPage((p) => Math.max(0, p - 1))}>
              Previous
            </Button>
            <Button disabled={data.last} onClick={() => setPage((p) => p + 1)}>
              Next
            </Button>
          </div>
        </div>
      )}

      <ApplicationFormModal open={formOpen} onClose={() => setFormOpen(false)} onSaved={() => load()} />

      <Modal
        open={jobDescription !== null}
        onClose={() => setJobDescription(null)}
        title={jobDescription?.role ?? 'Job description'}
        wide
      >
        {jobDescription?.text ? (
          <p className="max-h-[60vh] overflow-y-auto text-sm leading-relaxed whitespace-pre-wrap text-ink">
            {jobDescription.text}
          </p>
        ) : (
          <EmptyState title="No job description saved">
            Paste the advert when you edit this application — the original usually disappears.
          </EmptyState>
        )}
      </Modal>
    </>
  )
}
