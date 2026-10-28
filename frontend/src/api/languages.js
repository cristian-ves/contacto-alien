import { MSG } from '../messages'

export const LANGUAGES = [
  { id: 'zetariano', extension: '.z', label: MSG.languageNames.zetariano, badge: 'Z', tone: 'z' },
  { id: 'y', extension: '.y', label: MSG.languageNames.y, badge: 'Y', tone: 'y' },
  { id: 'pig', extension: '.pig', label: MSG.languageNames.pig, badge: 'P', tone: 'pig' },
]

export function detectLanguageFromFilename(filename) {
  if (!filename || typeof filename !== 'string') {
    return null
  }

  const lowerName = filename.toLowerCase()
  const match = LANGUAGES.find((lang) => lowerName.endsWith(lang.extension))
  return match ? match.id : null
}

export function languageById(id) {
  return LANGUAGES.find((lang) => lang.id === id) ?? null
}
