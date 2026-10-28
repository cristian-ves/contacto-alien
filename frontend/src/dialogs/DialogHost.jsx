import { ConfirmDialog } from './ConfirmDialog'
import { findNodeByPath } from '../workspace/treeModel'
import { basename } from '../workspace/paths'
import { MSG } from '../messages'

// Renders whichever dialog the shell asked for. dialog is { kind, ...payload } or null.
export function DialogHost({ dialog, ide, onClose }) {
  if (!dialog) return null
  const { tree, actions } = ide

  const done = (action) => () => {
    onClose()
    action()
  }

  switch (dialog.kind) {
    case 'delete':
      return (
        <ConfirmDialog
          title={MSG.deleteTitle(basename(dialog.path))}
          body={MSG.deleteBody(findNodeByPath(tree, dialog.path)?.type === 'folder')}
          confirmLabel="Delete"
          onClose={onClose}
          onConfirm={done(() => actions.remove(dialog.path))}
        />
      )
    case 'discard':
      return (
        <ConfirmDialog
          title={MSG.discardTitle}
          body={MSG.discardBody(basename(dialog.path))}
          confirmLabel="Discard changes"
          secondary={{
            label: 'Save',
            run: done(() => {
              actions.save(dialog.path)
              actions.closeTab(dialog.path)
            }),
          }}
          onClose={onClose}
          onConfirm={done(() => actions.closeTab(dialog.path))}
        />
      )
    case 'replace':
      return (
        <ConfirmDialog
          title={MSG.replaceTitle}
          body={MSG.replaceBody}
          confirmLabel="Replace workspace"
          onClose={onClose}
          onConfirm={done(() => actions.replaceWorkspace(dialog.result))}
        />
      )
    case 'saveProject':
      return (
        <ConfirmDialog
          title={MSG.saveProjectToDisk}
          body={MSG.saveProjectBody}
          confirmLabel={MSG.saveProjectChoose}
          destructive={false}
          onClose={onClose}
          onConfirm={done(actions.saveProjectToDisk)}
        />
      )
    default:
      return null
  }
}
