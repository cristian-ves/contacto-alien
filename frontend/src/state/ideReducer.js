import { rebasePath } from '../workspace/paths'

export const initialIdeState = {
  openPaths: [],
  activePath: null,
  selectedPath: '',
  drafts: {},
  // Disk file handles (File System Access API) by workspace path; memory only, like the drafts.
  handles: {},
  expanded: { '': true },
  results: {},
  cursor: { line: 1, column: 1 },
  reveal: null,
  options: { validateOnType: true, closeBrackets: false },
}

function mapKeys(object, oldPath, newPath) {
  const next = {}
  for (const [key, value] of Object.entries(object)) {
    next[rebasePath(key, oldPath, newPath) ?? key] = value
  }
  return next
}

function dropUnder(object, path) {
  const next = {}
  for (const [key, value] of Object.entries(object)) {
    if (rebasePath(key, path, '') === null) next[key] = value
  }
  return next
}

function pickNextActive(openPaths, closedIndex) {
  return openPaths[Math.min(closedIndex, openPaths.length - 1)] ?? null
}

export function ideReducer(state, action) {
  switch (action.type) {
    case 'open': {
      const openPaths = state.openPaths.includes(action.path) ? state.openPaths : [...state.openPaths, action.path]
      return { ...state, openPaths, activePath: action.path, selectedPath: action.path }
    }
    case 'activate':
      return { ...state, activePath: action.path, selectedPath: action.path }
    case 'close': {
      const index = state.openPaths.indexOf(action.path)
      if (index === -1) return state
      const openPaths = state.openPaths.filter((path) => path !== action.path)
      const { [action.path]: _discarded, ...drafts } = state.drafts
      const activePath = state.activePath === action.path ? pickNextActive(openPaths, index) : state.activePath
      return { ...state, openPaths, drafts, activePath }
    }
    case 'select':
      return { ...state, selectedPath: action.path }
    case 'edit':
      return { ...state, drafts: { ...state.drafts, [action.path]: action.text } }
    case 'saved': {
      const { [action.path]: _saved, ...drafts } = state.drafts
      return { ...state, drafts }
    }
    case 'link':
      return { ...state, handles: { ...state.handles, ...action.handles } }
    case 'toggle':
      return { ...state, expanded: { ...state.expanded, [action.path]: !state.expanded[action.path] } }
    case 'expand':
      return { ...state, expanded: { ...state.expanded, [action.path]: true } }
    case 'result':
      return { ...state, results: { ...state.results, [action.path]: action.result } }
    case 'renamed': {
      const rebase = (path) => rebasePath(path, action.oldPath, action.newPath) ?? path
      return {
        ...state,
        openPaths: state.openPaths.map(rebase),
        activePath: state.activePath && rebase(state.activePath),
        selectedPath: rebase(state.selectedPath),
        drafts: mapKeys(state.drafts, action.oldPath, action.newPath),
        handles: dropUnder(state.handles, action.oldPath),
        results: mapKeys(state.results, action.oldPath, action.newPath),
        expanded: mapKeys(state.expanded, action.oldPath, action.newPath),
      }
    }
    case 'removed': {
      const gone = (path) => rebasePath(path, action.path, '') !== null
      const openPaths = state.openPaths.filter((path) => !gone(path))
      const activePath =
        state.activePath && gone(state.activePath)
          ? pickNextActive(openPaths, state.openPaths.indexOf(state.activePath))
          : state.activePath
      return {
        ...state,
        openPaths,
        activePath,
        selectedPath: gone(state.selectedPath) ? '' : state.selectedPath,
        drafts: dropUnder(state.drafts, action.path),
        handles: dropUnder(state.handles, action.path),
        results: dropUnder(state.results, action.path),
      }
    }
    case 'cursor':
      return { ...state, cursor: action.cursor }
    case 'reveal':
      return { ...state, reveal: { path: action.path, line: action.line, column: action.column, nonce: Date.now() } }
    case 'option':
      return { ...state, options: { ...state.options, [action.name]: action.value } }
    case 'reset':
      return { ...initialIdeState, options: state.options }
    default:
      return state
  }
}
