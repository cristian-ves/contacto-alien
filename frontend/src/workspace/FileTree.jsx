import { useState } from 'react';
import { detectLanguageFromFilename } from '../api/languages';

function ChevronDownIcon() {
  return (
    <svg width="12" height="12" viewBox="0 0 16 16" fill="currentColor" className="me-1">
      <path fillRule="evenodd" d="M1.646 4.646a.5.5 0 0 1 .708 0L8 10.293l5.646-5.647a.5.5 0 0 1 .708.708l-6 6a.5.5 0 0 1-.708 0l-6-6a.5.5 0 0 1 0-.708z" />
    </svg>
  );
}

function ChevronRightIcon() {
  return (
    <svg width="12" height="12" viewBox="0 0 16 16" fill="currentColor" className="me-1">
      <path fillRule="evenodd" d="M4.646 1.646a.5.5 0 0 1 .708 0l6 6a.5.5 0 0 1 0 .708l-6 6a.5.5 0 0 1-.708-.708L10.293 8 4.646 2.354a.5.5 0 0 1 0-.708z" />
    </svg>
  );
}

function FolderIcon() {
  return (
    <svg width="14" height="14" viewBox="0 0 16 16" fill="currentColor" className="me-2 text-warning">
      <path d="M.54 3.87.5 3a2 2 0 0 1 2-2h3.672a2 2 0 0 1 1.414.586l.828.828A2 2 0 0 0 9.828 3h4.672a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2H2.5a2 2 0 0 1-2-2V3.87z" />
    </svg>
  );
}

function FileIcon() {
  return (
    <svg width="14" height="14" viewBox="0 0 16 16" fill="currentColor" className="me-2 text-secondary">
      <path d="M14 4.5V14a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V2a2 2 0 0 1 2-2h5.5L14 4.5zm-3 0A1.5 1.5 0 0 1 9.5 3V1H4a1 1 0 0 0-1 1v12a1 1 0 0 0 1 1h8a1 1 0 0 0 1-1V4.5h-2z" />
    </svg>
  );
}

export function FileTree({
  tree,
  activePath,
  onSelectFile,
  onCreateFile,
  onCreateFolder,
  onRename,
  onRemove,
}) {
  const [collapsedPaths, setCollapsedPaths] = useState({});

  const toggleFolder = (path) => {
    setCollapsedPaths((prev) => ({
      ...prev,
      [path]: !prev[path],
    }));
  };

  const handleCreateFile = (parentPath) => {
    const fileName = window.prompt('Enter file name (.z, .y, .pig):');
    if (!fileName || !fileName.trim()) return;

    const trimmed = fileName.trim();
    const language = detectLanguageFromFilename(trimmed);
    if (!language) {
      window.alert('Invalid extension. Supported extensions are: .z (Zetariano), .y (Y?), .pig (Pig Latin).');
      return;
    }

    onCreateFile(parentPath, trimmed);
  };

  const handleCreateFolder = (parentPath) => {
    const folderName = window.prompt('Enter folder name:');
    if (!folderName || !folderName.trim()) return;

    onCreateFolder(parentPath, folderName.trim());
  };

  const handleRename = (node) => {
    const newName = window.prompt('Enter new name:', node.name);
    if (!newName || !newName.trim() || newName.trim() === node.name) return;

    const trimmed = newName.trim();
    if (node.type === 'file') {
      const language = detectLanguageFromFilename(trimmed);
      if (!language) {
        window.alert('Invalid extension. Supported extensions are: .z (Zetariano), .y (Y?), .pig (Pig Latin).');
        return;
      }
    }

    onRename(node.path, trimmed);
  };

  const handleRemove = (node) => {
    const confirmed = window.confirm(`Are you sure you want to delete "${node.name || node.path}"?`);
    if (confirmed) {
      onRemove(node.path);
    }
  };

  const renderNode = (node, depth = 0) => {
    if (node.type === 'folder') {
      const isCollapsed = Boolean(collapsedPaths[node.path]);
      const paddingLeft = `${depth * 14 + 10}px`;

      return (
        <div key={node.id || node.path}>
          <div
            className="list-group-item list-group-item-action d-flex align-items-center justify-content-between py-1 px-2 border-0 rounded-0"
            style={{ paddingLeft, cursor: 'pointer', userSelect: 'none' }}
            onClick={() => toggleFolder(node.path)}
          >
            <div className="d-flex align-items-center text-truncate me-2">
              <span className="d-inline-flex align-items-center">
                {isCollapsed ? <ChevronRightIcon /> : <ChevronDownIcon />}
              </span>
              <FolderIcon />
              <span className="fw-semibold text-truncate small">{node.name}</span>
            </div>

            <div className="btn-group btn-group-sm opacity-75" onClick={(e) => e.stopPropagation()}>
              <button
                type="button"
                className="btn btn-sm btn-link p-0 text-decoration-none text-muted px-1"
                title="New file"
                onClick={() => handleCreateFile(node.path)}
              >
                +F
              </button>
              <button
                type="button"
                className="btn btn-sm btn-link p-0 text-decoration-none text-muted px-1"
                title="New folder"
                onClick={() => handleCreateFolder(node.path)}
              >
                +D
              </button>
              <button
                type="button"
                className="btn btn-sm btn-link p-0 text-decoration-none text-muted px-1"
                title="Rename"
                onClick={() => handleRename(node)}
              >
                Ren
              </button>
              <button
                type="button"
                className="btn btn-sm btn-link p-0 text-decoration-none text-danger px-1"
                title="Delete"
                onClick={() => handleRemove(node)}
              >
                Del
              </button>
            </div>
          </div>

          {!isCollapsed && Array.isArray(node.children) && (
            <div>{node.children.map((child) => renderNode(child, depth + 1))}</div>
          )}
        </div>
      );
    }

    const isSelected = activePath === node.path;
    const paddingLeft = `${depth * 14 + 22}px`;

    return (
      <div
        key={node.id || node.path}
        className={`list-group-item list-group-item-action d-flex align-items-center justify-content-between py-1 px-2 border-0 rounded-0 ${
          isSelected ? 'active' : ''
        }`}
        style={{ paddingLeft, cursor: 'pointer', userSelect: 'none' }}
        onClick={() => onSelectFile(node.path)}
      >
        <div className="d-flex align-items-center text-truncate me-2">
          <FileIcon />
          <span className="text-truncate small">{node.name}</span>
        </div>

        <div className="btn-group btn-group-sm opacity-75" onClick={(e) => e.stopPropagation()}>
          <button
            type="button"
            className={`btn btn-sm btn-link p-0 text-decoration-none px-1 ${
              isSelected ? 'text-white' : 'text-muted'
            }`}
            title="Rename"
            onClick={() => handleRename(node)}
          >
            Ren
          </button>
          <button
            type="button"
            className={`btn btn-sm btn-link p-0 text-decoration-none px-1 ${
              isSelected ? 'text-white' : 'text-danger'
            }`}
            title="Delete"
            onClick={() => handleRemove(node)}
          >
            Del
          </button>
        </div>
      </div>
    );
  };

  if (!tree || !Array.isArray(tree.children) || tree.children.length === 0) {
    return (
      <div className="p-3 text-center text-muted small">
        No files in workspace.
      </div>
    );
  }

  return (
    <div className="list-group list-group-flush">
      {tree.children.map((child) => renderNode(child, 0))}
    </div>
  );
}
