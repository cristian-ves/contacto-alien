import { useState, useRef, useEffect } from 'react';
import { useWorkspace } from './useWorkspace';
import { FileTree } from './FileTree';
import { findNodeByPath, listAllFilesFlat } from './treeModel';
import {
  readDirectoryInput,
  buildTreeFromFlatFiles,
  downloadProjectAsZip,
} from './importExport';
import { detectLanguageFromFilename } from '../api/languages';

export function WorkspacePanel({ onActiveFileChange }) {
  const {
    tree,
    isLoading,
    createFile,
    createFolder,
    remove,
    rename,
    replaceTree,
  } = useWorkspace();

  const [activePath, setActivePath] = useState(null);
  const fileInputRef = useRef(null);

  const activeNode = activePath && tree ? findNodeByPath(tree, activePath) : null;
  const isFile = activeNode?.type === 'file';

  useEffect(() => {
    if (activeNode && isFile) {
      onActiveFileChange?.({
        path: activeNode.path,
        name: activeNode.name,
        content: activeNode.content ?? '',
      });
    } else {
      onActiveFileChange?.(null);
    }
  }, [activeNode, isFile, onActiveFileChange]);

  const handleSelectFile = (path) => {
    setActivePath(path);
  };

  const handleFolderUploadClick = () => {
    if (fileInputRef.current) {
      fileInputRef.current.click();
    }
  };

  const handleFolderChange = async (event) => {
    const fileList = event.target.files;
    if (!fileList || fileList.length === 0) return;

    const currentFiles = listAllFilesFlat(tree);
    if (currentFiles.length > 0) {
      const confirmReplace = window.confirm(
        'Loading a folder will replace all files in the current workspace. Do you want to proceed?'
      );
      if (!confirmReplace) {
        event.target.value = '';
        return;
      }
    }

    try {
      const { files, ignored } = await readDirectoryInput(fileList);
      if (ignored.length > 0) {
        window.alert(
          `Imported ${files.length} file(s). ${ignored.length} file(s) were ignored (unsupported extensions).`
        );
      }
      const newTree = buildTreeFromFlatFiles(files);
      replaceTree(newTree);
      setActivePath(null);
    } catch (error) {
      console.error('Failed to import directory', error);
      window.alert('An error occurred while reading the folder.');
    } finally {
      event.target.value = '';
    }
  };

  const handleDownloadZip = async () => {
    if (!tree) return;
    try {
      await downloadProjectAsZip(tree, 'proyecto.zip');
    } catch (error) {
      console.error('Failed to create project zip', error);
      window.alert('Failed to generate zip file.');
    }
  };

  const handleRootCreateFile = () => {
    const fileName = window.prompt('Enter file name (.z, .y, .pig):');
    if (!fileName || !fileName.trim()) return;

    const trimmed = fileName.trim();
    const language = detectLanguageFromFilename(trimmed);
    if (!language) {
      window.alert('Invalid extension. Supported extensions are: .z (Zetariano), .y (Y?), .pig (Pig Latin).');
      return;
    }

    createFile('', trimmed);
  };

  const handleRootCreateFolder = () => {
    const folderName = window.prompt('Enter folder name:');
    if (!folderName || !folderName.trim()) return;

    createFolder('', folderName.trim());
  };

  return (
    <div className="card h-100 border-secondary-subtle shadow-sm d-flex flex-column">
      <div className="card-header bg-body-tertiary d-flex flex-column gap-2 py-2">
        <div className="d-flex align-items-center justify-content-between">
          <span className="fw-bold small text-uppercase text-secondary">Explorer</span>
          <div className="btn-group btn-group-sm">
            <button
              type="button"
              className="btn btn-outline-secondary btn-sm py-0 px-2"
              title="New file at root"
              onClick={handleRootCreateFile}
            >
              + File
            </button>
            <button
              type="button"
              className="btn btn-outline-secondary btn-sm py-0 px-2"
              title="New folder at root"
              onClick={handleRootCreateFolder}
            >
              + Folder
            </button>
          </div>
        </div>

        <div className="d-flex gap-2">
          <button
            type="button"
            className="btn btn-outline-primary btn-sm flex-fill"
            onClick={handleFolderUploadClick}
          >
            Cargar carpeta
          </button>
          <button
            type="button"
            className="btn btn-outline-success btn-sm flex-fill"
            onClick={handleDownloadZip}
          >
            Descargar proyecto
          </button>
          <input
            type="file"
            ref={fileInputRef}
            style={{ display: 'none' }}
            webkitdirectory="true"
            directory="true"
            multiple
            onChange={handleFolderChange}
          />
        </div>
      </div>

      <div className="card-body p-0 flex-grow-1 overflow-auto" style={{ maxHeight: 'calc(100vh - 160px)', minHeight: '300px' }}>
        {isLoading ? (
          <div className="p-3 text-center text-muted small">Loading workspace...</div>
        ) : (
          <FileTree
            tree={tree}
            activePath={activePath}
            onSelectFile={handleSelectFile}
            onCreateFile={createFile}
            onCreateFolder={createFolder}
            onRename={rename}
            onRemove={remove}
          />
        )}
      </div>
    </div>
  );
}
