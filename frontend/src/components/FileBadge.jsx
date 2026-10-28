import { detectLanguageFromFilename, languageById } from '../api/languages'

export function FileBadge({ path }) {
  const language = languageById(detectLanguageFromFilename(path))
  if (!language) return <span className="ft" aria-hidden="true" />
  return (
    <span className={`ft ${language.tone}`} aria-hidden="true">
      {language.badge}
    </span>
  )
}
