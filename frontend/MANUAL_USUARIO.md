# Manual de Usuario — Contacto 3xtraterrestre IDE

**Proyecto:** Compiladores 2 — USAC / CUNOC  
**Entorno:** IDE Web para Lenguajes Extraterrestres (Zetariano, Y?, Pig Latin)

---

## 1. Introducción

El IDE Web de **Contacto 3xtraterrestre** es un entorno interactivo diseñado para la edición, navegación, importación, exportación y validación de programas escritos en los lenguajes **Y?** (`.y`), **Zetariano** (`.z`) y **Pig Latin** (`.pig`).

La interfaz está dividida en dos paneles principales:
1. **Panel Lateral Izquierdo (Explorador de Archivos):** Gestión del árbol de carpetas, carga de directorios y descarga del proyecto en formato ZIP.
2. **Panel Central / Derecho (Editor de Código):** Editor con resaltado de sintaxis en tiempo real, soporte de tabulación estandarizada e indicador de estado de conexión.

---

## 2. Gestión de Archivos y Carpetas (Explorador)

### 2.1. Extensiones Soportadas
El sistema solo permite crear o cargar archivos con las siguientes extensiones:
- `.z`: Clases en lenguaje **Zetariano**.
- `.y`: Estructuras y funciones en lenguaje **Y?**.
- `.pig`: Módulo principal en lenguaje **Pig Latin**.

Cualquier archivo con otra extensión será rechazado o ignorado durante la importación.

### 2.2. Creación de Archivos y Carpetas
- **En la raíz del proyecto:** Utilice los botones `+ File` o `+ Folder` situados en la barra superior del panel izquierdo.
- **Dentro de una carpeta específica:** Pase el cursor sobre la carpeta deseada y haga clic en los botones de acción rápida:
  - `+F`: Crea un nuevo archivo dentro de la carpeta seleccionada (solicitará el nombre y validará la extensión).
  - `+D`: Crea una nueva subcarpeta.

### 2.3. Renombrar y Eliminar
- **Renombrar (`Ren`):** Permite cambiar el nombre de un archivo o carpeta. Si se renombra una carpeta, el sistema actualiza automáticamente las rutas relativas de todos sus archivos descendientes.
- **Eliminar (`Del`):** Solicita una confirmación antes de eliminar el archivo o carpeta seleccionada (junto con su contenido).

### 2.4. Navegación en el Árbol
- Las carpetas pueden expandirse o colapsarse haciendo clic directamente sobre su fila.
- Al hacer clic sobre un archivo, este se establece como el archivo activo y se carga inmediatamente en el editor de código.

---

## 3. Importación y Exportación de Proyectos

### 3.1. Cargar una Carpeta del Sistema (`Cargar carpeta`)
1. Haga clic en el botón **Cargar carpeta**.
2. Seleccione la carpeta local desde el explorador de archivos de su sistema operativo.
3. Si el espacio de trabajo actual ya contiene archivos, el sistema le pedirá confirmación antes de sobreescribir el árbol.
4. El sistema cargará todos los archivos soportados (`.z`, `.y`, `.pig`) respetando la estructura original de subcarpetas y mostrará un resumen en caso de que existan archivos ignorados por extensión.

### 3.2. Descargar Proyecto en ZIP (`Descargar proyecto`)
1. Haga clic en el botón **Descargar proyecto**.
2. El sistema empaquetará recursivamente todos los archivos del árbol de trabajo y descargará automáticamente un archivo `proyecto.zip` listo para respaldar o compartir.

---

## 4. Uso del Editor de Código

### 4.1. Edición y Resaltado de Sintaxis
- El editor cuenta con soporte de tema oscuro de alto contraste.
- **Resaltado por el compilador:** Las palabras clave, literales numéricos, cadenas de texto, identificadores, operadores y comentarios se colorean en tiempo real utilizando las definiciones léxicas de ANTLR4 del backend.
- **Indentación estándar:** Al presionar la tecla `Tab`, el editor inserta automáticamente **4 espacios** para cumplir con el estándar de indentación significativa del proyecto.

### 4.2. Indicador de Estado del Resaltado
- Si el backend se encuentra apagado o no hay conexión de red, el editor continúa funcionando normalmente en modo de texto plano y mostrará una notificación discreta en la esquina inferior: `Highlighting unavailable`.
- Una vez reestablecida la conexión con el backend, el coloreado se sincronizará automáticamente con la siguiente modificación.

---

## 5. Persistencia Automática

El IDE utiliza la base de datos local **IndexedDB** del navegador. Cualquier cambio realizado (creación, edición de código, renombramiento o eliminación) se guarda de manera persistente con un retardo de debounce (300 ms). Puede recargar la página o cerrar el navegador sin perder el estado de su proyecto.
