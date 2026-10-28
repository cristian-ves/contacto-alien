import { downloadFileContent } from '../../workspace/importExport'
import { MSG } from '../../messages'

// Copy and download shared by the listings; both report through the toast so the user sees the result.
export async function copyText(text, name, notify) {
  try {
    await navigator.clipboard.writeText(text)
    notify(MSG.copied(name))
  } catch {
    notify(MSG.copyFailed, 'error')
  }
}

export function saveText(filename, text, notify) {
  downloadFileContent(filename, text)
  notify(MSG.downloaded(filename))
}
