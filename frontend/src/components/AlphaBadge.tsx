const ALPHA_TOOLTIP =
  'Early alpha: bugs and incomplete features are expected'

export function AlphaBadge() {
  return (
    <span
      className="rounded-full bg-amber-100 px-2 py-0.5 text-xs font-semibold uppercase tracking-wide text-amber-900"
      title={ALPHA_TOOLTIP}
      aria-label={ALPHA_TOOLTIP}
    >
      Alpha
    </span>
  )
}
