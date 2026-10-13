import { useState } from 'react';
import { LANGUAGES } from '../api/languages';
import { CodeEditor } from './CodeEditor';

const SAMPLE_CODES = {
  zetariano: `// Sample Zetariano code
ENTERO total = 100;
DECIMAL tasa = 0.15;

FUNCION procesar(ENTERO valor) {
    SI (valor > 50) {
        IMPRIMIR("Valor alto: " + valor);
    } SINO {
        IMPRIMIR("Valor normal");
    }
}
`,
  y: `// Sample Y? code
DEFINIR contador = 0;
ESTRUCTURA Punto {
    x: INT,
    y: INT
}

DUM (contador < 10) {
    contador = contador + 1;
}
`,
  pig: `VARIABILES:
    int contador = 1;
    string mensaje = "Init";

MAIOR:
    SI (contador == 1) {
        SCRIBE(mensaje);
    }
PROGRAM_END
`,
};

export function EditorPlayground() {
  const [selectedLanguage, setSelectedLanguage] = useState(LANGUAGES[0].id);
  const [code, setCode] = useState(SAMPLE_CODES[LANGUAGES[0].id] || '');

  const handleLanguageChange = (e) => {
    const newLang = e.target.value;
    setSelectedLanguage(newLang);
    if (SAMPLE_CODES[newLang]) {
      setCode(SAMPLE_CODES[newLang]);
    }
  };

  return (
    <div className="card h-100 border-secondary-subtle shadow-sm d-flex flex-column">
      <div className="card-header bg-body-tertiary d-flex align-items-center justify-content-between py-2">
        <span className="fw-bold small text-uppercase text-secondary">Editor Playground</span>
        <div className="d-flex align-items-center gap-2">
          <label htmlFor="lang-select" className="small text-muted mb-0">
            Language:
          </label>
          <select
            id="lang-select"
            className="form-select form-select-sm"
            style={{ width: 'auto' }}
            value={selectedLanguage}
            onChange={handleLanguageChange}
          >
            {LANGUAGES.map((lang) => (
              <option key={lang.id} value={lang.id}>
                {lang.label} ({lang.extension})
              </option>
            ))}
          </select>
        </div>
      </div>

      <div className="card-body p-2 flex-grow-1" style={{ minHeight: '400px' }}>
        <CodeEditor
          code={code}
          language={selectedLanguage}
          onChange={setCode}
        />
      </div>
    </div>
  );
}
