import { useEffect, useState, type FormEvent } from 'react'
import { api, errorMessage } from '../api/client'
import { todayIso } from '../api/useStatuses'
import type { ApplicationDetail, ApplicationPayload } from '../api/types'
import { Modal } from '../components/Modal'
import { Button, ErrorNote, Field, Input, Textarea } from '../components/ui'

/**
 * Create and edit share one form (APP-01, APP-07). Creation is triggered from a
 * modal on the list; there is deliberately no /applications/new route.
 *
 * <p>Current status is absent because it is derived from the timeline.
 */
export function ApplicationFormModal({
  open,
  existing,
  onClose,
  onSaved,
}: {
  open: boolean
  existing?: ApplicationDetail | null
  onClose: () => void
  onSaved: (saved: ApplicationDetail) => void
}) {
  const editing = Boolean(existing)

  const [form, setForm] = useState<ApplicationPayload>({
    companyName: '',
    roleName: '',
    jobDescription: '',
    portalUrl: '',
    dateApplied: todayIso(),
    notes: '',
  })
  const [error, setError] = useState<string | null>(null)
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    if (!open) return
    setError(null)
    setForm({
      companyName: existing?.companyName ?? '',
      roleName: existing?.roleName ?? '',
      jobDescription: existing?.jobDescription ?? '',
      portalUrl: existing?.portalUrl ?? '',
      dateApplied: existing?.dateApplied ?? todayIso(),
      notes: existing?.notes ?? '',
    })
  }, [open, existing])

  const update = (patch: Partial<ApplicationPayload>) => setForm((prev) => ({ ...prev, ...patch }))

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault()
    setError(null)
    setSaving(true)

    // Blank optional fields are sent as null rather than "", so the API stores
    // absence instead of an empty string.
    const payload: ApplicationPayload = {
      ...form,
      jobDescription: form.jobDescription?.trim() || null,
      portalUrl: form.portalUrl?.trim() || null,
      notes: form.notes?.trim() || null,
    }

    try {
      const { data } = editing
        ? await api.put<ApplicationDetail>(`/applications/${existing!.id}`, payload)
        : await api.post<ApplicationDetail>('/applications', payload)
      onSaved(data)
      onClose()
    } catch (err) {
      setError(errorMessage(err, 'Could not save this application.'))
    } finally {
      setSaving(false)
    }
  }

  return (
    <Modal
      open={open}
      onClose={onClose}
      title={editing ? 'Edit application' : 'New application'}
      wide
      footer={
        <>
          <Button onClick={onClose} type="button">
            Cancel
          </Button>
          <Button variant="primary" form="application-form" type="submit" loading={saving}>
            {editing ? 'Save changes' : 'Add application'}
          </Button>
        </>
      }
    >
      <form id="application-form" onSubmit={handleSubmit} className="flex flex-col gap-4" noValidate>
        {error && <ErrorNote>{error}</ErrorNote>}

        <div className="grid gap-4 sm:grid-cols-2">
          <Field label="Company" htmlFor="companyName">
            <Input
              id="companyName"
              required
              maxLength={255}
              value={form.companyName}
              onChange={(e) => update({ companyName: e.target.value })}
            />
          </Field>

          <Field label="Role" htmlFor="roleName">
            <Input
              id="roleName"
              required
              maxLength={255}
              value={form.roleName}
              onChange={(e) => update({ roleName: e.target.value })}
            />
          </Field>

          <Field label="Date applied" htmlFor="dateApplied">
            <Input
              id="dateApplied"
              type="date"
              required
              max={todayIso()}
              value={form.dateApplied}
              onChange={(e) => update({ dateApplied: e.target.value })}
            />
          </Field>

          <Field label="Portal URL" htmlFor="portalUrl" hint="Optional. Must start with http:// or https://">
            <Input
              id="portalUrl"
              type="url"
              placeholder="https://"
              value={form.portalUrl ?? ''}
              onChange={(e) => update({ portalUrl: e.target.value })}
            />
          </Field>
        </div>

        <Field
          label="Job description"
          htmlFor="jobDescription"
          hint="Worth pasting — the original advert usually disappears."
        >
          <Textarea
            id="jobDescription"
            rows={6}
            value={form.jobDescription ?? ''}
            onChange={(e) => update({ jobDescription: e.target.value })}
          />
        </Field>

        <Field label="Notes" htmlFor="notes">
          <Textarea
            id="notes"
            rows={3}
            value={form.notes ?? ''}
            onChange={(e) => update({ notes: e.target.value })}
          />
        </Field>
      </form>
    </Modal>
  )
}
