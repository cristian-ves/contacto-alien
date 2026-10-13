import { useState, useEffect, useRef, useCallback } from 'react';
import EditorPackage from 'react-simple-code-editor';
import { tokenize } from '../api/client';
import { classifyToken, CATEGORY_COLORS } from './tokenColors';

// Handle CJS/ESM interop where default export might be nested as default.default
const SimpleEditor = EditorPackage.default?.default || EditorPackage.default || EditorPackage;

const EDITOR_STYLE = {
  fontFamily: 'ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", "Courier New", monospace',
  fontSize: 14,
  lineHeight: 1.5,
  minHeight: '100%',
  color: CATEGORY_COLORS.plain,
  caretColor: '#528bff',
  backgroundColor: '#1e1e1e',
};

function splitTokensByLine(tokens) {
  const lineMap = new Map();

  for (const token of tokens) {
    const rawText = token.text || '';
    const tokenLines = rawText.split('\n');

    if (tokenLines.length === 1) {
      const lineNum = token.line;
      if (!lineMap.has(lineNum)) lineMap.set(lineNum, []);
      lineMap.get(lineNum).push({
        ...token,
        text: rawText,
        startColumn: token.startColumn,
      });
    } else {
      // Distribute multiline token across spanned lines
      for (let offset = 0; offset < tokenLines.length; offset += 1) {
        const lineNum = token.line + offset;
        if (!lineMap.has(lineNum)) lineMap.set(lineNum, []);

        const isFirst = offset === 0;
        const segmentText = tokenLines[offset];
        const startColumn = isFirst ? token.startColumn : 0;

        lineMap.get(lineNum).push({
          ...token,
          text: segmentText,
          startColumn,
        });
      }
    }
  }

  return lineMap;
}

function renderHighlightedTokens(value, tokens) {
  const lines = value.split('\n');
  const lineMap = splitTokensByLine(tokens);
  const elements = [];

  for (let lineIdx = 0; lineIdx < lines.length; lineIdx += 1) {
    const lineNum = lineIdx + 1;
    const lineText = lines[lineIdx];
    const lineTokens = (lineMap.get(lineNum) || []).slice().sort((a, b) => a.startColumn - b.startColumn);

    let cursor = 0;

    for (let i = 0; i < lineTokens.length; i += 1) {
      const token = lineTokens[i];
      const start = token.startColumn;
      const tokenLength = token.text ? token.text.length : 0;
      const end = start + tokenLength;

      // Plain text preceding the token
      if (start > cursor && cursor < lineText.length) {
        elements.push(lineText.slice(cursor, Math.min(start, lineText.length)));
      }

      // Token span with category styling
      if (start >= cursor && start < lineText.length) {
        const category = token.hidden ? 'comment' : classifyToken(token.type);
        const textSegment = lineText.slice(start, Math.min(end, lineText.length));

        elements.push(
          <span
            key={`tok-${lineNum}-${start}-${i}`}
            style={{
              color: CATEGORY_COLORS[category] || CATEGORY_COLORS.plain,
              fontStyle: category === 'comment' ? 'italic' : 'normal',
            }}
          >
            {textSegment}
          </span>
        );

        cursor = Math.max(cursor, end);
      }
    }

    // Remaining trailing text on the line
    if (cursor < lineText.length) {
      elements.push(lineText.slice(cursor));
    }

    // Preserve newlines between lines
    if (lineIdx < lines.length - 1) {
      elements.push('\n');
    }
  }

  return elements;
}

export function CodeEditor({ code = '', language, onChange }) {
  const [tokens, setTokens] = useState([]);
  const [highlightStatus, setHighlightStatus] = useState('idle'); // 'idle' | 'loading' | 'ready' | 'error'

  const requestIdRef = useRef(0);
  const lastLanguageRef = useRef(language);

  const performTokenize = useCallback(
    async (codeToTokenize, langToTokenize) => {
      if (!langToTokenize) return;

      const currentReqId = ++requestIdRef.current;
      setHighlightStatus('loading');

      try {
        const response = await tokenize({
          code: codeToTokenize,
          language: langToTokenize,
        });

        if (requestIdRef.current === currentReqId) {
          setTokens(Array.isArray(response?.tokens) ? response.tokens : []);
          setHighlightStatus('ready');
        }
      } catch (error) {
        if (requestIdRef.current === currentReqId) {
          console.warn('Tokenize request failed, falling back to plain text', error);
          setHighlightStatus('error');
        }
      }
    },
    []
  );

  // Immediate tokenization on language change; debounced on code edits
  useEffect(() => {
    const isLanguageChanged = lastLanguageRef.current !== language;
    lastLanguageRef.current = language;

    if (isLanguageChanged) {
      performTokenize(code, language);
      return;
    }

    const timer = setTimeout(() => {
      performTokenize(code, language);
    }, 350);

    return () => {
      clearTimeout(timer);
    };
  }, [code, language, performTokenize]);

  const highlight = useCallback(
    (inputCode) => {
      if (tokens.length === 0 || highlightStatus === 'error') {
        return inputCode;
      }
      return renderHighlightedTokens(inputCode, tokens);
    },
    [tokens, highlightStatus]
  );

  return (
    <div
      className="code-editor-wrapper position-relative rounded overflow-auto shadow-sm"
      style={{
        backgroundColor: '#1e1e1e',
        height: '100%',
        minHeight: '350px',
      }}
    >
      <SimpleEditor
        value={code}
        onValueChange={(val) => onChange?.(val)}
        highlight={highlight}
        padding={14}
        tabSize={4}
        insertSpaces={true}
        style={EDITOR_STYLE}
        textareaClassName="code-editor-textarea"
      />

      {highlightStatus === 'error' && (
        <div
          className="position-absolute bottom-0 end-0 m-2 px-2 py-1 rounded bg-dark bg-opacity-75 text-warning small"
          style={{ zIndex: 10, fontSize: '11px', pointerEvents: 'none' }}
        >
          Highlighting unavailable
        </div>
      )}
    </div>
  );
}
