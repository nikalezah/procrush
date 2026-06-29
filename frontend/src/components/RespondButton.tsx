import {useTranslation} from 'react-i18next'
import type {InterestStatus} from '../api/types'

interface RespondButtonProps {
  status: InterestStatus | undefined
  loading?: boolean
  onRespond: () => void
}

export function RespondButton({status, loading = false, onRespond}: RespondButtonProps) {
  const {t} = useTranslation()
  const resolved = status ?? 'NONE'
  if (resolved === 'MUTUAL') return null

  const canRespond = resolved === 'NONE' || resolved === 'INCOMING'
  const disabled = !canRespond || loading

  let label = t('components.respondButton.respond')
  if (loading) {
    label = t('components.respondButton.sending')
  } else if (resolved === 'RESPONDED') {
    label = t('components.respondButton.waiting')
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
