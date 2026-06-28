import i18n from 'i18next'
import {initReactI18next} from 'react-i18next'
import enErrors from '../../../i18n/locales/en/errors.json'
import enUi from '../../../i18n/locales/en/ui.json'
import ruErrors from '../../../i18n/locales/ru/errors.json'
import ruUi from '../../../i18n/locales/ru/ui.json'
import {detectLocale} from './detectLocale'

void i18n.use(initReactI18next).init({
  resources: {
    ru: {ui: ruUi, errors: ruErrors},
    en: {ui: enUi, errors: enErrors},
  },
  lng: detectLocale(),
  fallbackLng: 'ru',
  defaultNS: 'ui',
  ns: ['ui', 'errors'],
  interpolation: {escapeValue: false},
})

export default i18n
