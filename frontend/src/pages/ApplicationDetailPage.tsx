import { useCallback, useEffect, useRef, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { api, errorMessage } from '../api/client'
import { formatDate } from '../api/useStatuses'
import type { ApplicationDetail, ProgressEvent } from '../api/types'
import { Modal } from '../components/Modal'
import {
  Button,
  ErrorNote,
  SkeletonRows,
  StatusDot,
  StatusLabel,
  cx,
} from '../components/ui'
import { ApplicationFormModal } from './ApplicationFormModal'
import { ProgressModal } from './ProgressModal'

export function ApplicationDetailPage() {
  const { id } = useParams<{ id: string }>()
  const applicationId = Number(id)
  const navigate = useNavigate()

  const [application, setApplication] = useState<ApplicationDetail | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const [editOpen, setEditOpen] = useState(false)
  const [progressTarget, setProgressTarget] = useState<ProgressEvent | null>(null)
  const [progressOpen, setProgressOpen] = useState(false)
  const [confirmDelete, setConfirmDelete] = useState(false)
  const [uploading, setUploading] = useState(false)
  const fileInput = useRef<HTMLInputElement>(null)

  const load = useCallback(() => {
    api
      .get<ApplicationDetail>(`/applications/${applicationId}`)
      .then((response) => {
        setApplication(response.data)
        setError(null)
      })
      .catch((err) => setError(errorMessage(err, 'Could not load this application.')))
      .finally(() => setLoading(false))
  }, [applicationId])

  useEffect(load, [load])

  const handleUpload = async (file: File) => {
    setUploading(true)
    setError(null)
    const body = new FormData()
    body.append('file', file)

    try {
      await api.post(`/applications/${applicationId}/resume`, body)
      load()
    } catch (err) {
      setError(errorMessage(err, 'Could not upload that file.'))
    } finally {
      setUploading(false)
      if (fileInput.current) fileInput.current.value = ''
    }
  }

  /*
   * The resume endpoint needs the Authorization header, so a plain link cannot
   * fetch it. We pull the bytes through axios and hand the browser an object
   * URL instead.
   */
  const openResume = async () => {
    try {
      const response = await api.get(`/applications/${applicationId}/resume`, {
        responseType: 'blob',
      })
      const url = URL.createObjectURL(response.data as Blob)
      window.open(url, '_blank', 'noopener')
      setTimeout(() => URL.revokeObjectURL(url), 60_000)
    } catch (err) {
      setError(errorMessage(err, 'Could not open the resume.'))
    }
  }

  const handleDelete = async () => {
    try {
      await api.delete(`/applications/${applicationId}`)
      navigate('/applications', { replace: true })
    } catch (err) {
      setError(errorMessage(err, 'Could not delete this application.'))
      setConfirmDelete(false)
    }
  }

  if (loading) return <SkeletonRows rows={8} />
  if (error && !application) return <ErrorNote>{error}</ErrorNote>
  if (!application) return null

  return (
    <>
      <Link to="/applications" className="text-sm text-muted hover:text-accent">
        ← Applications
      </Link>

      <div className="mt-3 mb-6 flex flex-wrap items-start justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-ink">{application.companyName}</h1>
          <p className="mt-1 text-base text-muted">{application.roleName}</p>
          <p className="mt-3 flex flex-wrap items-center gap-x-4 gap-y-1 text-sm text-muted">
            <StatusLabel
              code={application.currentStatus.code}
              name={application.currentStatus.name}
            />
            <span>Applied {formatDate(application.dateApplied)}</span>
            {application.portalUrl && (
              <a
                href={application.portalUrl}
                target="_blank"
                rel="noreferrer noopener"
                className="hover:text-accent"
              >
                Portal ↗
              </a>
            )}
          </p>
        </div>

        <div className="flex gap-2">
          <Button onClick={() => setEditOpen(true)}>Edit</Button>
          <Button variant="danger" onClick={() => setConfirmDelete(true)}>
            Delete
          </Button>
        </div>
      </div>

      {error && (
        <div className="mb-4">
          <ErrorNote>{error}</ErrorNote>
        </div>
      )}

      <div className="grid gap-6 lg:grid-cols-[1.3fr_1fr]">
        <section className="rounded-lg border border-line bg-surface">
          <header className="flex items-center justify-between border-b border-line px-5 py-3">
            <h2 className="text-sm font-semibold text-ink">Progress</h2>
            <Button
              variant="primary"
              onClick={() => {
                setProgressTarget(null)
                setProgressOpen(true)
              }}
            >
              Add event
            </Button>
          </header>

          <ol className="px-5 py-5">
            {application.progress.map((event, index) => {
              const isLatest = index === application.progress.length - 1
              return (
                <li key={event.id} className="relative flex gap-4 pb-6 last:pb-0">
                  {/* Connector, drawn behind every node except the last. */}
                  {index < application.progress.length - 1 && (
                    <span aria-hidden className="absolute top-4 bottom-0 left-[0.3125rem] w-px bg-line-strong" />
                  )}

                  <span className="relative z-10 mt-1.5">
                    <StatusDot code={event.status.code} />
                    {isLatest && (
                      <span
                        aria-hidden
                        className="absolute -inset-1 rounded-full border-2 border-accent"
                      />
                    )}
                  </span>

                  <button
                    type="button"
                    onClick={() => {
                      setProgressTarget(event)
                      setProgressOpen(true)
                    }}
                    className="flex-1 rounded-md px-2 py-1 text-left transition-colors duration-150 hover:bg-canvas"
                  >
                    <span className="flex flex-wrap items-baseline justify-between gap-x-3">
                      <span className={cx('font-medium', isLatest ? 'text-ink' : 'text-muted')}>
                        {event.status.name}
                      </span>
                      <span className="text-sm tabular-nums text-faint">
                        {formatDate(event.eventDate)}
                      </span>
                    </span>
                    {event.notes && (
                      <span className="mt-1 block text-sm leading-relaxed whitespace-pre-wrap text-muted">
                        {event.notes}
                      </span>
                    )}
                  </button>
                </li>
              )
            })}
          </ol>
        </section>

        <div className="flex flex-col gap-6">
          <section className="rounded-lg border border-line bg-surface">
            <h2 className="border-b border-line px-5 py-3 text-sm font-semibold text-ink">Resume</h2>
            <div className="flex flex-wrap items-center gap-3 px-5 py-4">
              {application.resumeFilename ? (
                <>
                  <span className="text-sm text-ink">{application.resumeFilename}</span>
                  <Button onClick={openResume}>View</Button>
                </>
              ) : (
                <span className="text-sm text-muted">No resume attached.</span>
              )}

              <Button
                onClick={() => fileInput.current?.click()}
                loading={uploading}
                className={application.resumeFilename ? '' : 'ml-auto'}
              >
                {application.resumeFilename ? 'Replace' : 'Upload PDF'}
              </Button>

              <input
                ref={fileInput}
                type="file"
                accept="application/pdf"
                className="hidden"
                onChange={(e) => {
                  const file = e.target.files?.[0]
                  if (file) handleUpload(file)
                }}
              />
            </div>
            <p className="px-5 pb-4 text-xs text-muted">PDF only, up to 5 MB.</p>
          </section>

          <section className="rounded-lg border border-line bg-surface">
            <h2 className="border-b border-line px-5 py-3 text-sm font-semibold text-ink">
              Job description
            </h2>
            <div className="px-5 py-4">
              {application.jobDescription ? (
                <p className="max-h-64 overflow-y-auto text-sm leading-relaxed whitespace-pre-wrap text-ink">
                  {application.jobDescription}
                </p>
              ) : (
                <p className="text-sm text-muted">
                  Nothing saved. Paste the advert when you edit — it usually disappears from the
                  original site.
                </p>
              )}
            </div>
          </section>

          {application.notes && (
            <section className="rounded-lg border border-line bg-surface">
              <h2 className="border-b border-line px-5 py-3 text-sm font-semibold text-ink">Notes</h2>
              <p className="px-5 py-4 text-sm leading-relaxed whitespace-pre-wrap text-ink">
                {application.notes}
              </p>
            </section>
          )}
        </div>
      </div>

      <ApplicationFormModal
        open={editOpen}
        existing={application}
        onClose={() => setEditOpen(false)}
        onSaved={setApplication}
      />

      <ProgressModal
        open={progressOpen}
        applicationId={applicationId}
        existing={progressTarget}
        canDelete={application.progress.length > 1}
        onClose={() => setProgressOpen(false)}
        onChanged={load}
      />

      <Modal
        open={confirmDelete}
        onClose={() => setConfirmDelete(false)}
        title="Delete this application?"
        footer={
          <>
            <Button onClick={() => setConfirmDelete(false)}>Cancel</Button>
            <Button variant="danger" onClick={handleDelete}>
              Delete permanently
            </Button>
          </>
        }
      >
        <p className="text-sm leading-relaxed text-muted">
          This removes {application.companyName} — {application.roleName}, its entire progress
          timeline and any attached resume. It cannot be undone.
        </p>
      </Modal>
    </>
  )
}
