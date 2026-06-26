const ALPHA_TOOLTIP = 'Ранняя альфа-версия: возможны ошибки и незавершённые функции'

export function AlphaBadge() {
  return (
    <span
      className="rounded-full bg-brand-100 px-2 py-0.5 text-xs font-semibold uppercase tracking-wide text-brand-700"
      title={ALPHA_TOOLTIP}
      aria-label={ALPHA_TOOLTIP}
    >
      Alpha
    </span>
  )
}
