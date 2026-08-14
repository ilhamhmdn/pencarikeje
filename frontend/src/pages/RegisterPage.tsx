import { useState, type FormEvent } from 'react'
import { Link, Navigate, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { errorCode, errorMessage } from '../api/client'
import { Button, ErrorNote, Field, Input } from '../components/ui'
import { AuthShell } from './LoginPage'

export function RegisterPage() {
  const { user, register } = useAuth()
  const navigate = useNavigate()

  const [name, setName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  if (user) return <Navigate to="/dashboard" replace />

  // Checked client-side too, so the mismatch is caught before a round trip.
  const mismatch = confirmPassword.length > 0 && password !== confirmPassword

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault()
    if (mismatch) return

    setError(null)
    setSubmitting(true)
    try {
      await register(name, email, password, confirmPassword)
      navigate('/dashboard', { replace: true })
    } catch (err) {
      setError(
        errorCode(err) === 'EMAIL_ALREADY_EXISTS'
          ? 'That email is already registered. Try signing in instead.'
          : errorMessage(err, 'Could not create your account.'),
      )
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <AuthShell title="Create account" subtitle="Your applications stay private to you.">
      <form onSubmit={handleSubmit} className="flex flex-col gap-4" noValidate>
        {error && <ErrorNote>{error}</ErrorNote>}

        <Field label="Name" htmlFor="name">
          <Input
            id="name"
            required
            minLength={2}
            maxLength={100}
            autoComplete="name"
            value={name}
            onChange={(e) => setName(e.target.value)}
          />
        </Field>

        <Field label="Email" htmlFor="email">
          <Input
            id="email"
            type="email"
            required
            autoComplete="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
          />
        </Field>

        <Field label="Password" htmlFor="password" hint="At least 8 characters.">
          <Input
            id="password"
            type="password"
            required
            minLength={8}
            autoComplete="new-password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
          />
        </Field>

        <Field
          label="Confirm password"
          htmlFor="confirmPassword"
          error={mismatch ? 'Passwords do not match.' : undefined}
        >
          <Input
            id="confirmPassword"
            type="password"
            required
            autoComplete="new-password"
            value={confirmPassword}
            onChange={(e) => setConfirmPassword(e.target.value)}
          />
        </Field>

        <Button type="submit" variant="primary" loading={submitting} disabled={mismatch} className="mt-2">
          Create account
        </Button>
      </form>

      <p className="mt-6 text-center text-sm text-muted">
        Already have an account?{' '}
        <Link to="/login" className="font-semibold text-accent hover:text-accent-hover">
          Sign in
        </Link>
      </p>
    </AuthShell>
  )
}
