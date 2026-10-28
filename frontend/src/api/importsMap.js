import { listAllFilesFlat } from '../workspace/treeModel'
import { dirname, isInside } from '../workspace/paths'
import { detectLanguageFromFilename } from './languages'

// Folder that every key of the virtual-file map is relative to: the folder of the .pig for a Pig program, and the
// project root for a Zetariano class (nearest ancestor folder holding a .pig, or its own folder when there is none).
// Same convention the backend uses for the path field of /api/validate: a .z is checked with every .z of its project.
function baseFolderFor(files, activePath, language) {
  const own = dirname(activePath)
  if (language !== 'zetariano') return own
  let folder = own
  for (;;) {
    if (files.some((file) => dirname(file.path) === folder && detectLanguageFromFilename(file.path) === 'pig')) return folder
    if (folder === '') return own
    folder = dirname(folder)
  }
}

function relativeTo(base, path) {
  return base === '' ? path : path.slice(base.length + 1)
}

// What the backend needs next to the code of the active file:
// - imports: virtual files keyed relative to base (a .pig resolves "import lib.Util.y" to "lib/Util.y" in it);
// - path: where a Zetariano class sits inside that map, so the backend knows its package (its folder);
// - base: the workspace folder the keys are relative to, used to map error prefixes back to workspace files.
// A .y file needs none of them. The active file is never part of the map, and unsaved drafts win over saved text.
export function buildImportContext(tree, activePath, getContent) {
  const language = detectLanguageFromFilename(activePath)
  if (language !== 'pig' && language !== 'zetariano') return { imports: undefined, path: undefined, base: '' }

  const files = listAllFilesFlat(tree)
  const base = baseFolderFor(files, activePath, language)
  const wanted = language === 'pig' ? ['y', 'zetariano'] : ['zetariano']
  const imports = {}
  for (const file of files) {
    if (file.path === activePath || !isInside(file.path, base)) continue
    if (wanted.includes(detectLanguageFromFilename(file.path))) imports[relativeTo(base, file.path)] = getContent(file.path)
  }
  return { imports, path: language === 'zetariano' ? relativeTo(base, activePath) : undefined, base }
}

// Number of files that travel with a request, shown in the "Compiling ... with N files" line.
export function countImports(imports) {
  return imports ? Object.keys(imports).length : 0
}
