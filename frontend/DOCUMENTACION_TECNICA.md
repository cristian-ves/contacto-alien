# Documentación Técnica del Frontend — Contacto 3xtraterrestre

**Proyecto:** Compiladores 2 — USAC / CUNOC  
**Sistema:** Contacto Alien (Entorno y Compilador Multi-Lenguaje)  
**Alcance:** describe lo que el código del frontend hace hoy. La documentación del compilador (lenguajes, gramáticas, tipos, cuartetas, RISC-V) está en `backend/DOCUMENTACION_TECNICA.md`.

---

## 1. Introducción

El frontend es una aplicación de una sola página (SPA) que ofrece un explorador de archivos con persistencia local, un editor con coloreado en tiempo real, validación de errores y la ejecución de programas Pig Latin (cuartetas, ensamblador RISC-V y salida). Trabaja con los tres lenguajes del proyecto: Zetariano (`.z`), Y? (`.y`) y Pig Latin (`.pig`). El texto de la interfaz está en inglés; la interfaz usa un tema oscuro único definido en `src/theme/`.

## 2. Tecnologías

Versiones de `frontend/package.json`.

| Componente | Uso |
| :--- | :--- |
| React / React DOM 19 | Interfaz |
| Vite 8 | Servidor de desarrollo y construcción (`npm run dev`, `npm run build`) |
| `@mui/material` + `@emotion/*` | Componentes (botones, diálogos, menús, tooltips, campos, avisos) y tema propio |
| `lucide-react` | Iconos (16 px) |
| `@fontsource/inter`, `@fontsource/fira-code` | Fuentes empaquetadas: la app se ve igual sin internet |
| `jszip` | Descargar y abrir proyectos `.zip` |
| IndexedDB | Persistencia local del árbol de trabajo |
| oxlint | Análisis estático (`npm run lint`) |

No se usa Bootstrap ni ninguna librería de coloreado: el color de cada token lo decide el backend (`/api/tokenize`) y el frontend solo lo traduce a una clase CSS.

La URL del backend sale de la variable `VITE_API_BASE_URL` (archivo `.env` o variable de entorno al ejecutar Vite). Si no existe, `src/api/client.js` usa `http://localhost:8080`, el puerto por defecto del backend; para otro puerto: `VITE_API_BASE_URL=http://localhost:8091 npm run dev`. El backend solo acepta el origen `http://localhost:5173` (CORS), así que el servidor de desarrollo debe usar ese puerto.

---

## 3. Estructura del código (`src/`)

| Carpeta / archivo | Responsabilidad |
| :--- | :--- |
| `main.jsx`, `App.jsx` | Arranque: fuentes, hojas de estilo, tema MUI y proveedor del estado del backend |
| `messages.js` | Todas las frases visibles de la interfaz (un solo lugar) |
| `theme/palette.js`, `theme/theme.js`, `theme/GlobalVars.jsx` | Colores, fuentes y sombras; tema MUI oscuro; variables CSS generadas desde la misma paleta |
| `styles/base.css`, `layout.css`, `code.css` | Chips, insignias, distribución general, superficies de código (editor, consola, listados) |
| `api/client.js` | `tokenize`, `validate` (con `imports` y `path` opcionales), `compile` (POST JSON con `AbortSignal`) y la ejecución interactiva: `startRun`, `sendRunInput`, `stopRun`, `stopRunOnUnload` (beacon); `ApiError` distingue red caída (`network`) de error HTTP (con `status` y el mensaje legible del backend) |
| `api/runStream.js` | Lector SSE hecho con `fetch` (`createSseParser`, `followRun`): eventos del programa en vivo, reconexión con `Last-Event-ID` y errores tipados |
| `api/languages.js` | Lenguajes soportados e insignia (P, Y, Z) por extensión |
| `api/importsMap.js` | `buildImportContext`: mapa `imports`, `path` y carpeta base que espera el backend |
| `api/diagnostics.js` | Normaliza los errores del backend, separa el prefijo `[archivo]` y explica los imports no encontrados |
| `api/BackendStatus.jsx`, `backendContext.js`, `useBackend.js` | Estado «backend conectado / caído» y reintento automático |
| `workspace/treeModel.js`, `paths.js`, `nameRules.js` | Funciones puras: árbol, rutas y validación de nombres |
| `workspace/useWorkspace.js`, `persistence.js` | Árbol en memoria con guardado en IndexedDB (300 ms tras cada cambio) |
| `workspace/importExport.js` | Leer archivos/carpetas/zip del disco (input clásico); descargar archivo o proyecto |
| `workspace/diskAccess.js` | Envoltorio de la File System Access API: detección, selectores, leer carpeta con handles, escribir archivo y árbol |
| `state/ideReducer.js` | Estado de la interfaz: pestañas, archivo activo, borradores, carpetas abiertas, resultados de validación, cursor |
| `state/useIde.js` | Une árbol, reducer, validación y compilación; expone las acciones (abrir, guardar, crear, renombrar, eliminar, descargar…) |
| `state/useDiskActions.js` | Guardar en el disco real: Ctrl+S sobre archivos enlazados, «Save file to disk», «Save project to disk», abrir carpeta con handles |
| `state/selectors.js` | Agrupa los errores por archivo |
| `hooks/useTokens.js` | Coloreado: petición con pausa de 350 ms y cancelación |
| `hooks/useValidation.js` | Validación con pausa de 600 ms, al guardar y bajo demanda |
| `hooks/useCompile.js` | Sesión de ejecución: arranca el programa, sigue su stream, envía la entrada (`sendLine`, `sendEof`), lo detiene (`stop`) y guarda cuartetas y ensamblador |
| `hooks/useTabKeys.js` | Navegación con flechas en listas de pestañas |
| `editor/CodeEditor.jsx` | Editor: textarea transparente sobre la capa coloreada, números de línea, línea actual, errores |
| `editor/tokenCategories.js` | Tipo de token del backend → categoría de color |
| `editor/lineModel.js` | Texto + tokens + errores → segmentos por línea (puro) |
| `editor/editing.js` | Tab, Enter con sangría, cierre automático de paréntesis |
| `editor/EditorArea.jsx`, `EditorTabs.jsx`, `EditorOptions.jsx` | Pestañas del editor, estado vacío y opciones |
| `explorer/Explorer.jsx`, `TreeNode.jsx`, `TreeMenu.jsx`, `visibleRows.js` | Árbol, fila «Workspace root», menú contextual, teclado, animación de carpetas (MUI `Collapse`, 150 ms, 0 con `prefers-reduced-motion`) |
| `explorer/NameRow.jsx` | Fila con campo de nombre dentro del árbol para crear y renombrar (Enter, Esc, blur, validación en línea con `validateName`) |
| `explorer/LegendDialog.jsx`, `DiskOpenControls.jsx` | Leyenda de puntos e insignias; menú «Open from disk» y sus inputs |
| `components/StateDot.jsx`, `noAutofill.js` | Punto de estado con tooltip (naranja sin guardar, rojo errores, verde en disco); atributos que apagan autocompletado en campos de nombre y de entrada |
| `dialogs/DialogHost.jsx`, `ConfirmDialog.jsx` | Diálogos MUI (eliminar, descartar, reemplazar, guardar proyecto) |
| `panel/BottomPanel.jsx`, `ErrorsView.jsx` | Panel inferior y botón de vista dividida |
| `panel/ListingPanes.jsx`, `QuadsPane.jsx`, `AsmPane.jsx` | Quadruples y Assembly (pestañas o lado a lado); la cuarteta elegida marca sus instrucciones |
| `panel/listing/syntax.js`, `asmModel.js`, `ListingView.jsx`, `ListingToolbar.jsx`, `exportText.js` | Coloreado propio (escáneres de cuartetas y RISC-V), índice de etiquetas, frontera del runtime y enlace cuarteta→assembly (puros), listado numerado, barra con copiar y descargar |
| `panel/OutputView.jsx`, `ConsoleView.jsx`, `ConsoleInput.jsx`, `ConsoleBar.jsx` | Pestaña Output: consola en vivo, línea de entrada y barra con la entrada precargada y «End input» |
| `panel/consoleModel.js`, `useStickToBottom.js` | Modelo puro de la consola (líneas abiertas, eco, stderr) y desplazamiento automático que respeta al usuario |
| `panel/formatQuadruples.js`, `useResize.js` | Texto de las cuartetas (puro); divisor redimensionable |
| `shell/DiskMenu.jsx` | Menú «Save to disk» (o «Download» sin la API) de la barra superior |
| `shell/AppShell.jsx`, `TopBar.jsx`, `StatusBar.jsx`, `Toast.jsx`, `BackendBanner.jsx` | Composición y barras |
| `shell/useShortcuts.js`, `useDiskOpen.js` | Atajos globales (`Ctrl+Enter`, `Esc`, `Ctrl+S`); lectura y reparto de lo abierto desde el disco |
| `components/` | `FileBadge`, `PhaseChip`, `TipButton` (botón de icono con tooltip) |

Ninguna clase supera ~250 líneas.

---

## 4. Flujo de datos

```
useWorkspace (árbol + IndexedDB) ─┐
ideReducer (pestañas, borradores, ├─> useIde ──> AppShell ──> Explorer / EditorArea / BottomPanel / barras
             resultados, cursor)  │      │
                                  │      ├─ useValidation ── POST /api/validate ─> ideReducer.results
                                  │      └─ useCompile ───── POST /api/run + stream SSE + POST input/stop ──> consola, cuartetas, .s
EditorArea ── useTokens ── POST /api/tokenize ──> CodeEditor (solo pinta)
```

- **Borradores.** Lo que se escribe vive en `drafts` (ruta → texto) hasta guardar. `getContent(ruta)` devuelve el borrador o, si no hay, el contenido del árbol. La marca de «sin guardar» (punto naranja en pestaña y árbol, «Unsaved changes» en la barra de estado) es la presencia de un borrador. Guardar (`Ctrl+S` o botón Save) pasa el borrador al árbol, que IndexedDB persiste. Al recargar la página se pierden los borradores; el navegador avisa antes de cerrar si hay alguno.
- **Mapa de imports.** `buildImportContext` devuelve `{imports, path, base}` y es la única función que decide qué viaja con una petición (regla completa en la sección 4.5). Siempre usa el borrador de un archivo si existe.
- **Errores.** El backend devuelve `{line, column, message, phase}`. Los errores de un archivo importado llegan como `[ruta] mensaje`, con la ruta relativa a la carpeta `base` del mapa; `normalizeErrors` la convierte en una ruta del espacio de trabajo (si existe) para poder abrir ese archivo al hacer clic. Un `Imported file not found: X` se reescribe con la ruta que el espacio de trabajo debería tener y, si hay un archivo con ese nombre en otra carpeta, dónde está. Las columnas se tratan como 1-based en todas las fases (sin compensar). Los errores se guardan por archivo validado y se agrupan por archivo afectado para marcar el editor, las pestañas y el árbol.

### 4.1. Coloreado en tiempo real

1. `useTokens` pide `/api/tokenize` 350 ms después de la última tecla; al cambiar de archivo lo hace sin espera. La petición anterior se cancela con `AbortController`.
2. `classifyTokens` asigna una categoría al nombre del tipo de token del backend (tabla abajo). Un identificador seguido de `(` es función/método; uno después de `new`/`novus`/`class`/`extends` o seguido de otro identificador es tipo. Las palabras reservadas están listadas explícitamente en `tokenCategories.js`: un nombre de token que el backend agregue y que no esté en ninguna lista cae en texto normal, sin error.

| Categoría (clase CSS) | Color | Tokens |
| :--- | :--- | :--- |
| `kw` | violeta `#C084FC` | Control y declaraciones de los tres lenguajes (`import`, `esto`, `novus`, `si`, `dum`, `per`, `definir`, `elegir`, `class`, `new`, `return`, `this`, `super`, `println`…), marcadores de sección (`VARIABILES>`, `MAIOR>`, `FINIS`, `%funciones`) |
| `mod` | violeta claro `#D8B4FE` | Visibilidad y herencia: `public`, `private`, `protected`, `extends` |
| `an` | violeta claro, cursiva | Anotación `@Override` |
| `str` | verde `#4ADE80` | Cadenas y caracteres; una cadena sin cerrar sale del backend con tipo `STRING_LITERAL` y se pinta igual (el error léxico la subraya) |
| `num` | naranja `#FB923C` | Números y literales (`verum`, `falsus`, `verdadero`, `falso`, `true`, `false`, `null`) |
| `cm` | gris `#64748B`, cursiva | Comentarios (tokens ocultos) |
| `fn` | azul `#60A5FA` | Identificador seguido de `(` |
| `ty` | turquesa `#2DD4BF` | Tipos primitivos (`numerus`, `entero`, `int`, `String`, `void`…) y nombres de clase |
| `op` | gris azulado `#94A3B8` | Operadores (`==`, `&&`, `++`, `+=`, `->`, `>>`, `<<`, `?`, `%` = `MOD` en Pig y Y?…) |
| `plain` (texto normal, a propósito) | `#E2E8F0` | Identificadores, delimitadores `( ) { } [ ] , ; . :`, `NEWLINE`/`INDENT`/`DEDENT` de Y?, `FUNCTIONS_SECTION` (`MUNERA>` de Pig; su cuerpo no genera tokens: la validación rechaza la sección y una palabra pintada taparía el subrayado rojo) y cualquier token desconocido |
3. `buildLineModel` reparte los tokens por línea (también los multilínea) y subraya con onda roja el token bajo la columna de cada error.
4. `CodeEditor` dibuja una capa de líneas coloreadas y, encima, un `<textarea>` con texto transparente que recibe la escritura. Contenedor único con desplazamiento, así que números de línea, capa y textarea nunca se desfasan. Altura de línea fija de 23 px (el diseño pide 1.7 × 13.5 px ≈ 22.95).
5. Si la petición falla, se conserva el último coloreado y aparece «Highlighting unavailable».

### 4.2. Validación

`useValidation` valida el archivo activo 600 ms después de dejar de escribir (opción «Validate while typing», activa por defecto), al guardar y con el botón Validate. Cada petición nueva aborta la anterior. El resultado alimenta la lista de errores, los contadores y los marcadores.

### 4.3. Compile & Run y consola interactiva

`useCompile` envía el `.pig` activo y los imports a `POST /api/run` (la entrada precargada solo va si el campo tiene texto). Si `valid` es `false`, el panel salta a **Errors** y no hay código ni sesión. Si es `true`, el programa ya está corriendo en el backend y el hook abre `GET /api/run/{id}/stream` con `fetch` (no `EventSource`: así se leen los códigos HTTP, se usa `AbortController` y se controla la reconexión). Las cuartetas y el `.s` se muestran numerados y coloreados por `panel/listing/syntax.js`. El backend no marca la frontera del runtime con un campo: se usa el comentario `# ---- runtime` que escribe el generador (lo anterior es el programa) y, tras él, los bloques de datos propios (`str_N`, `g_*`, `class_table`, `vt_N`) que siguen al runtime. Cada cuarteta se enlaza con sus instrucciones por el comentario `# <texto>` que el generador deja antes de ellas; si falta, esa cuarteta no marca nada.

Estados (`compileState.status`): `idle`, `starting` (compilando; el botón dice «Compiling»), `running` (el botón pasa a **Stop**), `done` (llegó el evento `exit`), `rejected` (el código tiene errores) y `failed` (red, sesión vencida, límite de sesiones, stream cortado).

```
Ctrl+Enter ─> run() ─> POST /api/run ─> sessionId ─> followRun (SSE) ──> stdout / stderr / system / exit ──> consoleReducer
                  ^ detiene la sesión anterior                                   │
Enter en la consola ─> sendLine ─> eco inmediato + POST /input ─────────────────┘ (el programa lee y escribe más)
Esc / botón Stop ─> POST /stop ─> llega `exit` con código 143
```

- **Consola.** `consoleModel.js` (puro, sin React) mantiene una lista de líneas. Cada fragmento de stdout/stderr se corta por `\n`; la última pieza sin salto deja la línea **abierta** (un prompt como `> `). La línea de entrada se dibuja justo después de una línea abierta de stdout, o en una fila propia si no hay ninguna. Si llega stderr, un mensaje del sistema o el cierre mientras hay una línea abierta de otro flujo, esa línea se cierra y lo nuevo empieza en **línea nueva**. Al pulsar Enter, el texto se muestra enseguida con estilo propio (eco, azul) y completa la línea abierta, y se envía con su salto de línea. Se conservan las últimas 3000 líneas y la consola avisa cuántas se ocultaron.
- **Colores** (ver `diseno-ui.md`): stdout claro, stderr naranja, eco azul, sistema apagado, cierre exitoso verde. La fuente de código solo se usa en la consola, no en los botones.
- **Entrada.** Mientras la sesión corre, la línea de entrada tiene el foco al aparecer; `Enter` envía, `Ctrl+D` o el botón **End input** cierran la entrada (EOF). Una entrada rechazada (programa terminado, demasiada entrada sin leer) se muestra en la consola y como aviso.
- **Entrada precargada.** Sección plegada «Preloaded input, optional» (un `textarea`): sus líneas viajan en el campo `input` de `/api/run` y el backend las entrega al arrancar. Sirve para repetir una prueba; la consola avisa «Sent N preloaded lines to the program».
- **Reemplazo y limpieza.** Ejecutar de nuevo detiene la sesión anterior (se aborta su stream y se envía `POST /stop`); un número de ejecución descarta los eventos tardíos de la sesión vieja. Al cerrar o recargar la página (`pagehide`) se envía `navigator.sendBeacon` a `/stop`; además, un stream cerrado hace que el backend mate el programa a los 5 s.
- **Errores.** `followRun` reconecta hasta 3 veces (1 s entre intentos) retomando con `Last-Event-ID`. Si el backend no contesta: «The backend stopped responding. The program was stopped.» y la franja roja. Si la sesión ya no existe (404), se muestra el mensaje del backend. Límite de sesiones (429): mensaje en consola y aviso con **Retry**.

### 4.4. Estado del backend

`BackendStatusProvider` prueba `/api/tokenize` al arrancar y cada petición reporta si llegó o no. Con el backend caído se muestra una franja roja con «The backend is not responding. Start it and try again.», y se reintenta cada 5 s hasta que responda.

### 4.5. Cómo se resuelven los imports y las carpetas de proyecto

| Archivo activo | Carpeta base del mapa | Contenido de `imports` | `path` |
| :--- | :--- | :--- | :--- |
| `.pig` | La carpeta del propio `.pig` | Todo `.y` y `.z` dentro de esa carpeta (a cualquier profundidad), con ruta relativa a ella | no se envía |
| `.z` | La raíz de su proyecto: la carpeta ancestro más cercana que contiene un `.pig`; si no hay, su propia carpeta | Todos los demás `.z` de esa raíz, con ruta relativa a ella | ruta del archivo relativa a la raíz |
| `.y` | — | nada | no se envía |

`import lib.Utiles.y` en `valido/Demo.pig` significa `valido/lib/Utiles.y`: la ruta del import se busca junto al `.pig`, no en la raíz del espacio de trabajo, así que el proyecto funciona igual en cualquier posición del árbol. El backend hace el trabajo con las claves del mapa (`ImportPathResolver` convierte `lib.Utiles.y` en la clave `lib/Utiles.y`) y no toca el disco.

Un `.z` se valida con todas las clases de su proyecto porque el paquete de una clase es su carpeta y una clase puede usar otra de una carpeta distinta (`clases/Linea.z` con `modelos/Punto.z`); `path` le dice al backend en qué carpeta está el archivo. Sin eso, la validación suelta de `Linea.z` daba «Unknown type 'Punto'» aunque el `.pig` compilara bien. Es la misma convención que usa el backend para el campo `path`.

Si un import no existe, el panel de errores indica la ruta que el espacio de trabajo debería tener («the workspace needs valido/lib/Utiles.y») y, si hay un archivo con ese nombre en otra carpeta, dónde está. Al hacer clic en un error de un archivo importado, el editor abre ese archivo.

---

## 5. Gestión del árbol de trabajo

- **Modelo.** Un nodo raíz con hijos; cada nodo es carpeta o archivo con `name`, `path` (ruta con `/`) y, en archivos, `content`. Las operaciones de `treeModel.js` devuelven un árbol nuevo.
- **Crear, renombrar, eliminar.** Botones del encabezado, menú contextual (clic derecho) y teclado (`F2` renombrar, `Supr` eliminar) abren diálogos MUI. `validateName` exige extensión `.pig`, `.y` o `.z` en archivos, prohíbe barras y espacios y rechaza nombres repetidos. Renombrar actualiza las rutas de los descendientes y mueve pestañas y borradores.
- **Abrir desde el disco.** Menú con «Open files» (se agregan a la carpeta seleccionada), «Open folder» (`webkitdirectory`) y «Open .zip project». Una carpeta o `.zip` reemplaza el espacio de trabajo y pide confirmación si ya hay archivos. Solo se leen `.z`, `.y` y `.pig`; el resto se ignora y se informa.
- **Guardar en el navegador y descargar.** `Ctrl+S` / Save guarda en IndexedDB. Descargar: archivo individual, carpeta o proyecto completo en `.zip` (incluye los borradores).
- **Guardar en el disco real** (sección 5.1).
- **Persistencia.** Árbol completo en IndexedDB (clave `workspace-tree`); si falla, la barra de estado muestra «Could not save to this browser».

### 5.1. Guardado en el disco real (File System Access API)

Disponible en Chrome y Edge (`showSaveFilePicker` y `showDirectoryPicker`). `diskAccess.js` detecta la API en cada uso; sin ella, la interfaz ofrece las descargas de siempre.

- **Handles enlazados.** `ideReducer.handles` guarda en memoria, por ruta del árbol, el `FileSystemFileHandle` del archivo en el disco. Un archivo «enlazado» se escribe directo con `Ctrl+S`. Los handles no se persisten (como los borradores): al recargar hay que volver a abrir o guardar.
- **Save file to disk.** `showSaveFilePicker` con el nombre del archivo; escribe el texto actual (borrador incluido), enlaza el handle, pasa el borrador al árbol (IndexedDB) y avisa «Saved X to disk».
- **Save project to disk.** Primero un diálogo avisa de que se sobrescribirán archivos con el mismo nombre. Luego `showDirectoryPicker({mode: 'readwrite'})` y `writeTreeToFolder` recorre el árbol creando carpetas y archivos con las ediciones sin guardar. Los archivos escritos quedan enlazados a esa carpeta y los borradores pasan al árbol. Los demás archivos de la carpeta no se tocan.
- **Open folder.** Con `showDirectoryPicker`, `readFolderHandle` lee recursivamente `.pig/.y/.z` (omite carpetas que empiezan con `.` y `node_modules`) conservando el handle de cada archivo; reemplaza el espacio de trabajo (con confirmación si ya hay archivos) y todos quedan enlazados. Sin la API se usa el `<input webkitdirectory>` de antes.
- **Ctrl+S.** Siempre guarda en IndexedDB; si el archivo está enlazado, además escribe en el disco («Saved X to disk»). Si falla la escritura en disco, el aviso rojo lo dice y aclara que la copia del navegador sí quedó.
- **Cancelar** un selector lanza `AbortError`; se ignora sin aviso. Cualquier otro error muestra un aviso rojo.
- **Sin la API** (Firefox, Safari): el botón de la barra superior se llama «Download», sus opciones son «Download file» y «Download project (.zip)» y el tooltip dice «Saving to your disk needs Chrome or Edge. Download instead.».
- **Renombrar o eliminar** un archivo enlazado quita su enlace (no se renombra ni se borra nada en el disco); el siguiente `Ctrl+S` guarda solo en el navegador y la barra de estado vuelve a decir «Saved to this browser».
- **Estado.** La barra de estado muestra «Saved to disk and this browser» para un archivo enlazado y sin cambios.

---

## 6. Accesibilidad y teclado

Roles ARIA `tablist/tab/tabpanel` en las pestañas del editor y del panel (flechas, Inicio y Fin), `tree/treeitem` en el árbol (flechas arriba/abajo, izquierda/derecha para plegar), `listbox/option` en la lista de errores y `separator` (flechas) en el divisor. Foco visible en todo. Atajos: `Ctrl+Enter` ejecuta (y reemplaza una ejecución en curso), `Esc` detiene el programa, `Ctrl+D` en la línea de entrada cierra la entrada, `Ctrl+S` guarda, `F2` renombra, `Supr` elimina. La consola es un `role="log"`. Se respeta `prefers-reduced-motion`.

---

## 7. Límites conocidos

- El mapa de imports de un `.pig` solo incluye archivos dentro de su carpeta (el backend no resuelve imports hacia arriba): un `.pig` no puede importar un archivo de una carpeta hermana o superior.
- Un `.z` se valida con todos los `.z` de su proyecto, aunque el `.pig` no los importe; en la compilación solo existen los importados.
- Las columnas se interpretan como 1-based en todas las fases; si el backend aún entrega 0-based en léxicos y sintácticos, el marcador cae una columna antes.
- Los borradores y los handles del disco no sobreviven a una recarga de la página.
- Las carpetas vacías del disco no aparecen al abrir una carpeta (el árbol se reconstruye desde los archivos).
- Con más de 3000 líneas de salida, la consola descarta las más antiguas (y lo dice). La salida total de un programa está limitada a 64 KiB por flujo en el backend.
- La duración que muestra el cierre es de reloj e incluye el tiempo que el usuario tardó en escribir; el límite del backend es de tiempo de CPU.
- No hay pruebas automatizadas del frontend; la verificación se hizo de punta a punta con un navegador contra una copia del backend.
