import JSZip from 'jszip';
import { detectLanguageFromFilename } from '../api/languages';
import {
  createRootNode,
  createFolderNode,
  createFileNode,
  findNodeByPath,
  addChild,
  listAllFilesFlat,
} from './treeModel';

function readFileAsText(file) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => resolve(reader.result || '');
    reader.onerror = () => reject(reader.error);
    reader.readAsText(file);
  });
}

export async function readDirectoryInput(fileList) {
  const files = [];
  const ignored = [];

  for (let i = 0; i < fileList.length; i += 1) {
    const file = fileList[i];
    const isSupported = detectLanguageFromFilename(file.name) !== null;

    if (!isSupported) {
      ignored.push(file.webkitRelativePath || file.name);
      continue;
    }

    const segments = file.webkitRelativePath ? file.webkitRelativePath.split('/') : [file.name];
    const relativePath = segments.length > 1 ? segments.slice(1).join('/') : file.name;

    try {
      const content = await readFileAsText(file);
      files.push({
        name: file.name,
        path: relativePath,
        content,
      });
    } catch (error) {
      console.error(`Failed to read file: ${file.name}`, error);
      ignored.push(file.webkitRelativePath || file.name);
    }
  }

  return { files, ignored };
}

export function buildTreeFromFlatFiles(flatFiles) {
  let root = createRootNode();

  for (const item of flatFiles) {
    const normalizedPath = item.path.replace(/^\/+|\/+$/g, '');
    if (!normalizedPath) continue;

    const parts = normalizedPath.split('/');
    const fileName = parts[parts.length - 1];

    for (let i = 0; i < parts.length - 1; i += 1) {
      const parentPath = parts.slice(0, i).join('/');
      const folderName = parts[i];
      const folderPath = parts.slice(0, i + 1).join('/');

      const existingFolder = findNodeByPath(root, folderPath);
      if (!existingFolder) {
        root = addChild(root, parentPath, createFolderNode(folderName, folderPath));
      }
    }

    const parentPath = parts.slice(0, -1).join('/');
    const fileNode = {
      ...createFileNode(fileName, normalizedPath),
      content: item.content ?? '',
    };
    root = addChild(root, parentPath, fileNode);
  }

  return root;
}

export function downloadFileContent(filename, content) {
  const blob = new Blob([content], { type: 'text/plain;charset=utf-8' });
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = filename;
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  URL.revokeObjectURL(url);
}

// getContent lets the caller include unsaved edits; stripPrefix makes paths relative when a single folder is exported.
export async function downloadProjectAsZip(root, zipFilename = 'project.zip', { getContent, stripPrefix = '' } = {}) {
  const files = listAllFilesFlat(root);
  const zip = new JSZip();

  for (const file of files) {
    const entryPath = stripPrefix ? file.path.slice(stripPrefix.length + 1) : file.path;
    zip.file(entryPath, getContent ? getContent(file.path) : file.content || '');
  }

  const blob = await zip.generateAsync({ type: 'blob' });
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = zipFilename;
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  URL.revokeObjectURL(url);
}

// Reads a project exported by downloadProjectAsZip back into the same flat shape readDirectoryInput returns.
export async function readZipFile(file) {
  const zip = await JSZip.loadAsync(file);
  const files = [];
  const ignored = [];

  for (const entry of Object.values(zip.files)) {
    if (entry.dir) continue;
    if (detectLanguageFromFilename(entry.name) === null) {
      ignored.push(entry.name);
      continue;
    }
    files.push({ name: entry.name.split('/').pop(), path: entry.name, content: await entry.async('string') });
  }

  return { files, ignored };
}
