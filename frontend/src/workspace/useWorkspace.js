import { useState, useEffect, useCallback, useRef } from 'react';
import {
  createRootNode,
  createFileNode,
  createFolderNode,
  addChild,
  removeNode,
  renameNode,
  updateFileContent,
  upsertFile,
} from './treeModel';
import { loadTree, saveTree } from './persistence';

export function useWorkspace() {
  const [tree, setTree] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const [persistFailed, setPersistFailed] = useState(false);
  const isInitialLoadRef = useRef(true);

  useEffect(() => {
    let isMounted = true;

    async function initialize() {
      try {
        const savedTree = await loadTree();
        if (isMounted) {
          setTree(savedTree ?? createRootNode());
          setIsLoading(false);
        }
      } catch (error) {
        console.error('Failed to load workspace tree from IndexedDB', error);
        if (isMounted) {
          setTree(createRootNode());
          setIsLoading(false);
        }
      }
    }

    initialize();

    return () => {
      isMounted = false;
    };
  }, []);

  useEffect(() => {
    if (isLoading || tree === null) return;

    if (isInitialLoadRef.current) {
      isInitialLoadRef.current = false;
      return;
    }

    const timeoutId = setTimeout(() => {
      saveTree(tree)
        .then(() => setPersistFailed(false))
        .catch((error) => {
          console.error('Failed to persist workspace tree', error);
          setPersistFailed(true);
        });
    }, 300);

    return () => {
      clearTimeout(timeoutId);
    };
  }, [tree, isLoading]);

  const createFile = useCallback((parentPath, name) => {
    const filePath = parentPath ? `${parentPath}/${name}` : name;
    const fileNode = createFileNode(name, filePath);
    setTree((prevTree) => (prevTree ? addChild(prevTree, parentPath, fileNode) : prevTree));
  }, []);

  const createFolder = useCallback((parentPath, name) => {
    const folderPath = parentPath ? `${parentPath}/${name}` : name;
    const folderNode = createFolderNode(name, folderPath);
    setTree((prevTree) => (prevTree ? addChild(prevTree, parentPath, folderNode) : prevTree));
  }, []);

  const remove = useCallback((path) => {
    setTree((prevTree) => (prevTree ? removeNode(prevTree, path) : prevTree));
  }, []);

  const rename = useCallback((path, newName) => {
    setTree((prevTree) => (prevTree ? renameNode(prevTree, path, newName) : prevTree));
  }, []);

  const updateContent = useCallback((path, newContent) => {
    setTree((prevTree) => (prevTree ? updateFileContent(prevTree, path, newContent) : prevTree));
  }, []);

  // Adds or overwrites files under baseFolder, creating the folders they need.
  const upsertFiles = useCallback((files, baseFolder = '') => {
    setTree((prevTree) => {
      if (!prevTree) return prevTree;
      return files.reduce((acc, file) => {
        const target = baseFolder ? `${baseFolder}/${file.path}` : file.path;
        return upsertFile(acc, target, file.content);
      }, prevTree);
    });
  }, []);

  const replaceTree = useCallback((newTree) => {
    setTree(newTree);
  }, []);

  return {
    tree,
    isLoading,
    persistFailed,
    createFile,
    createFolder,
    remove,
    rename,
    updateContent,
    upsertFiles,
    replaceTree,
  };
}
