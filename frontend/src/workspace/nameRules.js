import { MSG } from '../messages'
import { detectLanguageFromFilename } from '../api/languages'
import { findNodeByPath } from './treeModel'
import { joinPath } from './paths'

// Returns an error sentence for an invalid name, or '' when the name can be used inside parentPath.
export function validateName({ name, isFile, parentPath, tree, ownPath }) {
  const trimmed = name.trim()
  if (!trimmed) return MSG.nameEmpty
  if (/[\\/\s]/.test(trimmed)) return MSG.nameInvalidChars
  if (isFile && !detectLanguageFromFilename(trimmed)) return MSG.nameNeedsExtension
  const target = joinPath(parentPath, trimmed)
  if (target !== ownPath && findNodeByPath(tree, target)) return MSG.nameTaken
  return ''
}
