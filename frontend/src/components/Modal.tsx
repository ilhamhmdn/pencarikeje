import { useEffect, useRef, type ReactNode } from 'react'
import { Button } from './ui'

/**
 * Functional depth only: the modal sits above content by position, not by
 * imitating physical elevation. Closes on the X, on ESC and on outside click,
 * so every modal in the app behaves identically.
 */
export function Modal({
  open,
  title,
  onClose,
  children,
  footer,
  wide = false,
}: {
  open: boolean
  title: string
  onClose: () => void
  children: ReactNode
  footer?: ReactNode
  wide?: boolean
}) {
  const panelRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (!open) return

    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') onClose()
    }
    document.addEventListener('keydown', onKeyDown)

    // Prevent the page behind from scrolling while the dialog owns focus.
    const previousOverflow = document.body.style.overflow
    document.body.style.overflow = 'hidden'

    panelRef.current?.focus()

    return () => {
      document.removeEventListener('keydown', onKeyDown)
      document.body.style.overflow = previousOverflow
    }
  }, [open, onClose])

  if (!open) return null

  return (
    <div
      className="fixed inset-0 z-50 flex items-start justify-center overflow-y-auto bg-ink/25 p-4 sm:items-center"
      onMouseDown={(event) => {
        if (event.target === event.currentTarget) onClose()
      }}
    >
      <div
        ref={panelRef}
        tabIndex={-1}
        role="dialog"
        aria-modal="true"
        aria-label={title}
        className={`w-full ${wide ? 'max-w-3xl' : 'max-w-lg'} rounded-lg border border-line bg-surface`}
      >
        <header className="flex items-center justify-between gap-4 border-b border-line px-5 py-4">
          <h2 className="text-lg font-semibold tracking-tight text-ink">{title}</h2>
          <Button variant="ghost" onClick={onClose} aria-label="Close" className="px-2">
            <svg viewBox="0 0 20 20" className="size-4" fill="none" stroke="currentColor" strokeWidth="2">
              <path d="M5 5l10 10M15 5L5 15" strokeLinecap="round" />
            </svg>
          </Button>
        </header>

        <div className="px-5 py-5">{children}</div>

        {footer && (
          <footer className="flex justify-end gap-2 border-t border-line px-5 py-4">{footer}</footer>
        )}
      </div>
    </div>
  )
}
