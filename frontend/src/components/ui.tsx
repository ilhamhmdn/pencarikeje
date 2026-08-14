import type { ButtonHTMLAttributes, InputHTMLAttributes, ReactNode, TextareaHTMLAttributes } from 'react'

/* Small shared primitives. Hierarchy comes from scale, weight and spacing —
 * there are no drop shadows outside the modal layer, and containers are defined
 * by hairline borders against a warm canvas. */

export function cx(...parts: (string | false | null | undefined)[]) {
  return parts.filter(Boolean).join(' ')
}

type ButtonProps = ButtonHTMLAttributes<HTMLButtonElement> & {
  variant?: 'primary' | 'secondary' | 'ghost' | 'danger'
  loading?: boolean
}

export function Button({
  variant = 'secondary',
  loading = false,
  className,
  children,
  disabled,
  ...rest
}: ButtonProps) {
  const base =
    'inline-flex items-center justify-center gap-2 rounded-md px-4 min-h-[2.5rem] text-sm font-semibold transition-colors duration-150 disabled:opacity-50 disabled:pointer-events-none'

  const variants = {
    primary: 'bg-accent text-white hover:bg-accent-hover',
    secondary: 'border border-line-strong bg-surface text-ink hover:bg-canvas',
    ghost: 'text-muted hover:text-ink hover:bg-canvas',
    danger: 'border border-line-strong text-negative hover:bg-negative hover:text-white',
  }

  return (
    <button className={cx(base, variants[variant], className)} disabled={disabled || loading} {...rest}>
      {loading && <Spinner />}
      {children}
    </button>
  )
}

export function Spinner({ className }: { className?: string }) {
  return (
    <span
      role="status"
      aria-label="Loading"
      className={cx(
        'inline-block size-4 animate-spin rounded-full border-2 border-current border-t-transparent',
        className,
      )}
    />
  )
}

export function Field({
  label,
  htmlFor,
  error,
  hint,
  children,
}: {
  label: string
  htmlFor: string
  error?: string
  hint?: string
  children: ReactNode
}) {
  return (
    <div className="flex flex-col gap-1.5">
      <label htmlFor={htmlFor} className="text-sm font-medium text-ink">
        {label}
      </label>
      {children}
      {hint && !error && <p className="text-xs text-muted">{hint}</p>}
      {error && (
        <p className="text-xs text-negative" role="alert">
          {error}
        </p>
      )}
    </div>
  )
}

const controlStyles =
  'w-full rounded-md border border-line-strong bg-surface px-3 py-2 text-base text-ink placeholder:text-faint transition-colors duration-150 hover:border-muted focus:border-accent'

export function Input({ className, ...rest }: InputHTMLAttributes<HTMLInputElement>) {
  return <input className={cx(controlStyles, className)} {...rest} />
}

export function Textarea({ className, ...rest }: TextareaHTMLAttributes<HTMLTextAreaElement>) {
  return <textarea className={cx(controlStyles, 'resize-y leading-relaxed', className)} {...rest} />
}

export function Select({
  className,
  children,
  ...rest
}: InputHTMLAttributes<HTMLSelectElement> & { children: ReactNode }) {
  return (
    <select className={cx(controlStyles, className)} {...rest}>
      {children}
    </select>
  )
}

/**
 * Status is shown as a dot plus its name. Colour never carries the meaning on
 * its own, so the label stays readable for colour-blind users (WCAG 1.4.1).
 */
export function StatusDot({ code }: { code: string }) {
  const tone = statusTone(code)
  return <span aria-hidden className={cx('inline-block size-2 shrink-0 rounded-full', tone)} />
}

export function statusTone(code: string): string {
  if (code === 'OFFER' || code === 'ACCEPTED') return 'bg-positive'
  if (code === 'REJECTED' || code === 'WITHDRAWN') return 'bg-negative'
  if (code.includes('INTERVIEW')) return 'bg-accent'
  return 'bg-neutralish'
}

export function StatusLabel({ code, name }: { code: string; name: string }) {
  return (
    <span className="inline-flex items-center gap-2 whitespace-nowrap">
      <StatusDot code={code} />
      <span>{name}</span>
    </span>
  )
}

export function ErrorNote({ children }: { children: ReactNode }) {
  return (
    <p role="alert" className="rounded-md border border-line-strong bg-accent-soft px-3 py-2 text-sm text-ink">
      {children}
    </p>
  )
}

export function EmptyState({ title, children }: { title: string; children?: ReactNode }) {
  return (
    <div className="flex flex-col items-center gap-2 px-6 py-16 text-center">
      <p className="text-base font-semibold text-ink">{title}</p>
      {children && <div className="max-w-sm text-sm text-muted">{children}</div>}
    </div>
  )
}

/** Skeleton rows, so a slow list does not flash an empty state first. */
export function SkeletonRows({ rows = 5, className }: { rows?: number; className?: string }) {
  return (
    <div className={cx('flex flex-col gap-2 p-4', className)} aria-hidden>
      {Array.from({ length: rows }).map((_, index) => (
        <div key={index} className="h-8 animate-pulse rounded bg-canvas" />
      ))}
    </div>
  )
}
