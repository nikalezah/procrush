export function LoadingSpinner() {
  return (
    <div className="flex min-h-screen items-center justify-center">
      <div
        className="h-10 w-10 animate-spin rounded-full border-4 border-neutral-300 border-t-neutral-900"
        role="status"
        aria-label="Загрузка"
      />
    </div>
  )
}
