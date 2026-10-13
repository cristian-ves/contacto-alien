export function createRootNode() {
  return {
    id: crypto.randomUUID(),
    name: '',
    type: 'folder',
    path: '',
    content: null,
    children: [],
  };
}

export function createFileNode(name, path) {
  return {
    id: crypto.randomUUID(),
    name,
    type: 'file',
    path,
    content: '',
    children: null,
  };
}

export function createFolderNode(name, path) {
  return {
    id: crypto.randomUUID(),
    name,
    type: 'folder',
    path,
    content: null,
    children: [],
  };
}

export function findNodeByPath(root, path) {
  if (!root) return null;
  if (root.path === path) return root;

  if (root.type === 'folder' && Array.isArray(root.children)) {
    for (const child of root.children) {
      const found = findNodeByPath(child, path);
      if (found) return found;
    }
  }

  return null;
}

export function addChild(root, parentPath, childNode) {
  if (!root) return root;

  if (root.path === parentPath) {
    if (root.type !== 'folder') return root;
    return {
      ...root,
      children: [...(root.children || []), childNode],
    };
  }

  if (root.type === 'folder' && Array.isArray(root.children)) {
    return {
      ...root,
      children: root.children.map((child) => addChild(child, parentPath, childNode)),
    };
  }

  return root;
}

export function removeNode(root, path) {
  if (!root || root.path === path) return root;

  if (root.type === 'folder' && Array.isArray(root.children)) {
    return {
      ...root,
      children: root.children
        .filter((child) => child.path !== path)
        .map((child) => removeNode(child, path)),
    };
  }

  return root;
}

function updateNodePaths(node, oldPrefix, newPrefix) {
  const isExactMatch = node.path === oldPrefix;
  const isDescendant = node.path.startsWith(`${oldPrefix}/`);

  let updatedPath = node.path;
  if (isExactMatch) {
    updatedPath = newPrefix;
  } else if (isDescendant) {
    updatedPath = `${newPrefix}${node.path.slice(oldPrefix.length)}`;
  }

  if (node.type === 'folder' && Array.isArray(node.children)) {
    return {
      ...node,
      path: updatedPath,
      children: node.children.map((child) => updateNodePaths(child, oldPrefix, newPrefix)),
    };
  }

  return {
    ...node,
    path: updatedPath,
  };
}

export function renameNode(root, path, newName) {
  if (!root || path === '' || root.path === path) return root;

  const lastSlash = path.lastIndexOf('/');
  const parentPath = lastSlash === -1 ? '' : path.slice(0, lastSlash);
  const newPath = parentPath ? `${parentPath}/${newName}` : newName;

  function traverseAndRename(node) {
    if (node.path === path) {
      const renamedNode = { ...node, name: newName };
      return updateNodePaths(renamedNode, path, newPath);
    }

    if (node.type === 'folder' && Array.isArray(node.children)) {
      return {
        ...node,
        children: node.children.map(traverseAndRename),
      };
    }

    return node;
  }

  return traverseAndRename(root);
}

export function updateFileContent(root, path, newContent) {
  if (!root) return root;

  if (root.path === path) {
    if (root.type !== 'file') return root;
    return {
      ...root,
      content: newContent,
    };
  }

  if (root.type === 'folder' && Array.isArray(root.children)) {
    return {
      ...root,
      children: root.children.map((child) => updateFileContent(child, path, newContent)),
    };
  }

  return root;
}

export function listAllFilesFlat(root) {
  if (!root) return [];
  if (root.type === 'file') return [root];

  if (root.type === 'folder' && Array.isArray(root.children)) {
    return root.children.flatMap((child) => listAllFilesFlat(child));
  }

  return [];
}
