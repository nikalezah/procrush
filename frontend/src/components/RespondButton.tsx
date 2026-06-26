import type {InterestStatus} from '../api/types'

interface RespondButtonProps {
  status: InterestStatus | undefined
  loading?: boolean
  onRespond: () => void
}

export function RespondButton({status, loading = false, onRespond}: RespondButtonProps) {
  const resolved = status ?? 'NONE'
  const canRespond = resolved === 'NONE' || resolved === 'INCOMING'
  const disabled = !canRespond || loading

  let label = '❤️ Откликнуться'
  if (loading) {
    label = 'Отправка…'
  } else if (resolved === 'RESPONDED') {
    label = '⏳ Ожидание ответа'
  } else if (resolved === 'MUTUAL') {
    label = '💕 Взаимный интерес!'
  }

  return (
    <button
      type="button"
      disabled={disabled}
      onClick={onRespond}
      className={
        disabled
          ? 'rounded-full bg-stone-100 px-4 py-2 text-sm text-stone-400'
          : 'gradient-brand rounded-full px-5 py-2 text-sm font-semibold text-white shadow-md shadow-brand-500/25 transition hover:brightness-110 active:scale-95'
      }
    >
      {label}
    </button>
  )
}
