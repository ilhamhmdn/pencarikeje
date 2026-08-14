import { useState, type FormEvent } from 'react'
import { api, errorMessage } from '../api/client'
import { useAuth } from '../auth/AuthContext'
import type { UserResponse } from '../api/types'
import { PageHeader } from '../components/AppLayout'
import { Button, ErrorNote, Field, Input } from '../components/ui'

export function ProfilePage() {
  const { user, setUser } = useAuth()

  const [name, setName] = useState(user?.name ?? '')
  const [nameStatus, setNameStatus] = useState<Message | null>(null)
  const [savingName, setSavingName] = useState(false)

  const [currentPassword, setCurrentPassword] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [passwordStatus, setPasswordStatus] = useState<Message | null>(null)
  const [savingPassword, setSavingPassword] = useState(false)

  const mismatch = confirmPassword.length > 0 && newPassword !== confirmPassword

  const handleName = async (event: FormEvent) => {
    event.preventDefault()
    setNameStatus(null)
    setSavingName(true)
    try {
      const { data } = await api.put<UserResponse>('/profile', { name })
      setUser(data)
      setNameStatus({ tone: 'ok', text: 'Name updated.' })
    } catch (err) {
      setNameStatus({ tone: 'error', text: errorMessage(err, 'Could not update your name.') })
    } finally {
      setSavingName(false)
    }
  }

  const handlePassword = async (event: FormEvent) => {
    event.preventDefault()
    if (mismatch) return

    setPasswordStatus(null)
    setSavingPassword(true)
    try {
      await api.put('/profile/password', { currentPassword, newPassword, confirmPassword })
      setCurrentPassword('')
      setNewPassword('')
      setConfirmPassword('')
      setPasswordStatus({
        tone: 'ok',
        // MVP.md 12 records this as an accepted limitation, so it is stated
        // rather than hidden.
        text: 'Password changed. Sessions already signed in elsewhere stay valid until they expire.',
      })
    } catch (err) {
      setPasswordStatus({ tone: 'error', text: errorMessage(err, 'Could not change your password.') })
    } finally {
      setSavingPassword(false)
    }
  }

  return (
    <>
      <PageHeader title="Profile" description="Your account details." />

      <div className="grid max-w-3xl gap-6">
        <section className="rounded-lg border border-line bg-surface">
          <h2 className="border-b border-line px-5 py-3 text-sm font-semibold text-ink">Details</h2>

          <form onSubmit={handleName} className="flex flex-col gap-4 px-5 py-5" noValidate>
            {nameStatus && <Status message={nameStatus} />}

            <Field label="Name" htmlFor="name">
              <Input
                id="name"
                required
                minLength={2}
                maxLength={100}
                value={name}
                onChange={(e) => setName(e.target.value)}
              />
            </Field>

            <Field label="Email" htmlFor="email" hint="Email cannot be changed in this version.">
              <Input id="email" value={user?.email ?? ''} readOnly disabled />
            </Field>

            <div>
              <Button type="submit" variant="primary" loading={savingName}>
                Save
              </Button>
            </div>
          </form>
        </section>

        <section className="rounded-lg border border-line bg-surface">
          <h2 className="border-b border-line px-5 py-3 text-sm font-semibold text-ink">Password</h2>

          <form onSubmit={handlePassword} className="flex flex-col gap-4 px-5 py-5" noValidate>
            {passwordStatus && <Status message={passwordStatus} />}

            <Field label="Current password" htmlFor="currentPassword">
              <Input
                id="currentPassword"
                type="password"
                required
                autoComplete="current-password"
                value={currentPassword}
                onChange={(e) => setCurrentPassword(e.target.value)}
              />
            </Field>

            <Field label="New password" htmlFor="newPassword" hint="At least 8 characters.">
              <Input
                id="newPassword"
                type="password"
                required
                minLength={8}
                autoComplete="new-password"
                value={newPassword}
                onChange={(e) => setNewPassword(e.target.value)}
              />
            </Field>

            <Field
              label="Confirm new password"
              htmlFor="confirmNewPassword"
              error={mismatch ? 'Passwords do not match.' : undefined}
            >
              <Input
                id="confirmNewPassword"
                type="password"
                required
                autoComplete="new-password"
                value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)}
              />
            </Field>

            <div>
              <Button type="submit" variant="primary" loading={savingPassword} disabled={mismatch}>
                Change password
              </Button>
            </div>
          </form>
        </section>
      </div>
    </>
  )
}

interface Message {
  tone: 'ok' | 'error'
  text: string
}

function Status({ message }: { message: Message }) {
  if (message.tone === 'error') return <ErrorNote>{message.text}</ErrorNote>
  return (
    <p role="status" className="rounded-md border border-line-strong bg-canvas px-3 py-2 text-sm text-ink">
      {message.text}
    </p>
  )
}
