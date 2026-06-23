import type {InterestStatus} from '../api/types'

interface RespondButtonProps {
  status: InterestStatus | undefined
  loading?: boolean
  onRespond: () => void
}

export function RespondButton({ status, loading = false, onRespond }: RespondButtonProps) {
  const resolved = status ?? 'NONE'
  const canRespond = resolved === 'NONE' || resolved === 'INCOMING'
  const disabled = !canRespond || loading

  let label = 'Откликнуться'
  if (loading) {
    label = 'Отправка…'
  } else if (resolved === 'RESPONDED') {
    label = 'Ожидание ответа'
  } else if (resolved === 'MUTUAL') {
    label = 'Взаимный интерес'
  }

  return (
    <button
      type="button"
      disabled={disabled}
      onClick={onRespond}
      className={
        disabled
          ? 'rounded-lg bg-neutral-200 px-3 py-1.5 text-sm text-neutral-500'
          : 'rounded-lg bg-neutral-900 px-3 py-1.5 text-sm font-medium text-white hover:bg-neutral-800'
      }
    >
      {label}
    </button>
  )
}
