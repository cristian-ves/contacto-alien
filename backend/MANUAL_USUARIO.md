# Manual de Usuario — Backend de Contacto 3xtraterrestre

**Proyecto:** Compiladores 2 — USAC / CUNOC
**Destinatario:** quien instala, arranca y opera el backend (compilador y API).

Para el uso del IDE web (editor, árbol de archivos, consola) consulte [`frontend/MANUAL_USUARIO.md`](../frontend/MANUAL_USUARIO.md). La descripción interna del compilador está en [`DOCUMENTACION_TECNICA.md`](DOCUMENTACION_TECNICA.md).

---

## 1. Qué hace el backend

El backend es una API REST que recibe código fuente en **Pig Latin** (`.pig`), **Y?** (`.y`) o **Zetariano** (`.z`) y ofrece cuatro servicios:

| Servicio | Endpoint | Para qué sirve |
| :--- | :--- | :--- |
| Tokenizar | `POST /api/tokenize` | Devuelve los tokens de un texto para colorearlo |
| Validar | `POST /api/validate` | Revisa léxico, sintaxis y semántica de un archivo |
| Compilar y ejecutar | `POST /api/compile` | Compila un programa Pig a RISC-V, lo ejecuta y devuelve la salida |
| Ejecución interactiva | `POST /api/run` y `/api/run/{id}/...` | Ejecuta un programa que lee datos del usuario, con la salida en vivo |

Para compilar y ejecutar, el backend usa herramientas externas de RISC-V (ensamblador, enlazador y simulador) que deben estar instaladas en la misma máquina.

---

## 2. Requisitos

| Componente | Requisito |
| :--- | :--- |
| Sistema operativo | Linux (las sesiones interactivas miden el CPU en `/proc`) |
| JDK | Java 21 o superior |
| Binutils de RISC-V | `riscv64-linux-gnu-as` y `riscv64-linux-gnu-ld` |
| Simulador | `qemu-riscv64` (paquete `qemu-user`) |
| Maven | No hace falta instalarlo: el proyecto incluye `./mvnw` |
| Red | Solo para que Maven descargue las dependencias la primera vez |

En Debian o Ubuntu:

```bash
sudo apt install openjdk-21-jdk binutils-riscv64-linux-gnu qemu-user
```

Verifique cada herramienta antes de arrancar:

```bash
java -version
riscv64-linux-gnu-as --version
riscv64-linux-gnu-ld --version
qemu-riscv64 --version
```

Si alguna no aparece, vea la sección 7.1.

---

## 3. Arranque

Desde la carpeta `backend/`:

```bash
./mvnw clean package        # genera los analizadores ANTLR, compila y ejecuta las pruebas
./mvnw spring-boot:run      # arranca la API
```

Cuando el arranque termina, la API escucha en `http://localhost:8080`. Detenga el servidor con `Ctrl+C`: las sesiones de ejecución interactiva en curso se detienen antes de cerrar.

También puede ejecutarse el JAR empaquetado:

```bash
java -jar target/contacto-alien-backend-0.0.1-SNAPSHOT.jar
```

### 3.1. Cambiar el puerto

```bash
./mvnw spring-boot:run -Dspring-boot.run.arguments=--server.port=8091
# o, con el JAR:
java -jar target/contacto-alien-backend-0.0.1-SNAPSHOT.jar --server.port=8091
```

El frontend debe apuntar a ese puerto con la variable `VITE_API_BASE_URL` (por ejemplo `VITE_API_BASE_URL=http://localhost:8091`); consulte la documentación del frontend.

### 3.2. Uso desde el frontend

El backend acepta peticiones desde el origen `http://localhost:5173` (el servidor de desarrollo del frontend). Si sirve el frontend desde otro origen, el navegador bloqueará las peticiones por CORS; ese origen se declara en `config/CorsConfig.java`.

---

## 4. Configuración

Todo se define en `backend/src/main/resources/application.properties`. Tras cambiar un valor hay que reiniciar el backend.

### 4.1. Herramientas de RISC-V

| Propiedad | Valor por defecto | Para qué |
| :--- | :--- | :--- |
| `compiler.target.assembler` | `riscv64-linux-gnu-as` | Comando del ensamblador (nombre en el `PATH` o ruta completa) |
| `compiler.target.assembler-flags` | `-march=rv64imafdc` | Banderas del ensamblador |
| `compiler.target.linker` | `riscv64-linux-gnu-ld` | Comando del enlazador |
| `compiler.target.linker-flags` | `-static` | Banderas del enlazador |
| `compiler.target.simulator` | `qemu-riscv64` | Comando del simulador |
| `compiler.target.simulator-flags` | `-s,67108864` | Banderas del simulador (separadas por coma); fija una pila de 64 MiB para el programa y debe coincidir con `TargetConfig.STACK_SIZE_BYTES` |

Si sus herramientas tienen otro nombre o están fuera del `PATH`, escriba la ruta completa, por ejemplo `compiler.target.simulator=/opt/qemu/bin/qemu-riscv64`.

### 4.2. Límites de ejecución

| Propiedad | Valor | Qué limita |
| :--- | :---: | :--- |
| `compiler.target.timeout-seconds` | 10 | Tiempo máximo de un programa: de reloj en `/api/compile`, de CPU en `/api/run` (esperar un dato no cuenta) |
| `compiler.target.max-output-bytes` | 65536 | Salida máxima por flujo (stdout y stderr); el resto se descarta |
| `compiler.run.max-sessions` | 5 | Programas interactivos simultáneos |
| `compiler.run.wall-seconds` | 600 | Duración total máxima de una sesión |
| `compiler.run.idle-seconds` | 300 | Máximo sin salida ni entrada |
| `compiler.run.detach-grace-seconds` | 5 | Cuánto vive un programa sin ninguna consola conectada |
| `compiler.run.finished-retention-seconds` | 30 | Cuánto se conserva una sesión terminada |
| `compiler.run.max-pending-input-bytes` | 16384 | Entrada enviada que el programa aún no leyó |

El programa en ejecución tiene además un heap de 128 MiB y una pila de 64 MiB, fijos en el código.

---

## 5. Uso de la API con `curl`

Con el backend corriendo en el puerto 8080. Los ejemplos usan `jq` para armar el JSON a partir de archivos (`sudo apt install jq`).

### 5.1. Un programa de prueba

Cree `hola.pig`:

```
VARIABILES>
esto nombre : textum "mundo";
MAIOR>
>> "Hola " >> nombre;
FINIS;
```

### 5.2. Validar

```bash
curl -s -X POST localhost:8080/api/validate -H 'Content-Type: application/json' \
  -d "$(jq -n --rawfile code hola.pig '{language:"pig", code:$code, imports:{}}')"
```

```json
{ "valid": true, "errors": [] }
```

Con un error (por ejemplo, usar una variable no declarada) la respuesta lleva la posición, empezando a contar en 1:

```json
{ "valid": false,
  "errors": [ { "line": 5, "column": 4, "message": "Variable 'y' is not declared", "phase": "SEMANTIC" } ] }
```

### 5.3. Compilar y ejecutar

```bash
curl -s -X POST localhost:8080/api/compile -H 'Content-Type: application/json' \
  -d "$(jq -n --rawfile code hola.pig '{language:"pig", code:$code, imports:{}, input:""}')" \
  | jq '{valid, stdout, stderr, exitCode}'
```

```json
{ "valid": true, "stdout": "Hola mundo\n", "stderr": "", "exitCode": 0 }
```

La respuesta completa incluye también `quadruples` (las cuartetas) y `assembly` (el código RISC-V). Para ver solo el ensamblador:

```bash
curl -s -X POST localhost:8080/api/compile -H 'Content-Type: application/json' \
  -d "$(jq -n --rawfile code hola.pig '{language:"pig", code:$code, imports:{}}')" | jq -r .assembly
```

Si el programa lee datos, envíelos en `input`, una línea por dato:

```bash
curl -s -X POST localhost:8080/api/compile -H 'Content-Type: application/json' \
  -d '{"language":"pig","code":"VARIABILES>\nesto n : numerus 0;\nMAIOR>\nn <<\n>> n * 2;\nFINIS;","imports":{},"input":"21\n"}' \
  | jq '{stdout, exitCode}'
```

### 5.4. Programas con imports

Los archivos importados viajan en el mapa `imports`, con rutas relativas y barras como clave. Si `main.pig` tiene `import clases.Nodo.z`:

```bash
curl -s -X POST localhost:8080/api/compile -H 'Content-Type: application/json' \
  -d "$(jq -n --rawfile code main.pig --rawfile nodo clases/Nodo.z \
        '{language:"pig", code:$code, imports:{"clases/Nodo.z":$nodo}}')" \
  | jq '{valid, errors, stdout}'
```

Solo los programas Pig se compilan y ejecutan; los archivos `.y` y `.z` se validan solos con `/api/validate` y se compilan a través de los `import` de un `.pig`.

### 5.5. Ejecución interactiva

Con un `Entrada.pig` que pregunta un dato al usuario:

```bash
# 1. Arrancar el programa: devuelve un sessionId
curl -s -X POST localhost:8080/api/run -H 'Content-Type: application/json' \
  -d "$(jq -n --rawfile code Entrada.pig '{language:"pig", code:$code, imports:{}}')"

# 2. En otra terminal: seguir la salida en vivo (queda abierta hasta que el programa termina)
curl -sN localhost:8080/api/run/<sessionId>/stream

# 3. Responder lo que el programa pregunta (con su salto de línea)
curl -s -X POST localhost:8080/api/run/<sessionId>/input \
  -H 'Content-Type: application/json' -d '{"text":"Ana\n"}'

# 4. Cerrar la entrada (EOF) o detener el programa
curl -s -X POST localhost:8080/api/run/<sessionId>/input \
  -H 'Content-Type: application/json' -d '{"eof":true}'
curl -s -X POST localhost:8080/api/run/<sessionId>/stop
```

El `stream` entrega eventos `stdout`, `stderr`, `system` y un `exit` final con el código de salida. Las respuestas de `input` y `stop` son `204` (sin cuerpo). Los códigos de error son: `404` sesión desconocida o vencida, `409` el programa ya terminó o la entrada ya estaba cerrada, `413` demasiada entrada sin leer, `429` demasiadas sesiones simultáneas.

Un programa abandonado (se cerró el cliente) se detiene solo en unos 11 segundos y su directorio temporal se borra.

### 5.6. Tokenizar

```bash
curl -s -X POST localhost:8080/api/tokenize -H 'Content-Type: application/json' \
  -d '{"code":"esto x : numerus 10;","language":"pig"}' | jq '.tokens[0]'
```

```json
{ "type": "ESTO", "text": "esto", "line": 1, "startColumn": 0, "endColumn": 4, "hidden": false }
```

El campo `language` debe ser exactamente `pig`, `y` o `zetariano`.

---

## 6. Cómo leer una respuesta

### 6.1. Errores de compilación

Todo error del código fuente llega con HTTP 200 y `valid: false`. Cada error trae `line` y `column` (ambos desde 1), `message` (en inglés) y `phase`:

| `phase` | Significado |
| :--- | :--- |
| `LEXICAL` | Un carácter o literal no reconocido |
| `SYNTACTIC` | El programa no cumple la gramática |
| `SEMANTIC` | Tipos incompatibles, símbolos no declarados, reglas de herencia, etc. |
| `GENERATION` | Reservada: el análisis semántico rechaza antes lo que el generador no traduce, así que no debería aparecer para un programa que `/api/validate` acepta |
| `INTERNAL` | Fallo inesperado del compilador |
| `RUNTIME` | Problema con la sesión de ejecución (límite, sesión desconocida, entrada rechazada) |

El backend nunca responde HTTP 500 por un programa mal escrito.

### 6.2. Resultado de la ejecución

| `exitCode` | Significado |
| :---: | :--- |
| `0` | El programa terminó bien |
| `1` | Error de ejecución del programa (el mensaje está en `stderr`) |
| `12` | Memoria agotada (`Out of memory`) |
| `124` | Se cortó por superar el tiempo máximo |
| `143` | La sesión se detuvo desde fuera (botón Stop, desconexión, apagado) |
| `128 + n` | El simulador terminó por la señal `n` |
| `-1` | Falló una herramienta (ensamblador, enlazador o simulador); el texto está en `stderr` |

Los errores de ejecución `Division by zero`, `Index out of bounds`, `Null reference`, `Stack overflow` y `Out of memory` se escriben en el **error estándar** del programa: en `/api/compile` llegan en el campo `stderr` y en `/api/run` como eventos `stderr`.

---

## 7. Solución de problemas

### 7.1. Las herramientas de RISC-V no están instaladas

**Síntoma.** `/api/compile` devuelve `valid: true` pero `exitCode: -1` y un `stderr` como:

```
The assembler 'riscv64-linux-gnu-as' is not installed or is not on the PATH. Install it (Debian/Ubuntu: sudo apt install binutils-riscv64-linux-gnu) or set compiler.target.assembler in application.properties.
```

Si falta el simulador, el mensaje nombra `qemu-riscv64` y el paquete `qemu-user`. En `/api/run` la respuesta es `valid: false` con un error de fase `INTERNAL` con ese mismo texto. La validación (`/api/validate`) y la tokenización siguen funcionando, porque no necesitan las herramientas.

**Solución.** Instale los paquetes (sección 2) y confirme con `which riscv64-linux-gnu-as riscv64-linux-gnu-ld qemu-riscv64`. Si las herramientas están en otra ruta o tienen otro nombre, ajuste `compiler.target.*` (sección 4.1) y reinicie.

### 7.2. El ensamblador o el enlazador fallan

**Síntoma.** `exitCode: -1` y `stderr` que empieza con `Assembler failed:` o `Linker failed:`.

**Causa y solución.** El texto de `stderr` es el mensaje de la herramienta. Revise que `compiler.target.assembler-flags` y `compiler.target.linker-flags` sean válidos para su versión de binutils (los valores por defecto son `-march=rv64imafdc` y `-static`). Si el error persiste con un programa mínimo, conserve el texto de `stderr` y el campo `assembly` de la respuesta para diagnosticarlo.

### 7.3. El puerto ya está en uso

**Síntoma.** El arranque falla con `Web server failed to start. Port 8080 was already in use.`

**Solución.** Cierre el proceso que usa el puerto (`ss -ltnp | grep 8080`) o arranque en otro puerto (sección 3.1) y actualice `VITE_API_BASE_URL` del frontend.

### 7.4. El navegador dice que la petición fue bloqueada por CORS

**Causa.** El frontend no se sirve desde `http://localhost:5173`. El backend solo acepta ese origen.

**Solución.** Arranque el frontend en el puerto 5173, o añada su origen en `config/CorsConfig.java` y reinicie el backend.

### 7.5. El programa se corta por tiempo

**Síntoma.** `exitCode: 124` y una nota `Execution stopped: time limit exceeded (10 s)` o `CPU time limit exceeded`.

**Causa.** El programa (por ejemplo un bucle infinito) superó `compiler.target.timeout-seconds`. En `/api/run` el límite cuenta **tiempo de CPU**: un programa que solo espera un dato no se corta por eso, pero sí por inactividad (`compiler.run.idle-seconds`, 300 s) o por duración total (`compiler.run.wall-seconds`, 600 s).

**Solución.** Corrija el programa, o aumente la propiedad correspondiente si necesita más tiempo.

### 7.6. `POST /api/run` responde 429

Hay `compiler.run.max-sessions` programas en ejecución. Deténgalos con `POST /api/run/<id>/stop` o espere a que terminen; las sesiones abandonadas se limpian en pocos segundos.

### 7.7. La sesión desaparece (`404`)

Una sesión terminada se conserva `compiler.run.finished-retention-seconds` (30 s) y las sesiones viven solo en la memoria del backend: si se reinició, se perdieron. Vuelva a arrancar el programa con `POST /api/run`.

### 7.8. La salida aparece cortada

Cada flujo (stdout y stderr) se limita a `compiler.target.max-output-bytes` (65536 bytes). Al llegar al límite, `/api/compile` agrega `[output truncated: the output limit was reached]` al final y `/api/run` envía un evento `system`. Aumente la propiedad si necesita más salida.

### 7.9. Falla el arranque por la versión de Java

Spring Boot 4 requiere Java 21 o superior. Compruebe `java -version` y `echo $JAVA_HOME`; si hay varios JDK instalados, apunte `JAVA_HOME` al 21.

### 7.10. Quedan directorios temporales en `/tmp`

Cada ejecución usa un directorio temporal que se borra al terminar. Solo una muerte forzada del backend (`kill -9`) puede dejar alguno; es seguro borrarlo a mano.
