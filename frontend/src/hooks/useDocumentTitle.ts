import {useEffect} from 'react'
import {useTranslation} from 'react-i18next'

export function useDocumentTitle() {
  const {t, i18n} = useTranslation()

  useEffect(() => {
    const apply = () => {
      document.documentElement.lang = i18n.language
      document.title = t('app.documentTitle')
    }
    apply()
    i18n.on('languageChanged', apply)
    return () => {
      i18n.off('languageChanged', apply)
    }
  }, [i18n, t])
}
