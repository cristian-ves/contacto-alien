# Manual de Usuario — Contacto Alien IDE

**Proyecto:** Compiladores 2 — USAC / CUNOC  
**Entorno:** IDE web para los lenguajes Zetariano (`.z`), Y? (`.y`) y Pig Latin (`.pig`)

---

## 1. Qué es y qué necesita

Contacto Alien es un IDE web: organiza archivos en carpetas, los edita con coloreado en tiempo real, marca los errores léxicos, sintácticos y semánticos, y compila y ejecuta los programas Pig Latin mostrando la salida, las cuartetas y el ensamblador RISC-V.

Para usarlo se necesita:

1. El backend en ejecución (por defecto en `http://localhost:8080`). Si usa otro puerto, arranque el frontend con `VITE_API_BASE_URL=http://localhost:PUERTO npm run dev`.
2. El frontend abierto en el navegador (`http://localhost:5173`).

Si el backend no está activo verá una franja roja arriba con el texto «The backend is not responding. Start it and try again.». El editor sigue funcionando sin colores ni validación; cuando el backend vuelva, la franja desaparece sola (o pulse **Retry**).

## 2. Recorrido de la pantalla

| Zona | Qué hace |
| :--- | :--- |
| Barra superior | Ruta y archivo activo con su lenguaje; botones **Save**, **Validate**, **Download .s** y **Compile & Run** |
| Explorer (izquierda) | Árbol de carpetas y archivos; cuatro botones: nuevo archivo, nueva carpeta, abrir desde el disco, descargar proyecto |
| Pestañas del editor | Un archivo por pestaña; el botón de la derecha abre las opciones del editor |
| Editor | Código con números de línea; los errores se subrayan en rojo |
| Panel inferior | Pestañas **Output**, **Errors**, **Quadruples** y **Assembly**; se puede cambiar su altura arrastrando el divisor |
| Barra de estado | Lenguaje, línea y columna, errores, estado de guardado y conexión con el backend |

Cada archivo muestra una insignia: **P** verde (Pig Latin), **Y** azul (Y?) y **Z** naranja (Zetariano). Un punto naranja significa cambios sin guardar; un punto rojo, que el archivo tiene errores; un punto verde, que está guardado en el disco. Pase el cursor sobre un punto para ver su significado, o pulse el botón **?** del Explorer para abrir la leyenda con todas las marcas.

## 3. Crear y organizar archivos

### 3.1. Crear un archivo
1. Decida dónde va: seleccione una carpeta, o pulse **Workspace root** (la primera fila del Explorer), `Esc` o un espacio vacío del árbol para volver a la raíz. El tooltip del botón dice dónde se creará («New file in banco/»).
2. Pulse el icono de **New file** del Explorer (o clic derecho → **New file**). Aparece una fila con un campo de nombre dentro del árbol, ya enfocada.
3. Escriba el nombre con su extensión, por ejemplo `Banco.pig`, `Cuenta.z` o `Impuestos.y`, y pulse `Enter`. El archivo se abre en una pestaña.

`Esc` cancela. Si el campo pierde el foco, el nombre se crea cuando es válido y se descarta cuando está vacío o inválido. El nombre debe terminar en `.pig`, `.y` o `.z`, no puede llevar espacios ni barras y no puede repetirse en la misma carpeta; el motivo aparece en rojo bajo la fila.

### 3.2. Crear una carpeta
Icono **New folder** (o clic derecho → **New folder**), escriba el nombre en la fila nueva y pulse `Enter`. Las carpetas se abren y cierran con una animación breve (se desactiva si el sistema pide menos movimiento).

> La ruta de cada archivo importa: los `import` de un `.pig` se resuelven con las carpetas. `import banco.Cuenta.z` busca `banco/Cuenta.z` dentro de la carpeta del `.pig`.

### 3.3. Renombrar y eliminar
- **Renombrar:** clic derecho → **Rename**, o seleccione el elemento y pulse `F2`. El nombre se edita en la propia fila, con el nombre sin la extensión ya seleccionado. Si renombra una carpeta, las rutas de su contenido se actualizan solas.
- **Eliminar:** clic derecho → **Delete**, o `Supr` sobre el elemento. Un diálogo pide confirmación; la acción borra también el contenido de una carpeta.

### 3.4. Plegar carpetas y navegar con teclado
Clic en una carpeta para abrirla o cerrarla. Con el foco en el árbol, las flechas arriba y abajo se mueven entre filas; derecha abre una carpeta y izquierda la cierra.

## 4. Abrir archivos desde el disco

Pulse el icono de **Open from disk** y elija:

1. **Open files:** agrega uno o varios `.pig`, `.y` o `.z` a la carpeta seleccionada. Un archivo con el mismo nombre se sobrescribe.
2. **Open folder:** carga una carpeta completa con sus subcarpetas. Reemplaza todo el espacio de trabajo; si ya había archivos, un diálogo pide confirmación (**Replace workspace**). En Chrome y Edge los archivos quedan enlazados al disco: `Ctrl+S` escribe de vuelta en esos mismos archivos (ver 5.2).
3. **Open .zip project:** igual que la carpeta, desde un `.zip` (por ejemplo, uno descargado antes con este IDE).

Los archivos con otra extensión se ignoran y el aviso indica cuántos fueron.

## 5. Editar código

- Haga clic en un archivo del Explorer para abrirlo; las pestañas permiten alternar entre varios. Se cierran con la **x**, con la tecla `Supr` sobre la pestaña o con clic central.
- El coloreado aparece unos instantes después de dejar de escribir: palabras reservadas en violeta, cadenas en verde, números y literales (`verum`, `falsus`, `null`, `true`) en naranja, comentarios en gris, funciones y métodos en azul, tipos en turquesa y operadores en gris azulado.
- `Tab` inserta 4 espacios. `Enter` conserva la sangría de la línea anterior y agrega un nivel después de `{`.
- **Opciones del editor** (icono a la derecha de las pestañas): *Validate while typing* (activa) y *Close brackets automatically* (apagada).
- Si el coloreado no puede actualizarse (backend caído) aparece «Highlighting unavailable» y se conserva el último coloreado.

### 5.1. Guardar
- Los cambios nuevos quedan como **borrador**: la pestaña y el árbol muestran un punto naranja y la barra de estado dice «Unsaved changes».
- Pulse **Save** o `Ctrl+S` para guardar en el navegador; el punto desaparece y la barra dice «Saved to this browser». El proyecto sobrevive a recargar la página.
- Al guardar también se valida el archivo.
- Si cierra una pestaña con cambios, el diálogo ofrece **Save**, **Discard changes** o **Cancel**. Al cerrar la ventana del navegador con borradores, el navegador pide confirmación.

### 5.2. Guardar en el disco real (Chrome y Edge)

Cuando el navegador lo permite, el botón **Save to disk** de la barra superior ofrece:

1. **Save file to disk:** abre el selector del sistema con el nombre del archivo. Al guardar, el archivo queda enlazado: los siguientes `Ctrl+S` escriben directamente ahí (el aviso dice «Saved X to disk» y la barra de estado «Saved to disk and this browser»).
2. **Save project to disk:** un diálogo avisa de que los archivos con el mismo nombre en la carpeta elegida se sobrescribirán; pulse **Choose folder**, elija o cree una carpeta y se escribe todo el árbol (carpetas y archivos, con los cambios sin guardar). Los demás archivos de esa carpeta no se tocan.
3. **Open folder** (Explorer → Open from disk): los archivos se enlazan al disco, así que editar y pulsar `Ctrl+S` actualiza los archivos originales.

Notas:
- Cerrar el selector sin elegir nada no es un error y no muestra avisos.
- Renombrar o eliminar un archivo enlazado no cambia nada en el disco y rompe su enlace: el siguiente `Ctrl+S` guarda solo en el navegador. Use **Save file to disk** para volver a enlazarlo.
- Los enlaces con el disco se pierden al recargar la página; vuelva a abrir la carpeta o a guardar.
- Si ocurre un error al escribir en el disco, el cambio sí queda guardado en el navegador y el aviso lo indica.
- En Firefox y Safari el botón se llama **Download** y ofrece «Download file» y «Download project (.zip)»; el tooltip explica que guardar en el disco requiere Chrome o Edge.

### 5.3. Descargar
- Un archivo: clic derecho → **Download**.
- Una carpeta: clic derecho sobre la carpeta → **Download** (genera `<carpeta>.zip`).
- Todo el proyecto: icono **Download project as .zip** del Explorer (genera `project.zip`).

## 6. Ver y corregir errores

1. Escriba o abra un archivo. Unos instantes después de dejar de escribir se valida automáticamente (también con **Validate** o al guardar).
2. Un error aparece como subrayado ondulado rojo, la línea con fondo rojo y su número en rojo; el archivo muestra un punto rojo en el árbol y en su pestaña.
3. Abra la pestaña **Errors** del panel inferior: cada fila tiene la fase (**Lexical**, **Syntax**, **Semantic**, **Generation** o **Internal**), el mensaje, el archivo, la línea y la columna.
4. **Haga clic en una fila** para saltar a esa posición. Si el error está en un archivo importado, ese archivo se abre.
5. El contador aparece en la pestaña **Errors** y en la barra de estado («No errors» / «N errors»).

## 7. Compilar y ejecutar un programa Pig Latin

1. Abra el archivo `.pig` principal. Sus importados (`.y` y `.z`) deben estar en el árbol dentro de la carpeta del `.pig` (ver 7.2).
2. Pulse **Compile & Run** o `Ctrl+Enter`. El botón dice «Compiling» un instante y después cambia a **Stop**: el programa está corriendo.
3. Mire la pestaña **Output**: es una consola. Lo que el programa imprime aparece a medida que lo escribe.
4. Resultado:
   - **Output:** líneas de sistema con hora, lo que el programa imprimió (`stdout`), mensajes de error en ejecución (`stderr`, en naranja) y, al final, «Finished with exit code N in X ms».
   - **Quadruples:** el código de tres direcciones, numerado por línea y coloreado (temporales, etiquetas, operadores y literales). Un clic en una cuarteta marca en Assembly las instrucciones que le corresponden. Botones para copiar y descargar.
   - **Assembly:** el ensamblador RISC-V generado, de solo lectura, con coloreado (etiquetas, directivas, instrucciones, registros, inmediatos y comentarios) y números de línea. La caja **Jump to label or function** salta a una etiqueta; **Program code only** oculta las rutinas fijas del runtime. Copiar copia lo que se ve; **Download** baja siempre el `.s` completo.
   - El botón de columnas, a la derecha de las pestañas, muestra Quadruples y Assembly lado a lado.
5. Si el programa tiene errores, no se genera código: el panel cambia a **Errors** y la salida dice «Compilation stopped with N errors».

**Compile & Run** está deshabilitado cuando el archivo activo es `.y` o `.z`; el tooltip dice «Open a .pig file to run it». Esos archivos se validan, pero solo se ejecutan a través de un `.pig` que los importe.

### 7.1. La consola: programas que piden datos (paso a paso)

Un programa que lee (`<<`, `readln()`) se detiene y espera. La consola funciona como la de cualquier IDE:

1. Pulse **Compile & Run**. Aparece la salida del programa, por ejemplo `Name:`.
2. Al final de la consola hay una **línea de entrada** con el cursor ya puesto (dice «Type here and press Enter»). Si el programa dejó un aviso sin terminar la línea (por ejemplo `> `), la línea de entrada aparece justo a continuación.
3. Escriba la respuesta y pulse `Enter`. Se muestra en azul, como eco, y el programa la recibe.
4. Repita con cada pregunta. Cuando el programa termina, la línea de entrada desaparece, el botón vuelve a **Compile & Run** y se muestra el código de salida.

Otras acciones:

- **Detener:** el botón **Stop** (mismo lugar que Compile & Run) o la tecla `Esc`. La consola dice «Execution stopped by the user» y el código de salida es 143.
- **Terminar la entrada (EOF):** el botón **End input** de la barra de la consola o `Ctrl+D` en la línea de entrada. El programa ve «fin de entrada» y sigue sin esperar más. Útil para probar cómo reacciona cuando se acaban los datos.
- **Ejecutar otra vez:** `Ctrl+Enter` o **Compile & Run** detienen el programa anterior y empiezan de nuevo con la consola vacía.
- **Bucles infinitos:** no hace falta hacer nada. A los 10 segundos de procesamiento el programa se detiene solo con el mensaje «CPU time limit exceeded (10 s of processing)» (código 124). Esperar a que usted escriba no cuenta: puede tardar lo que quiera en responder.
- **Mensajes de error de ejecución** (`Division by zero`, `Index out of bounds`, `Null reference`, `Stack overflow`, `Out of memory`): salen en naranja y en una línea nueva, aunque el programa estuviera a media línea.
- **Salida muy larga:** se conserva hasta 64 KiB por flujo y la consola muestra las últimas 3000 líneas.
- **Cerrar o recargar la página** detiene el programa.

**Entrada precargada (opcional).** Debajo de las pestañas, la sección plegada **Preloaded input, optional** permite escribir de antemano las respuestas, una por línea. Se envían al programa en cuanto arranca y después la consola sigue siendo interactiva. Sirve para repetir una prueba sin teclear; la consola avisa «Sent N preloaded lines to the program».

Avisos que puede ver:

| Mensaje | Qué hacer |
| :--- | :--- |
| Too many programs are running at the moment… | Hay 5 programas corriendo (límite del servidor). Detenga uno o pulse **Retry** en el aviso. |
| The run session does not exist or has expired. | El servidor ya olvidó ese programa (terminó hace más de 30 s o se reinició). Ejecútelo de nuevo. |
| The program has already finished, so it cannot receive input. | Escribió después de que el programa terminó. |
| The program has not read the input already sent… | Envió mucho texto que el programa aún no leyó. Espere. |
| The connection to the program was interrupted. | La conexión se cortó y no se pudo recuperar. Ejecute de nuevo. |
| The backend stopped responding. The program was stopped. | El backend se cayó o se apagó mientras el programa corría. Inícielo y ejecute de nuevo. |
| Execution stopped: no activity for 300 s while waiting for input | Dejó el programa esperando más de 5 minutos. |

### 7.2. How imports and project folders work

Los imports de un `.pig` se buscan **a partir de la carpeta donde está el propio `.pig`**, no desde la raíz del Explorer. Cada punto del import es una carpeta y la última parte (`y` o `z`) es la extensión: `import lib.Utiles.y` significa el archivo `lib/Utiles.y` junto al `.pig`.

Ejemplo, con el proyecto en cualquier lugar del árbol:

```
valido/
  Demo.pig        ← import lib.Utiles.y
  lib/
    Utiles.y
```

Mover `valido/` dentro de otra carpeta (`proyectos/valido/`) o abrirla como carpeta raíz no cambia nada: lo que cuenta es que `lib/Utiles.y` esté junto a `Demo.pig`. Varios proyectos pueden convivir en el mismo espacio de trabajo, cada uno con su `.pig`.

- Un `.pig` no puede importar archivos de una carpeta hermana ni de una superior.
- Las clases `.z` se validan junto con las demás clases `.z` del proyecto (la carpeta más cercana hacia arriba que contiene un `.pig`), por eso una clase puede usar otra de una carpeta distinta.
- Si un import no se encuentra, el error dice qué ruta esperaba el Explorer (por ejemplo «the workspace needs valido/lib/Utiles.y») y, si hay un archivo con ese nombre en otra carpeta, dónde está. Cree o mueva el archivo a esa ruta, o corrija el `import`.

## 8. Atajos de teclado

| Atajo | Acción |
| :--- | :--- |
| `Ctrl+Enter` | Compile & Run (si hay un programa corriendo, lo reemplaza) |
| `Esc` | Detener el programa que corre |
| `Enter` (línea de entrada) | Enviar la línea al programa |
| `Ctrl+D` (línea de entrada) | Terminar la entrada (EOF) |
| `Ctrl+S` | Guardar el archivo activo |
| `F2` | Renombrar el elemento del Explorer con foco |
| `Supr` | Eliminar el elemento del Explorer con foco (o cerrar la pestaña con foco) |
| `Tab` | Insertar 4 espacios |
| Flechas, `Inicio`, `Fin` | Moverse entre pestañas (editor y panel inferior) y entre filas del árbol |

## 9. Mensajes frecuentes

| Mensaje | Qué significa y qué hacer |
| :--- | :--- |
| The backend is not responding. Start it and try again. | El backend no contesta. Inícielo; el IDE reintenta cada 5 segundos o pulse **Retry**. |
| Highlighting unavailable | No se pudo obtener el coloreado; el código sigue siendo editable. |
| Use a name ending in .pig, .y or .z. | El nombre del archivo no tiene una extensión soportada. |
| Something with that name already exists here. | Ya existe un elemento con ese nombre en la carpeta. |
| No .pig, .y or .z files were found there. | Lo que se abrió desde el disco no contenía archivos soportados. |
| Import not found: lib/Utiles.y. Imports are resolved from the folder of the .pig, so the workspace needs valido/lib/Utiles.y. | El `import` no tiene archivo en la ruta esperada. Cree el archivo allí o corrija el `import` (ver 7.2). |
| Could not save to this browser | IndexedDB no está disponible (por ejemplo, ventana privada restringida). Descargue el proyecto para no perder el trabajo. |
