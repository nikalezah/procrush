import {StrictMode} from 'react'
import {createRoot} from 'react-dom/client'
import App from './App'
import {ThemeProvider} from './hooks/useTheme'
import {applyDocumentLocale, detectLocale} from './i18n/detectLocale'
import './i18n/config'
import './index.css'

applyDocumentLocale(detectLocale())

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <ThemeProvider>
      <App />
    </ThemeProvider>
  </StrictMode>,
)
