export const LANGUAGES = [
  { id: 'zetariano', extension: '.z', label: 'Zetariano' },
  { id: 'y', extension: '.y', label: 'Y?' },
  { id: 'pig', extension: '.pig', label: 'Pig Latin' },
]

export function detectLanguageFromFilename(filename) {
  if (!filename || typeof filename !== 'string') {
    return null
  }

  const lowerName = filename.toLowerCase()
  const match = LANGUAGES.find((lang) => lowerName.endsWith(lang.extension))
  return match ? match.id : null
}
