import { useEffect, useState, type FormEvent } from 'react'
import { api, errorCode, errorMessage } from '../api/client'
import { todayIso, useStatuses } from '../api/useStatuses'
import type { ProgressEvent } from '../api/types'
import { Modal } from '../components/Modal'
import { Button, ErrorNote, Field, Input, Select, Textarea } from '../components/ui'

/**
 * Add or edit one timeline event (PRG-02, PRG-03, PRG-05).
 *
 * <p>The status list comes from the API. No transition is blocked here, because
 * the server accepts any status after any other and the UI must not invent a
 * rule the domain does not have.
 */
export function ProgressModal({
  open,
  applicationId,
  existing,
  canDelete,
  onClose,
  onChanged,
}: {
  open: boolean
  applicationId: number
  existing?: ProgressEvent | null
  canDelete: boolean
  onClose: () => void
  onChanged: () => void
}) {
  const { statuses } = useStatuses()
  const editing = Boolean(existing)

  const [statusId, setStatusId] = useState<number | ''>('')
  const [eventDate, setEventDate] = useState(todayIso())
  const [notes, setNotes] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [saving, setSaving] = useState(false)
  const [deleting, setDeleting] = useState(false)

  useEffect(() => {
    if (!open) return
    setError(null)
    setStatusId(existing?.statusId ?? statuses[0]?.id ?? '')
    setEventDate(existing?.eventDate ?? todayIso())
    setNotes(existing?.notes ?? '')
  }, [open, existing, statuses])

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault()
    if (statusId === '') return

    setError(null)
    setSaving(true)
    const payload = { statusId, eventDate, notes: notes.trim() || null }

    try {
      if (editing) {
        await api.put(`/applications/${applicationId}/progress/${existing!.id}`, payload)
      } else {
        await api.post(`/applications/${applicationId}/progress`, payload)
      }
      onChanged()
      onClose()
    } catch (err) {
      setError(errorMessage(err, 'Could not save this event.'))
    } finally {
      setSaving(false)
    }
  }

  const handleDelete = async () => {
    setError(null)
    setDeleting(true)
    try {
      await api.delete(`/applications/${applicationId}/progress/${existing!.id}`)
      onChanged()
      onClose()
    } catch (err) {
      setError(
        errorCode(err) === 'LAST_PROGRESS_EVENT'
          ? 'An application must keep at least one event. Add another before deleting this one.'
          : errorMessage(err, 'Could not delete this event.'),
      )
    } finally {
      setDeleting(false)
    }
  }

  return (
    <Modal
      open={open}
      onClose={onClose}
      title={editing ? 'Edit event' : 'Add event'}
      footer={
        <>
          {editing && (
            <Button
              variant="danger"
              onClick={handleDelete}
              loading={deleting}
              disabled={!canDelete}
              title={canDelete ? undefined : 'An application must keep at least one event.'}
              className="mr-auto"
            >
              Delete
            </Button>
          )}
          <Button onClick={onClose} type="button">
            Cancel
          </Button>
          <Button variant="primary" form="progress-form" type="submit" loading={saving}>
            Save
          </Button>
        </>
      }
    >
      <form id="progress-form" onSubmit={handleSubmit} className="flex flex-col gap-4" noValidate>
        {error && <ErrorNote>{error}</ErrorNote>}

        <Field label="Status" htmlFor="statusId">
          <Select
            id="statusId"
            required
            value={statusId}
            onChange={(e) => setStatusId(Number(e.target.value))}
          >
            {statuses.map((status) => (
              <option key={status.id} value={status.id}>
                {status.name}
              </option>
            ))}
          </Select>
        </Field>

        <Field label="Date" htmlFor="eventDate">
          <Input
            id="eventDate"
            type="date"
            required
            value={eventDate}
            onChange={(e) => setEventDate(e.target.value)}
          />
        </Field>

        <Field
          label="Notes"
          htmlFor="progressNotes"
          hint="Context for this step — including why a rejection happened."
        >
          <Textarea
            id="progressNotes"
            rows={4}
            value={notes}
            onChange={(e) => setNotes(e.target.value)}
          />
        </Field>
      </form>
    </Modal>
  )
}
