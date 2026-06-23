import {Spinner} from './Spinner'

export function LoadingSpinner() {
  return (
    <div className="flex min-h-screen items-center justify-center">
      <Spinner size="lg" />
    </div>
  )
}
