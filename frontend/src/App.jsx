import { useState } from 'react';
import { WorkspacePanel } from './workspace/WorkspacePanel';
// TEMPORARY: replaced in final assembly block
import { EditorPlayground } from './editor/EditorPlayground';

function App() {
  const [, setActiveFile] = useState(null);

  return (
    <div className="container-fluid py-3 px-4">
      <div className="row g-3">
        <div className="col-12 col-md-4 col-lg-3">
          <WorkspacePanel onActiveFileChange={setActiveFile} />
        </div>

        <div className="col-12 col-md-8 col-lg-9">
          {/* TEMPORARY: replaced in final assembly block */}
          <EditorPlayground />
        </div>
      </div>
    </div>
  );
}

export default App;
