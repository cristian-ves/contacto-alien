# Documentación Técnica — Contacto 3xtraterrestre

**Proyecto:** Compiladores 2 — USAC / CUNOC  
**Sistema:** Contacto Alien (Entorno y Compilador Multi-Lenguaje)

---

## 1. Introducción y Arquitectura General

El proyecto **Contacto 3xtraterrestre** es una plataforma de análisis y compilación multi-lenguaje compuesta por dos subsistemas desacoplados:

1. **Backend (API REST y Motor de Compilación):** Desarrollado en Java 21 con Spring Boot y ANTLR4. Ejecuta análisis léxico, sintáctico y semántico sobre tres lenguajes interconectados (**Y?**, **Zetariano** y **Pig Latin**), proporcionando endpoints para tokenización en tiempo real y validación semántica completa con resolución de dependencias entre archivos.
2. **Frontend (IDE Web):** Aplicación de una sola página (SPA) desarrollada en React 19 con Vite. Incluye un explorador de archivos con persistencia local en IndexedDB, soporte de carga de directorios (`<input webkitdirectory>`), exportación en formato ZIP (`JSZip`) y un editor de código con coloreado de sintaxis impulsado directamente por los lexers ANTLR del backend.

```
+-----------------------------------------------------------------------+
|                             Frontend (SPA)                            |
|  - Workspace Tree & Persistence (IndexedDB)                           |
|  - File Explorer (Bootstrap 5, FileTree.jsx)                          |
|  - Code Editor (react-simple-code-editor, 4-space indent)             |
|  - Directory Import / ZIP Export                                      |
+-----------------------------------------------------------------------+
                                   |
                         HTTP REST API (JSON)
                                   v
+-----------------------------------------------------------------------+
|                         Backend (Spring Boot)                         |
|  - POST /api/tokenize  --> ANTLR4 Lexers (Zetariano, Y?, Pig Latin)   |
|  - POST /api/validate  --> Full AST + Semantic Analysis Pipelines     |
|  - CompilationUnitRegistry & PigImportLoader                          |
+-----------------------------------------------------------------------+
```

---

## 2. Tecnologías Utilizadas

### Backend
- **Lenguaje:** Java 21 LTS.
- **Framework:** Spring Boot 4.x (`spring-boot-starter-webmvc`).
- **Generador de Parsers:** ANTLR4 4.13.2 (`antlr4-maven-plugin`, target Java, modo visitor).
- **Gestor de Construcción:** Apache Maven.
- **Utilidades:** Project Lombok.

### Frontend
- **Framework/Librería:** React 19.
- **Herramienta de Construcción:** Vite 8.
- **Diseño y Estilos:** Bootstrap 5.3.
- **Motor del Editor:** `react-simple-code-editor` (con renderizado de tokens por spans de color).
- **Persistencia en Cliente:** IndexedDB (API nativa del navegador, sin dependencias externas).
- **Compresión y Exportación:** `jszip` 3.10.

---

## 3. Especificación de los Lenguajes

El sistema integra tres lenguajes complementarios:

### 3.1. Y? (Extensión `.y`)
- **Paradigma:** Procedural / estructurado inspirado en Python.
- **Sensibilidad a mayúsculas:** Case-sensitive.
- **Estructura del programa:**
  - Sección opcional `%estructuras`: Definición de estructuras de datos compuestas con campos tipados, permitiendo anidamiento y arreglos de longitud fija.
  - Sección obligatoria `%funciones`: Definición de funciones y lógica del módulo.
  - No permite variables globales sueltas fuera de funciones/estructuras.
- **Indentación significativa:** Estándar obligatorio de **4 espacios por nivel** (`INDENT` / `DEDENT`).
- **Excepción de bloque:** Las estructuras de control `si`, `sino`, `para`, `mientras`, `hacer` y las funciones usan indentación significativa. El bloque `elegir / caso / siempre` utiliza llaves `{ }`, dentro de las cuales se suspende el tracking de indentación.
- **Tipos de datos primitivos:** `entero`, `flotante`, `cadena`, `caracter`, `bool` (`verdadero` / `falso`).
- **Funciones especiales de I/O:** `imprimir(...)`, `leer()`.
- **Palabras reservadas:** `estructura`, `definir`, `retornar`, `si`, `entonces`, `sino`, `contrario`, `elegir`, `caso`, `siempre`, `romper`, `para`, `mientras`, `hacer`, `continuar`.

### 3.2. Zetariano (Extensión `.z`)
- **Paradigma:** Orientado a objetos (OOP) basado en un subconjunto de Java.
- **Estructura del programa:** 1 archivo = 1 clase pública (`public class NombreClase`). El nombre del archivo debe coincidir con el nombre de la clase.
- **Características OOP:**
  - Atributos y métodos con modificadores de visibilidad `public` / `private` (reconocidos sintácticamente).
  - Palabra clave `this` para desambiguar acceso a campos y métodos (`this.campo`, `this.metodo()`).
  - Instanciación dinámica con `new`.
  - Soporte para llamadas recursivas.
- **Tipos de datos:** `int`, `double`, `char`, `boolean` (`true` / `false`), `String`, `void`.
- **Funciones especiales de I/O:** `println(...)`, `print(...)`, `readln()`.
- **Resolución entre clases:** Implementada mediante `CompilationUnitRegistry`, permitiendo que una clase `.z` referencie a otra (ej. `Pila` utiliza `Nodo`) sin requerir sentencias de importación individuales.

### 3.3. Pig Latin (Extensión `.pig`)
- **Paradigma:** Orquestador principal y punto de entrada (*entrypoint*).
- **Estructura del programa:**
  1. Declaraciones de `import` (0 o más).
  2. Sección opcional `VARIABILES>`: Declaración e inicialización de variables globales.
  3. Sección obligatoria `MAIOR>`: Bloque ejecutable principal.
  4. Cierre obligatorio `FINIS;`.
- **Sintaxis de importación:** `import <ruta>.<extension>` donde la extensión es literalmente `y` o `z` (ej. `import Nodo.z`, `import utils.utils.y`).
- **Tipos de datos:** `numerus` (entero), `decimalis` (flotante), `textum` (cadena), `littera` (caracter), tipo booleano inferido de los literales `verum` / `falsus`, y arreglos con la palabra clave `series`.
- **Instanciación:**
  - Clases Zetariano: `esto x : novus Clase(argumentos);`
  - Estructuras Y?: `esto x : Estructura {valores};`
- **Operadores especiales:**
  - `>>`: Impresión encadenable (`>> "Hola" >> variable;`).
  - `<<`: Lectura por consola (`identificador <<` sin punto y coma final).
- **Estructuras de control:**
  - Condicional: `si (cond) { ... } aliter (cond) { ... } aliter { ... } finis;`
  - Bucle while: `dum (cond) { ... } finis;`
  - Bucle do-while: `facere { ... } dum(cond);` (requiere `;` final).
  - Bucle for: `per (...) { ... }`
  - Control de salto: `perge` (continue), `interrumpe` (break).
- **Comentarios:** Bloques delimitados por líneas `##` ... `##`.
- **Reglas del punto y coma (`;`):** Es opcional en declaraciones simples, asignaciones y sentencias de impresión; es obligatorio en cierres estructurales (`finis;`, `FINIS;`, `facere { } dum(...);`).

---

## 4. Mapeo y Compatibilidad de Tipos

### 4.1. Mapeo de Tipos entre Lenguajes

| Pig Latin | Zetariano | Y? |
| :--- | :--- | :--- |
| `numerus` | `int` | `entero` |
| `decimalis` | `double` | `flotante` |
| `textum` | `String` | `cadena` |
| `littera` | `char` | `caracter` |
| `bool` (`verum`/`falsus`) | `boolean` (`true`/`false`) | `bool` (`verdadero`/`falso`) |

### 4.2. Reglas de Compatibilidad Semántica

1. **Ensanchamiento numérico (*Widening*):**
   - Válido únicamente de menor a mayor precisión: `int` / `entero` / `numerus` &rarr; `double` / `flotante` / `decimalis`.
   - La conversión inversa no es implícita y genera error semántico.
2. **Caracteres:** `char` / `caracter` / `littera` no posee conversión implícita hacia o desde tipos numéricos.
3. **Concatenación (`+`):** Si al menos uno de los operandos es de tipo texto, el otro operando es convertido a texto y el resultado de la expresión es texto.
4. **Aritmética numérico-numérico:** El tipo resultante es el más amplio de los dos operandos.
5. **Operadores lógicos (`&&`, `||`, `!`):** Operan exclusivamente sobre expresiones booleanas y devuelven booleano.
6. **Operadores relacionales (`<`, `>`, `<=`, `>=`):** Operan exclusivamente entre combinaciones de operandos numéricos.
7. **Operadores de igualdad (`==`, `!=`):**
   - Numérico &harr; Numérico.
   - Booleano &harr; Booleano.
   - Caracter &harr; Caracter.
   - Texto &harr; Texto.
   - Estructura/Clase &harr; Misma Estructura/Clase (o `null` en Zetariano).
8. **Invocación de funciones, métodos y constructores:** La validación semántica verifica la coincidencia en la aridad (cantidad de argumentos).

---

## 5. Endpoints de la API REST

### `POST /api/tokenize`
- **Propósito:** Tokenización léxica en tiempo real para resaltado de sintaxis.
- **Payload:**
  ```json
  {
    "code": "ENTERO x = 10;",
    "language": "y"
  }
  ```
- **Respuesta:**
  ```json
  {
    "tokens": [
      {
        "type": "ENTERO",
        "text": "ENTERO",
        "line": 1,
        "startColumn": 0,
        "endColumn": 6,
        "hidden": false
      }
    ],
    "lexicalErrors": []
  }
  ```

### `POST /api/validate`
- **Propósito:** Validación sintáctica y semántica completa de un archivo o proyecto.
- **Payload:**
  ```json
  {
    "code": "import Nodo.z\nVARIABILES>\nMAIOR>\nFINIS;",
    "language": "pig",
    "imports": {
      "Nodo.z": "public class Nodo { int dato; }"
    }
  }
  ```
- **Respuesta:** Lista de errores encontrados (léxicos, sintácticos o semánticos) o arreglo vacío si el código es válido.
